//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.cloudstack.bypass.download.manager.DownloadBypassedTemplateAnswer;
import org.apache.cloudstack.bypass.download.manager.DownloadBypassedTemplateCommand;
import org.apache.cloudstack.bypass.download.manager.DownloadBypassedTemplateCommand.DownloadProtocol;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.BypassTemplateInfo;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;

@ResourceWrapper(handles = DownloadBypassedTemplateCommand.class)
public class LibvirtDownloadBypassTemplateCommandWrapper extends CommandWrapper<DownloadBypassedTemplateCommand, DownloadBypassedTemplateAnswer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtDownloadBypassTemplateCommandWrapper.class);

    @Override
    public DownloadBypassedTemplateAnswer execute(DownloadBypassedTemplateCommand command, LibvirtComputingResource serverResource) {
        s_logger.info("Download template bypass secondary storage");
        String downloadUrl = command.getTemplateDownloadUrl();
        String poolUuid = command.getDestPoolUuid();
        StoragePoolType poolType = command.getDestPoolType();
        DownloadProtocol protocol = command.getDownloadProtocol();
        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        final KVMStoragePool pool = storagePoolMgr.getStoragePool(poolType, poolUuid);
        BypassTemplateInfo info = pool.downloadTemplate(downloadUrl, protocol);
        if (info != null) {
            return new DownloadBypassedTemplateAnswer(true, info.getSize(),
                    info.getChecksum(), info.getPath(), info.getStatus(), info.getDownloadPct());
        }
        return new DownloadBypassedTemplateAnswer(false);
    }

}
