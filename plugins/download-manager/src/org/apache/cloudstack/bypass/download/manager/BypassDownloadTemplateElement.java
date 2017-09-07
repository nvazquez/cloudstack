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

package org.apache.cloudstack.bypass.download.manager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.bypass.download.DownloadBypassedTemplateCmd;
import org.apache.cloudstack.bypass.download.manager.DownloadBypassedTemplateCommand.DownloadProtocol;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.component.AdapterBase;

public class BypassDownloadTemplateElement extends AdapterBase implements DownloadBypassedTemplateService {

    @Inject
    protected VMTemplateDao templateDao;
    @Inject
    protected PrimaryDataStoreDao poolDao;
    @Inject
    protected HostDao hostDao;
    @Inject
    protected AgentManager agentManager;
    @Inject
    protected VMTemplatePoolDao vmTemplatePoolDao;

    private static BypassDownloadTemplateElement bypassDownloadTemplateElement = null;
    private static final Logger s_logger = Logger.getLogger(BypassDownloadTemplateElement.class);

    protected BypassDownloadTemplateElement() {
    }

    public static BypassDownloadTemplateElement getInstance() {
        if (bypassDownloadTemplateElement == null) {
            bypassDownloadTemplateElement = new BypassDownloadTemplateElement();
        }
        return bypassDownloadTemplateElement;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(DownloadBypassedTemplateCmd.class);
        return cmdList;
    }

    @Override
    public boolean downloadTemplateToStoragePool(long templateId, long poolId, long hostId) throws AgentUnavailableException, OperationTimedoutException {
        VMTemplateVO templateVO = templateDao.findById(templateId);
        StoragePoolVO poolVO = poolDao.findById(poolId);
        HostVO hostVO = hostDao.findById(hostId);
        String url = templateVO.getUrl();
        String poolUuid = poolVO.getUuid();
        StoragePoolType poolType = poolVO.getPoolType();
        final DownloadBypassedTemplateCommand cmd =
                new DownloadBypassedTemplateCommand(DownloadProtocol.HTTP, url, poolVO.getUuid(), poolVO.getPoolType());
        s_logger.debug("Downloading bypassed template from: " + url + " to pool: " + poolUuid);
        DownloadBypassedTemplateAnswer answer = null;
        if (hostVO.getHypervisorType().equals(HypervisorType.KVM)) {
            s_logger.debug("Sending cmd to host " + hostVO.getId());
            answer = (DownloadBypassedTemplateAnswer) agentManager.send(hostVO.getId(), cmd);
        }
        else {
            //EndPoint ep = endpointSelector.select(dataStore);
            //answer = (DownloadBypassedTemplateAnswer) ep.sendMessage(cmd);
            s_logger.debug("Select SSVM");
        }
        s_logger.debug("Answer received: " + answer.getResult());
        if (answer.getResult()) {
            CallContext.current().setEventDetails("Successfully downloaded template from: " + url + " to pool: " + poolUuid);
            updateTemplateStoreRef(answer, poolId, templateId);
            return true;
        }
        else {
            return false;
        }
    }

    protected void updateTemplateStoreRef(DownloadBypassedTemplateAnswer answer, long poolId, long templateId) {
        s_logger.debug("Persisting entry on template_store_ref");
        VMTemplateStoragePoolVO templateStoreRef = vmTemplatePoolDao.findByPoolTemplate(poolId, templateId);
        if (templateStoreRef == null) {
            templateStoreRef = new VMTemplateStoragePoolVO(poolId, templateId);
        }
        templateStoreRef.setDownloadPercent(answer.getDownloadPct());
        templateStoreRef.setInstallPath(answer.getPath());
        templateStoreRef.setLocalDownloadPath(answer.getPath());
        templateStoreRef.setTemplateSize(answer.getSize());
        if (answer.getStatus().equalsIgnoreCase("DOWNLOADED")) {
            templateStoreRef.setDownloadState(Status.DOWNLOADED);
        }
        templateStoreRef.setState(State.Ready);
        vmTemplatePoolDao.persist(templateStoreRef);
    }
}
