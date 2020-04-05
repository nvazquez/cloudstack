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
package com.cloud.hypervisor.vmware.mo;

import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vcenter.VM;
import com.vmware.vcenter.VMTypes;
import org.junit.Test;
import vmware.samples.common.SslUtil;
import vmware.samples.common.authentication.VapiAuthenticationHelper;
import vmware.samples.common.authentication.VimAuthenticationHelper;
import vmware.samples.vcenter.helpers.DatacenterHelper;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class VmwareAutomationTest {

    private VapiAuthenticationHelper vapiAuthHelper = new VapiAuthenticationHelper();
    private VimAuthenticationHelper vimAuthHelper = new VimAuthenticationHelper();
    private StubConfiguration sessionStubConfig;
    private VM vmService;

    private final String vCenterIp = "10.2.2.195";
    private final String username = "administrator@vsphere.local";
    private final String password = "P@ssword123";
    private final String datacenterName = "Trillian";

    protected HttpConfiguration.SslConfiguration buildSslConfiguration() throws Exception {
        HttpConfiguration.SslConfiguration sslConfig;
        SslUtil.trustAllHttpsCertificates();
        sslConfig = (new com.vmware.vapi.protocol.HttpConfiguration.SslConfiguration.Builder()).disableCertificateValidation().disableHostnameVerification().getConfig();
        return sslConfig;
    }

    protected HttpConfiguration buildHttpConfiguration() throws Exception {
        HttpConfiguration httpConfig = (new HttpConfiguration.Builder()).setSslConfiguration(this.buildSslConfiguration()).getConfig();
        return httpConfig;
    }

    protected void run() throws Exception {
        VMTypes.FilterSpec.Builder bldr = new VMTypes.FilterSpec.Builder();
        if (null != datacenterName && !datacenterName.isEmpty()) {
            bldr.setDatacenters(Collections.singleton(DatacenterHelper.getDatacenter(vapiAuthHelper.getStubFactory(), sessionStubConfig, datacenterName)));
        }
        /**
        if (null != this.clusterName && !this.clusterName.isEmpty()) {
            bldr.setClusters(Collections.singleton(ClusterHelper.getCluster(this.vapiAuthHelper.getStubFactory(), this.sessionStubConfig, this.clusterName)));
        }

        if (null != this.vmFolderName && !this.vmFolderName.isEmpty()) {
            bldr.setFolders(Collections.singleton(FolderHelper.getFolder(this.vapiAuthHelper.getStubFactory(), this.sessionStubConfig, this.vmFolderName)));
        }
        **/

        List<VMTypes.Summary> vmList = vmService.list(bldr.build());
        System.out.println("----------------------------------------");
        System.out.println("List of VMs");
        Iterator var3 = vmList.iterator();

        while(var3.hasNext()) {
            VMTypes.Summary vmSummary = (VMTypes.Summary)var3.next();
            System.out.println(vmSummary);
        }

        System.out.println("----------------------------------------");
    }

    @Test
    public void testAutomation() throws Exception {
        HttpConfiguration httpConfig = buildHttpConfiguration();
        sessionStubConfig = vapiAuthHelper.loginByUsernameAndPassword(vCenterIp, username, password, httpConfig);
        vimAuthHelper.loginByUsernameAndPassword(vCenterIp, username, password);

        vmService = (VM)this.vapiAuthHelper.getStubFactory().createStub(VM.class, sessionStubConfig);
        run();

        vapiAuthHelper.logout();
        vimAuthHelper.logout();
    }
}
