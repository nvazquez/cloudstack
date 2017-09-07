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

package org.apache.cloudstack.bypass.download.manager;

import com.cloud.agent.api.Answer;

public class DownloadBypassedTemplateAnswer extends Answer {

    private long size;
    private String checksum;
    private String path;
    private String status;
    private int downloadPct;

    public DownloadBypassedTemplateAnswer(final boolean result, final long size, final String checksum, final String path,
            final String status, final int downloadPct) {
        super();
        this.result = result;
        this.size = size;
        this.checksum = checksum;
        this.path = path;
        this.status = status;
        this.downloadPct = downloadPct;
    }

    public DownloadBypassedTemplateAnswer(final boolean result) {
        super();
        this.result = result;
    }

    public long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getPath() {
        return path;
    }

    public String getStatus() {
        return status;
    }

    public int getDownloadPct() {
        return downloadPct;
    }
}
