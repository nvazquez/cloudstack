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

package org.apache.cloudstack.api.command.bypass.download;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DownloadBypassedTemplateResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.bypass.download.manager.DownloadBypassedTemplateService;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@APICommand(name = "downloadBypassedTemplate",
        responseObject = DownloadBypassedTemplateResponse.class,
        description = "Download template, bypass secondary storage.",
        since = "4.11.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class DownloadBypassedTemplateCmd extends BaseAsyncCmd {

    @Inject
    DownloadBypassedTemplateService service; //services can be added

    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class,
            required = true, description = "the id of the template")
    private Long templateId;

    @Parameter(name = ApiConstants.POOL_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class,
            required = true, description = "the Id of the storage pool")
    private Long poolId;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            required = true, description = "the Id of the host")
    private Long hostId;

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DOWNLOAD_BYPASSED_TEMPLATE_TO_POOL;
    }

    @Override
    public String getEventDescription() {
        return "Downloading bypassed template to pool";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            boolean result = service.downloadTemplateToStoragePool(templateId, poolId, hostId);
            DownloadBypassedTemplateResponse response = new DownloadBypassedTemplateResponse();
            response.setDownloadResult(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to download template to pool: " + e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return "downloadbypassedtemplateresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

}
