// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.storage.download;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.DownloadCommand;
import com.cloud.agent.api.storage.DownloadCommand.Proxy;
import com.cloud.agent.api.storage.DownloadCommand.ResourceType;
import com.cloud.agent.api.storage.DownloadProgressCommand;
import com.cloud.agent.api.storage.DownloadProgressCommand.RequestType;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.dao.UserVmDao;

@Component
@Local(value = { DownloadMonitor.class })
public class DownloadMonitorImpl extends ManagerBase implements DownloadMonitor {
    static final Logger s_logger = Logger.getLogger(DownloadMonitorImpl.class);

    @Inject
    TemplateDataStoreDao _vmTemplateStoreDao;
    @Inject
    ImageStoreDao _imageStoreDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    AlertManager _alertMgr;
    @Inject
    protected SwiftManager _swiftMgr;
    @Inject
    SecondaryStorageVmManager _ssvmMgr;
    @Inject
    StorageManager _storageMgr;
    @Inject
    VMTemplateDao _templateDao = null;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    SecondaryStorageVmManager _secMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    UserVmManager _vmMgr;

    @Inject
    TemplateManager templateMgr;

    @Inject
    protected ResourceLimitService _resourceLimitMgr;
    @Inject
    protected UserVmDao _userVmDao;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    TemplateDataFactory tmplFactory;

    private Boolean _sslCopy = new Boolean(false);
    private String _copyAuthPasswd;
    private String _proxy = null;
    protected SearchBuilder<TemplateDataStoreVO> ReadyTemplateStatesSearch;

    Timer _timer;

    @Inject
    DataStoreManager storeMgr;

    final Map<TemplateDataStoreVO, DownloadListener> _listenerTemplateMap = new ConcurrentHashMap<TemplateDataStoreVO, DownloadListener>();
    final Map<VMTemplateHostVO, DownloadListener> _listenerMap = new ConcurrentHashMap<VMTemplateHostVO, DownloadListener>();
    final Map<VolumeHostVO, DownloadListener> _listenerVolumeMap = new ConcurrentHashMap<VolumeHostVO, DownloadListener>();
    final Map<VolumeDataStoreVO, DownloadListener> _listenerVolMap = new ConcurrentHashMap<VolumeDataStoreVO, DownloadListener>();

