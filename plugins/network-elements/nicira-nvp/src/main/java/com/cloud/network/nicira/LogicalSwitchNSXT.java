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
package com.cloud.network.nicira;

import java.util.List;

public class LogicalSwitchNSXT extends BaseNSXTEntity {

    public static final String ADMIN_MODE_UP = "UP";
    public static final String ADMIN_MODE_DOWN = "DOWN";
    public static final String REPLICATION_MODE_MTEP = "MTEP";
    public static final String REPLICATION_MODE_SOURCE = "SOURCE";

    private final String resourceType = "LogicalSwitch";
    private String adminState;
    private String replicationMode;
    private String transportZoneId;
    private int vni;
    private List<SwitchingProfileTypeIdEntry> switchingProfileIds;

    public LogicalSwitchNSXT(final String displayName, final String transportZoneId, final String adminMode){
        this.displayName = displayName;
        this.transportZoneId = transportZoneId;
        this.adminState = adminMode;
    }

    public String getAdminState() {
        return adminState;
    }
    public String getReplicationMode() {
        return replicationMode;
    }
    public String getTransportZoneId() {
        return transportZoneId;
    }
    public int getVni() {
        return vni;
    }
    public List<SwitchingProfileTypeIdEntry> getSwitchingProfileIds() {
        return switchingProfileIds;
    }
    public String getResourceType() {
        return resourceType;
    }

}
