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
package com.cloud.hypervisor.vmware.util;

import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vcenter.VM;
import com.vmware.vcenter.VMTypes;
import com.vmware.vcenter.vm.GuestOS;
import com.vmware.vcenter.vm.hardware.DiskTypes;
import com.vmware.vcenter.vm.hardware.EthernetTypes;
import com.vmware.vcenter.vm.hardware.boot.DeviceTypes;
import vmware.samples.common.authentication.VapiAuthenticationHelper;
import vmware.samples.vcenter.helpers.NetworkHelper;
import vmware.samples.vcenter.helpers.PlacementHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VmwareAutomationHelper {

    public VmwareAutomationHelper() {
    }

    public void createVM(VapiAuthenticationHelper vapiAuthHelper, StubConfiguration sessionStubConfig, VM vmService, String vmName) {
        String hostName = null;
        String clusterName = "p1-c1";
        String datacenterName = "Trillian";
        String vmFolderName = "Folder";
        String datastoreName = "17218111b7fc38f3a2f170cdc3128dbb";
        String standardPortgroupName = "cloud.guest.1455.200.1-vSwitch1";

        VMTypes.PlacementSpec vmPlacementSpec = PlacementHelper.getVMPlacementSpec(vapiAuthHelper.getStubFactory(),
                sessionStubConfig, hostName, clusterName, datacenterName, vmFolderName, datastoreName);
        String standardNetworkBacking = NetworkHelper.getStandardNetworkBacking(vapiAuthHelper.getStubFactory(), sessionStubConfig,
                datacenterName, standardPortgroupName);
        createBasicVM(vmPlacementSpec, standardNetworkBacking, vmService, vmName);
    }

    private void createBasicVM(VMTypes.PlacementSpec vmPlacementSpec, String standardNetworkBacking, VM vmService, String vmName) {
        DiskTypes.CreateSpec bootDiskCreateSpec = (new DiskTypes.CreateSpec.Builder()).setType(DiskTypes.HostBusAdapterType.SCSI).setScsi((new com.vmware.vcenter.vm.hardware.ScsiAddressSpec.Builder(0L)).setUnit(0L).build()).setNewVmdk(new DiskTypes.VmdkCreateSpec()).build();
        DiskTypes.CreateSpec dataDiskCreateSpec = (new DiskTypes.CreateSpec.Builder()).setNewVmdk(new DiskTypes.VmdkCreateSpec()).build();
        List<DiskTypes.CreateSpec> disks = Arrays.asList(bootDiskCreateSpec, dataDiskCreateSpec);
        EthernetTypes.BackingSpec nicBackingSpec = (new com.vmware.vcenter.vm.hardware.EthernetTypes.BackingSpec.Builder(EthernetTypes.BackingType.STANDARD_PORTGROUP)).setNetwork(standardNetworkBacking).build();
        com.vmware.vcenter.vm.hardware.EthernetTypes.CreateSpec nicCreateSpec = (new com.vmware.vcenter.vm.hardware.EthernetTypes.CreateSpec.Builder()).setStartConnected(true).setBacking(nicBackingSpec).build();
        List<com.vmware.vcenter.vm.hardware.EthernetTypes.CreateSpec> nics = Collections.singletonList(nicCreateSpec);
        List<DeviceTypes.EntryCreateSpec> bootDevices = Arrays.asList((new com.vmware.vcenter.vm.hardware.boot.DeviceTypes.EntryCreateSpec.Builder(DeviceTypes.Type.ETHERNET)).build(), (new com.vmware.vcenter.vm.hardware.boot.DeviceTypes.EntryCreateSpec.Builder(DeviceTypes.Type.DISK)).build());
        com.vmware.vcenter.VMTypes.CreateSpec vmCreateSpec = (new com.vmware.vcenter.VMTypes.CreateSpec.Builder(GuestOS.WINDOWS_9_64)).setName(vmName).setBootDevices(bootDevices).setPlacement(vmPlacementSpec).setNics(nics).setDisks(disks).build();
        System.out.println("\n\n#### Example: Creating Basic VM with spec:\n" + vmCreateSpec);
        String basicVMId = vmService.create(vmCreateSpec);
        VMTypes.Info vmInfo = vmService.get(basicVMId);
        System.out.println("\nBasic VM Info:\n" + vmInfo);
    }
}
