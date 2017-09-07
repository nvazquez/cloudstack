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

import com.cloud.agent.api.Command;
import com.cloud.storage.Storage.StoragePoolType;

public class DownloadBypassedTemplateCommand extends Command {

    public enum DownloadProtocol {
        HTTP, NFS
    }

    private String templateDownloadUrl;
    private String destPoolUuid;
    private StoragePoolType destPoolType;
    private DownloadProtocol protocol;

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public DownloadBypassedTemplateCommand(final DownloadProtocol protocol, final String url, final String poolUuid, final StoragePoolType poolType) {
        this.protocol = protocol;
        this.templateDownloadUrl = url;
        this.destPoolUuid = poolUuid;
        this.destPoolType = poolType;
    }

    public String getTemplateDownloadUrl() {
        return templateDownloadUrl;
    }

    public String getDestPoolUuid() {
        return destPoolUuid;
    }

    public StoragePoolType getDestPoolType() {
        return destPoolType;
    }

    public DownloadProtocol getDownloadProtocol() {
        return protocol;
    }
}