    public void send(Long hostId, Command cmd, Listener listener) throws AgentUnavailableException {
        _agentMgr.send(hostId, new Commands(cmd), listener);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        final Map<String, String> configs = _configDao.getConfiguration("ManagementServer", params);
        _sslCopy = Boolean.parseBoolean(configs.get("secstorage.encrypt.copy"));
        _proxy = configs.get(Config.SecStorageProxy.key());

        String cert = configs.get("secstorage.ssl.cert.domain");
        if (!"realhostip.com".equalsIgnoreCase(cert)) {
            s_logger.warn("Only realhostip.com ssl cert is supported, ignoring self-signed and other certs");
        }

        _copyAuthPasswd = configs.get("secstorage.copy.password");

        _agentMgr.registerForHostEvents(new DownloadListener(this), true, false, false);

        ReadyTemplateStatesSearch = _vmTemplateStoreDao.createSearchBuilder();
        ReadyTemplateStatesSearch.and("state", ReadyTemplateStatesSearch.entity().getState(), SearchCriteria.Op.EQ);
        ReadyTemplateStatesSearch.and("destroyed", ReadyTemplateStatesSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        ReadyTemplateStatesSearch.and("store_id", ReadyTemplateStatesSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);

        SearchBuilder<VMTemplateVO> TemplatesWithNoChecksumSearch = _templateDao.createSearchBuilder();
        TemplatesWithNoChecksumSearch.and("checksum", TemplatesWithNoChecksumSearch.entity().getChecksum(), SearchCriteria.Op.NULL);

        ReadyTemplateStatesSearch.join("vm_template", TemplatesWithNoChecksumSearch, TemplatesWithNoChecksumSearch.entity().getId(),
                ReadyTemplateStatesSearch.entity().getTemplateId(), JoinBuilder.JoinType.INNER);
        TemplatesWithNoChecksumSearch.done();
        ReadyTemplateStatesSearch.done();

        return true;
    }

    @Override
    public boolean start() {
        _timer = new Timer();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public boolean isTemplateUpdateable(Long templateId, Long storeId) {
        List<TemplateDataStoreVO> downloadsInProgress = _vmTemplateStoreDao.listByTemplateStoreDownloadStatus(templateId, storeId,
                Status.DOWNLOAD_IN_PROGRESS, Status.DOWNLOADED);
        return (downloadsInProgress.size() == 0);
    }

    private void initiateTemplateDownload(DataObject template, DataStore store, AsyncCompletionCallback<DownloadAnswer> callback) {
        boolean downloadJobExists = false;
        TemplateDataStoreVO vmTemplateStore = null;

        vmTemplateStore = _vmTemplateStoreDao.findByStoreTemplate(store.getId(), template.getId());
        if (vmTemplateStore == null) {
            // This method can be invoked other places, for example,
            // handleTemplateSync, in that case, vmTemplateStore may be null
            vmTemplateStore = new TemplateDataStoreVO(store.getId(), template.getId(), new Date(), 0,
                    VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED, null, null, "jobid0000", null, template.getUri());
            _vmTemplateStoreDao.persist(vmTemplateStore);
        } else if ((vmTemplateStore.getJobId() != null) && (vmTemplateStore.getJobId().length() > 2)) {
            downloadJobExists = true;
        }

        Long maxTemplateSizeInBytes = getMaxTemplateSizeInBytes();
        if (vmTemplateStore != null) {
            start();
            VirtualMachineTemplate tmpl = this._templateDao.findById(template.getId());
            DownloadCommand dcmd = new DownloadCommand(store.getTO(), tmpl, maxTemplateSizeInBytes);
            dcmd.setProxy(getHttpProxy());
            if (downloadJobExists) {
                dcmd = new DownloadProgressCommand(dcmd, vmTemplateStore.getJobId(), RequestType.GET_OR_RESTART);
            }
            if (vmTemplateStore.isCopy()) {
                dcmd.setCreds(TemplateConstants.DEFAULT_HTTP_AUTH_USER, _copyAuthPasswd);
            }
            EndPoint ep = _epSelector.select(template);
            if (ep == null) {
                s_logger.warn("There is no secondary storage VM for downloading template to image store " + store.getName());
                return;
            }
            DownloadListener dl = new DownloadListener(ep, store, template, _timer, this, dcmd,
                     callback);
            if (downloadJobExists) {
                // due to handling existing download job issues, we still keep
                // downloadState in template_store_ref to avoid big change in
                // DownloadListener to use
                // new ObjectInDataStore.State transition. TODO: fix this later
                // to be able to remove downloadState from template_store_ref.
                dl.setCurrState(vmTemplateStore.getDownloadState());
            }
            DownloadListener old = null;
            synchronized (_listenerTemplateMap) {
                old = _listenerTemplateMap.put(vmTemplateStore, dl);
            }
            if (old != null) {
                old.abandon();
            }

            try {
                ep.sendMessageAsyncWithListener(dcmd, dl);
            } catch (Exception e) {
                s_logger.warn("Unable to start /resume download of template " + template.getId() + " to " + store.getName(), e);
                dl.setDisconnected();
                dl.scheduleStatusCheck(RequestType.GET_OR_RESTART);
            }
        }
    }


    @Override
    public void downloadTemplateToStorage(DataObject template, DataStore store, AsyncCompletionCallback<DownloadAnswer> callback) {
        long templateId = template.getId();
        if (isTemplateUpdateable(templateId, store.getId())) {
            if (template != null && template.getUri() != null) {
                initiateTemplateDownload(template, store, callback);
            }
        }
    }

    @Override
    public void downloadVolumeToStorage(DataObject volume, DataStore store, String url, String checkSum, ImageFormat format,
            AsyncCompletionCallback<DownloadAnswer> callback) {
        boolean downloadJobExists = false;
        VolumeDataStoreVO volumeHost = null;

        volumeHost = _volumeStoreDao.findByStoreVolume(store.getId(), volume.getId());
        if (volumeHost == null) {
            volumeHost = new VolumeDataStoreVO(store.getId(), volume.getId(), new Date(), 0, VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED,
                    null, null, "jobid0000", null, url, checkSum, format);
            _volumeStoreDao.persist(volumeHost);
        } else if ((volumeHost.getJobId() != null) && (volumeHost.getJobId().length() > 2)) {
            downloadJobExists = true;
        }

        Long maxVolumeSizeInBytes = getMaxVolumeSizeInBytes();
        if (volumeHost != null) {
            start();
            Volume vol = this._volumeDao.findById(volume.getId());
            DownloadCommand dcmd = new DownloadCommand(store.getTO(), vol, maxVolumeSizeInBytes, checkSum, url, format);
            dcmd.setProxy(getHttpProxy());
            if (downloadJobExists) {
                dcmd = new DownloadProgressCommand(dcmd, volumeHost.getJobId(), RequestType.GET_OR_RESTART);
                dcmd.setResourceType(ResourceType.VOLUME);
            }

            EndPoint ep = this._epSelector.select(volume);
            if (ep == null) {
                s_logger.warn("There is no secondary storage VM for image store " + store.getName());
                return;
            }
            DownloadListener dl = new DownloadListener(ep, store, volume, _timer, this, dcmd, callback);

            if (downloadJobExists) {
                dl.setCurrState(volumeHost.getDownloadState());
            }
            DownloadListener old = null;
            synchronized (_listenerVolMap) {
                old = _listenerVolMap.put(volumeHost, dl);
            }
            if (old != null) {
                old.abandon();
            }

            try {
                ep.sendMessageAsyncWithListener(dcmd, dl);
            } catch (Exception e) {
                s_logger.warn("Unable to start /resume download of volume " + volume.getId() + " to " + store.getName(), e);
                dl.setDisconnected();
                dl.scheduleStatusCheck(RequestType.GET_OR_RESTART);
            }
        }
    }




    private Long getMaxTemplateSizeInBytes() {
        try {
            return Long.parseLong(_configDao.getValue("max.template.iso.size")) * 1024L * 1024L * 1024L;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getMaxVolumeSizeInBytes() {
        try {
            return Long.parseLong(_configDao.getValue("storage.max.volume.upload.size")) * 1024L * 1024L * 1024L;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Proxy getHttpProxy() {
        if (_proxy == null) {
            return null;
        }
        try {
            URI uri = new URI(_proxy);
            Proxy prx = new Proxy(uri);
            return prx;
        } catch (URISyntaxException e) {
            return null;
        }
    }

}
