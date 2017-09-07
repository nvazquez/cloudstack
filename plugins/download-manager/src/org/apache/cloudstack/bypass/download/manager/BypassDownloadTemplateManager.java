package org.apache.cloudstack.bypass.download.manager;

import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;

public interface BypassDownloadTemplateManager {

    boolean downloadTemplateToStoragePool(long templateId, long poolId, long hostId) throws AgentUnavailableException, OperationTimedoutException;
}
