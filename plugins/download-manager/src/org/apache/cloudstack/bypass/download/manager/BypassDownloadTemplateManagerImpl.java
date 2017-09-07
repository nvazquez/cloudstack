package org.apache.cloudstack.bypass.download.manager;

import javax.inject.Inject;

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
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.component.ManagerBase;

public class BypassDownloadTemplateManagerImpl extends ManagerBase implements BypassDownloadTemplateManager {

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

    private static final Logger s_logger = Logger.getLogger(BypassDownloadTemplateManagerImpl.class);

    @Override
    public boolean downloadTemplateToStoragePool(long templateId, long poolId, long hostId) throws AgentUnavailableException, OperationTimedoutException {
        VMTemplateVO templateVO = templateDao.findById(templateId);
        StoragePoolVO poolVO = poolDao.findById(poolId);
        HostVO hostVO = hostDao.findById(hostId);
        String url = templateVO.getUrl();
        String poolUuid = poolVO.getUuid();
        StoragePoolType poolType = poolVO.getPoolType();
        final DownloadBypassedTemplateCommand cmd =
                new DownloadBypassedTemplateCommand(DownloadProtocol.HTTP, url, poolUuid, poolType);
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
