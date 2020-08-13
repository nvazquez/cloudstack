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
package com.cloud.agent.api.storage;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public class OVFHelperTest {

    private String ovfFileProductSection =
        "<ProductSection>" +
            "<Info>VM Arguments</Info>" +
            "<Property ovf:key=\"va-ssh-public-key\" ovf:type=\"string\" ovf:userConfigurable=\"true\" ovf:value=\"\">" +
                "<Label>Set the SSH public key allowed to access the appliance</Label>" +
                "<Description>This will enable the SSHD service and configure the specified public key</Description>" +
            "</Property>" +
            "<Property ovf:key=\"user-data\" ovf:type=\"string\" ovf:userConfigurable=\"true\" ovf:value=\"\">" +
                "<Label>User data to be made available inside the instance</Label>" +
                "<Description>This allows to pass any text to the appliance. The value should be encoded in base64</Description>" +
            "</Property>" +
        "</ProductSection>";

    private String ovfFileDeploymentOptionsSection =
            "<DeploymentOptionSection>\n" +
                    "    <Info>Deployment Configuration information</Info>\n" +
                    "    <Configuration ovf:id=\"ASAv5\">\n" +
                    "      <Label>100 Mbps (ASAv5)</Label>\n" +
                    "      <Description>Use this option to deploy an ASAv with a maximum throughput of 100 Mbps (uses 1 vCPU and 2 GB of memory).</Description>\n" +
                    "    </Configuration>\n" +
                    "    <Configuration ovf:id=\"ASAv10\">\n" +
                    "      <Label>1 Gbps (ASAv10)</Label>\n" +
                    "      <Description>Use this option to deploy an ASAv with a maximum throughput of 1 Gbps (uses 1 vCPU and 2 GB of memory).</Description>\n" +
                    "    </Configuration>\n" +
                    "    <Configuration ovf:id=\"ASAv30\">\n" +
                    "      <Label>2 Gbps (ASAv30)</Label>\n" +
                    "      <Description>Use this option to deploy an ASAv with a maximum throughput of 2 Gbps (uses 4 vCPUs and 8 GB of memory).</Description>\n" +
                    "    </Configuration>\n" +
                    "  </DeploymentOptionSection>";

    private String ovfFileVirtualHardwareSection =
            "<VirtualHardwareSection ovf:transport=\"iso\">\n" +
                    "      <Info>Virtual hardware requirements</Info>\n" +
                    "      <System>\n" +
                    "        <vssd:ElementName>Virtual Hardware Family</vssd:ElementName>\n" +
                    "        <vssd:InstanceID>0</vssd:InstanceID>\n" +
                    "        <vssd:VirtualSystemIdentifier>ASAv</vssd:VirtualSystemIdentifier>\n" +
                    "        <vssd:VirtualSystemType>vmx-08,vmx-09</vssd:VirtualSystemType>\n" +
                    "      </System>\n" +
                    "      <Item ovf:configuration=\"ASAv5 ASAv10\">\n" +
                    "        <rasd:AllocationUnits>hertz * 10^6</rasd:AllocationUnits>\n" +
                    "        <rasd:Description>Number of Virtual CPUs</rasd:Description>\n" +
                    "        <rasd:ElementName>1 virtual CPU(s)</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>1</rasd:InstanceID>\n" +
                    "        <rasd:Limit>5000</rasd:Limit>\n" +
                    "        <rasd:Reservation>1000</rasd:Reservation>\n" +
                    "        <rasd:ResourceType>3</rasd:ResourceType>\n" +
                    "        <rasd:VirtualQuantity>1</rasd:VirtualQuantity>\n" +
                    "      </Item>\n" +
                    "      <Item ovf:configuration=\"ASAv30\">\n" +
                    "        <rasd:AllocationUnits>hertz * 10^6</rasd:AllocationUnits>\n" +
                    "        <rasd:Description>Number of Virtual CPUs</rasd:Description>\n" +
                    "        <rasd:ElementName>4 virtual CPU(s)</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>1</rasd:InstanceID>\n" +
                    "        <rasd:Limit>20000</rasd:Limit>\n" +
                    "        <rasd:Reservation>1000</rasd:Reservation>\n" +
                    "        <rasd:ResourceType>3</rasd:ResourceType>\n" +
                    "        <rasd:VirtualQuantity>4</rasd:VirtualQuantity>\n" +
                    "      </Item>\n" +
                    "      <Item ovf:configuration=\"ASAv5 ASAv10\">\n" +
                    "        <rasd:AllocationUnits>byte * 2^20</rasd:AllocationUnits>\n" +
                    "        <rasd:Description>Memory Size</rasd:Description>\n" +
                    "        <rasd:ElementName>2048MB of memory</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>2</rasd:InstanceID>\n" +
                    "        <rasd:Limit>2048</rasd:Limit>\n" +
                    "        <rasd:Reservation>2048</rasd:Reservation>\n" +
                    "        <rasd:ResourceType>4</rasd:ResourceType>\n" +
                    "        <rasd:VirtualQuantity>2048</rasd:VirtualQuantity>\n" +
                    "      </Item>\n" +
                    "      <Item ovf:configuration=\"ASAv30\">\n" +
                    "        <rasd:AllocationUnits>byte * 2^20</rasd:AllocationUnits>\n" +
                    "        <rasd:Description>Memory Size</rasd:Description>\n" +
                    "        <rasd:ElementName>8192MB of memory</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>2</rasd:InstanceID>\n" +
                    "        <rasd:Limit>8192</rasd:Limit>\n" +
                    "        <rasd:Reservation>8192</rasd:Reservation>\n" +
                    "        <rasd:ResourceType>4</rasd:ResourceType>\n" +
                    "        <rasd:VirtualQuantity>8192</rasd:VirtualQuantity>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:Address>0</rasd:Address>\n" +
                    "        <rasd:Description>SCSI Controller</rasd:Description>\n" +
                    "        <rasd:ElementName>SCSI controller 0</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>3</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>lsilogic</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>6</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:Address>0</rasd:Address>\n" +
                    "        <rasd:Description>IDE Controller</rasd:Description>\n" +
                    "        <rasd:ElementName>IDE 0</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>4</rasd:InstanceID>\n" +
                    "        <rasd:ResourceType>5</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item ovf:required=\"false\">\n" +
                    "        <rasd:AddressOnParent>0</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:ElementName>CD/DVD Drive</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>5</rasd:InstanceID>\n" +
                    "        <rasd:Parent>4</rasd:Parent>\n" +
                    "        <rasd:ResourceType>15</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item ovf:required=\"false\">\n" +
                    "        <rasd:AddressOnParent>1</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:ElementName>CD/DVD Drive</rasd:ElementName>\n" +
                    "        <rasd:HostResource>ovf:/file/file3</rasd:HostResource>\n" +
                    "        <rasd:InstanceID>18</rasd:InstanceID>\n" +
                    "        <rasd:Parent>4</rasd:Parent>\n" +
                    "        <rasd:ResourceType>15</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>7</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>Management0-0</rasd:Connection>\n" +
                    "        <rasd:Description>E1000 Ethernet adapter on \"Management Network\"</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 1</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>6</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>0</rasd:AddressOnParent>\n" +
                    "        <rasd:ElementName>Hard disk 1</rasd:ElementName>\n" +
                    "        <rasd:HostResource>ovf:/disk/vmdisk1</rasd:HostResource>\n" +
                    "        <rasd:InstanceID>7</rasd:InstanceID>\n" +
                    "        <rasd:Parent>3</rasd:Parent>\n" +
                    "        <rasd:ResourceType>17</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>1</rasd:AddressOnParent>\n" +
                    "        <rasd:ElementName>Hard disk 2</rasd:ElementName>\n" +
                    "        <rasd:HostResource>ovf:/disk/vmdisk2</rasd:HostResource>\n" +
                    "        <rasd:InstanceID>8</rasd:InstanceID>\n" +
                    "        <rasd:Parent>3</rasd:Parent>\n" +
                    "        <rasd:ResourceType>17</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>8</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-0</rasd:Connection>\n" +
                    "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 2</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>9</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>9</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-1</rasd:Connection>\n" +
                    "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 3</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>10</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>10</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-2</rasd:Connection>\n" +
                    "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 4</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>11</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>11</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-3</rasd:Connection>\n" +
                    "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 5</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>12</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>12</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-4</rasd:Connection>\n" +
                    "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 6</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>13</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>13</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-5</rasd:Connection>\n" +
                    "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 7</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>14</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>14</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-6</rasd:Connection>\n" +
                    "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 8</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>15</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>15</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-7</rasd:Connection>\n" +
                    "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 9</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>16</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <Item>\n" +
                    "        <rasd:AddressOnParent>16</rasd:AddressOnParent>\n" +
                    "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                    "        <rasd:Connection>GigabitEthernet0-8</rasd:Connection>\n" +
                    "        <rasd:Description>Default HA failover E1000 Ethernet adapter, or additional standalone general purpose adapter</rasd:Description>\n" +
                    "        <rasd:ElementName>Network adapter 10</rasd:ElementName>\n" +
                    "        <rasd:InstanceID>17</rasd:InstanceID>\n" +
                    "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                    "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                    "      </Item>\n" +
                    "      <vmw:ExtraConfig vmw:key=\"monitor_control.pseudo_perfctr\" vmw:value=\"TRUE\"></vmw:ExtraConfig>\n" +
                    "    </VirtualHardwareSection>";

    private String eulaSections =
            "<VirtualSystem>\n" +
            "<EulaSection>\n" +
            "      <Info>end-user license agreement</Info>\n" +
            "      <License>END USER LICENSE AGREEMENT\n" +
            "\n" +
            "IMPORTANT: PLEASE READ THIS END USER LICENSE AGREEMENT CAREFULLY. IT IS VERY IMPORTANT THAT YOU CHECK THAT YOU ARE PURCHASING CISCO SOFTWARE OR EQUIPMENT FROM AN APPROVED SOURCE AND THAT YOU, OR THE ENTITY YOU REPRESENT (COLLECTIVELY, THE \"CUSTOMER\") HAVE BEEN REGISTERED AS THE END USER FOR THE PURPOSES OF THIS CISCO END USER LICENSE AGREEMENT. IF YOU ARE NOT REGISTERED AS THE END USER YOU HAVE NO LICENSE TO USE THE SOFTWARE AND THE LIMITED WARRANTY IN THIS END USER LICENSE AGREEMENT DOES NOT APPLY. ASSUMING YOU HAVE PURCHASED FROM AN APPROVED SOURCE, DOWNLOADING, INSTALLING OR USING CISCO OR CISCO-SUPPLIED SOFTWARE CONSTITUTES ACCEPTANCE OF THIS AGREEMENT.\n" +
            "\n" +
            "CISCO SYSTEMS, INC. OR ITS AFFILIATE LICENSING THE SOFTWARE (\"CISCO\") IS WILLING TO LICENSE THIS SOFTWARE TO YOU ONLY UPON THE CONDITION THAT YOU PURCHASED THE SOFTWARE FROM AN APPROVED SOURCE AND THAT YOU ACCEPT ALL OF THE TERMS CONTAINED IN THIS END USER LICENSE AGREEMENT PLUS ANY ADDITIONAL LIMITATIONS ON THE LICENSE SET FORTH IN A SUPPLEMENTAL LICENSE AGREEMENT ACCOMPANYING THE PRODUCT, MADE AVAILABLE AT THE TIME OF YOUR ORDER, OR POSTED ON THE CISCO WEBSITE AT www.cisco.com/go/terms (COLLECTIVELY THE \"AGREEMENT\"). TO THE EXTENT OF ANY CONFLICT BETWEEN THE TERMS OF THIS END USER LICENSE AGREEMENT AND ANY SUPPLEMENTAL LICENSE AGREEMENT, THE SUPPLEMENTAL LICENSE AGREEMENT SHALL APPLY. BY DOWNLOADING, INSTALLING, OR USING THE SOFTWARE, YOU ARE REPRESENTING THAT YOU PURCHASED THE SOFTWARE FROM AN APPROVED SOURCE AND BINDING YOURSELF TO THE AGREEMENT. IF YOU DO " +
                    "NOT AGREE TO ALL OF THE TERMS OF THE AGREEMENT, THEN CISCO IS UNWILLING TO LICENSE THE SOFTWARE TO YOU AND (A) YOU MAY NOT DOWNLOAD, INSTALL OR USE THE SOFTWARE, AND (B) YOU MAY RETURN THE SOFTWARE (INCLUDING ANY UNOPENED CD PACKAGE AND ANY WRITTEN MATERIALS) FOR A FULL REFUND, OR, IF THE SOFTWARE AND WRITTEN MATERIALS ARE SUPPLIED AS PART OF ANOTHER PRODUCT, YOU MAY RETURN THE ENTIRE PRODUCT FOR A FULL REFUND. YOUR RIGHT TO RETURN AND REFUND EXPIRES 30 DAYS AFTER PURCHASE FROM AN APPROVED SOURCE, AND APPLIES ONLY IF YOU ARE THE ORIGINAL AND REGISTERED END USER PURCHASER. FOR THE PURPOSES OF THIS END USER LICENSE AGREEMENT, AN \"APPROVED SOURCE\" MEANS (A) CISCO; OR (B) A DISTRIBUTOR OR SYSTEMS INTEGRATOR AUTHORIZED BY CISCO TO DISTRIBUTE / SELL CISCO EQUIPMENT, SOFTWARE AND SERVICES WITHIN YOUR TERRITORY TO END " +
                    "USERS; OR (C) A RESELLER AUTHORIZED BY ANY SUCH DISTRIBUTOR OR SYSTEMS INTEGRATOR IN ACCORDANCE WITH THE TERMS OF THE DISTRIBUTOR'S AGREEMENT WITH CISCO TO DISTRIBUTE / SELL THE CISCO EQUIPMENT, SOFTWARE AND SERVICES WITHIN YOUR TERRITORY TO END USERS.\n" +
            "\n" +
            "THE FOLLOWING TERMS OF THE AGREEMENT GOVERN CUSTOMER'S USE OF THE SOFTWARE (DEFINED BELOW), EXCEPT TO THE EXTENT: (A) THERE IS A SEPARATE SIGNED CONTRACT BETWEEN CUSTOMER AND CISCO GOVERNING CUSTOMER'S USE OF THE SOFTWARE, OR (B) THE SOFTWARE INCLUDES A SEPARATE \"CLICK-ACCEPT\" LICENSE AGREEMENT OR THIRD PARTY LICENSE AGREEMENT AS PART OF THE INSTALLATION OR DOWNLOAD PROCESS GOVERNING CUSTOMER'S USE OF THE SOFTWARE. TO THE EXTENT OF A CONFLICT BETWEEN THE PROVISIONS OF THE FOREGOING DOCUMENTS, THE ORDER OF PRECEDENCE SHALL BE (1)THE SIGNED CONTRACT, (2) THE CLICK-ACCEPT AGREEMENT OR THIRD PARTY LICENSE AGREEMENT, AND (3) THE AGREEMENT. FOR PURPOSES OF THE AGREEMENT, \"SOFTWARE\" SHALL MEAN COMPUTER PROGRAMS, INCLUDING FIRMWARE AND COMPUTER PROGRAMS EMBEDDED IN CISCO EQUIPMENT, AS PROVIDED TO CUSTOMER BY AN APPROVED SOURCE, AND ANY UPGRADES, UPDATES, BUG FIXES " +
                    "OR MODIFIED VERSIONS THERETO (COLLECTIVELY, \"UPGRADES\"), ANY OF THE SAME WHICH HAS BEEN RELICENSED UNDER THE CISCO SOFTWARE TRANSFER AND RE-LICENSING POLICY (AS MAY BE AMENDED BY CISCO FROM TIME TO TIME) OR BACKUP COPIES OF ANY OF THE FOREGOING.\n" +
            "\n" +
            "License. Conditioned upon compliance with the terms and conditions of the Agreement, Cisco grants to Customer a nonexclusive and nontransferable license to use for Customer's internal business purposes the Software and the Documentation for which Customer has paid the required license fees to an Approved Source. \"Documentation\" means written information (whether contained in user or technical manuals, training materials, specifications or otherwise) pertaining to the Software and made available by an Approved Source with the Software in any manner (including on CD-Rom, or on-line). In order to use the Software, Customer may be required to input a registration number or product authorization key and register Customer's copy of the Software online at Cisco's website to obtain the necessary license key or license file.\n" +
            "\n" +
            "Customer's license to use the Software shall be limited to, and Customer shall not use the Software in excess of, a single hardware chassis or card or such other limitations as are set forth in the applicable Supplemental License Agreement or in the applicable purchase order which has been accepted by an Approved Source and for which Customer has paid to an Approved Source the required license fee (the \"Purchase Order\").\n" +
            "\n" +
            "Unless otherwise expressly provided in the Documentation or any applicable Supplemental License Agreement, Customer shall use the Software solely as embedded in, for execution on, or (where the applicable Documentation permits installation on non-Cisco equipment) for communication with Cisco equipment owned or leased by Customer and used for Customer's internal business purposes. No other licenses are granted by implication, estoppel or otherwise.\n" +
            "\n" +
            "For evaluation or beta copies for which Cisco does not charge a license fee, the above requirement to pay license fees does not apply.\n" +
            "\n" +
            "General Limitations. This is a license, not a transfer of title, to the Software and Documentation, and Cisco retains ownership of all copies of the Software and Documentation. Customer acknowledges that the Software and Documentation contain trade secrets of Cisco or its suppliers or licensors, including but not limited to the specific internal design and structure of individual programs and associated interface information. Except as otherwise expressly provided under the Agreement, Customer shall only use the Software in connection with the use of Cisco equipment purchased by the Customer from an Approved Source and Customer shall have no right, and Customer specifically agrees not to:\n" +
            "\n" +
            "(i) transfer, assign or sublicense its license rights to any other person or entity (other than in compliance with any Cisco relicensing/transfer policy then in force), or use the Software on Cisco equipment not purchased by the Customer from an Approved Source or on secondhand Cisco equipment, and Customer acknowledges that any attempted transfer, assignment, sublicense or use shall be void;\n" +
            "\n" +
            "(ii) make error corrections to or otherwise modify or adapt the Software or create derivative works based upon the Software, or permit third parties to do the same;\n" +
            "\n" +
            "(iii) reverse engineer or decompile, decrypt, disassemble or otherwise reduce the Software to human-readable form, except to the extent otherwise expressly permitted under applicable law notwithstanding this restriction or except to the extent that Cisco is legally required to permit such specific activity pursuant to any applicable open source license;\n" +
            "\n" +
            "(iv) publish any results of benchmark tests run on the Software;\n" +
            "\n" +
            "(v) use or permit the Software to be used to perform services for third parties, whether on a service bureau or time sharing basis or otherwise, without the express written authorization of Cisco; or\n" +
            "\n" +
            "(vi) disclose, provide, or otherwise make available trade secrets contained within the Software and Documentation in any form to any third party without the prior written consent of Cisco. Customer shall implement reasonable security measures to protect such trade secrets.\n" +
            "\n" +
            "To the extent required by applicable law, and at Customer's written request, Cisco shall provide Customer with the interface information needed to achieve interoperability between the Software and another independently created program, on payment of Cisco's applicable fee, if any. Customer shall observe strict obligations of confidentiality with respect to such information and shall use such information in compliance with any applicable terms and conditions upon which Cisco makes such information available.\n" +
            "\n" +
            "Software, Upgrades and Additional Copies. NOTWITHSTANDING ANY OTHER PROVISION OF THE AGREEMENT: (1) CUSTOMER HAS NO LICENSE OR RIGHT TO MAKE OR USE ANY ADDITIONAL COPIES OR UPGRADES UNLESS CUSTOMER, AT THE TIME OF MAKING OR ACQUIRING SUCH COPY OR UPGRADE, ALREADY HOLDS A VALID LICENSE TO THE ORIGINAL SOFTWARE AND HAS PAID THE APPLICABLE FEE TO AN APPROVED SOURCE FOR THE UPGRADE OR ADDITIONAL COPIES; (2) USE OF UPGRADES IS LIMITED TO CISCO EQUIPMENT SUPPLIED BY AN APPROVED SOURCE FOR WHICH CUSTOMER IS THE ORIGINAL END USER PURCHASER OR LESSEE OR OTHERWISE HOLDS A VALID LICENSE TO USE THE SOFTWARE WHICH IS BEING UPGRADED; AND (3) THE MAKING AND USE OF ADDITIONAL COPIES IS LIMITED TO NECESSARY BACKUP PURPOSES ONLY.\n" +
            "\n" +
            "Proprietary Notices. Customer agrees to maintain and reproduce all copyright, proprietary, and other notices on all copies, in any form, of the Software in the same form and manner that such copyright and other proprietary notices are included on the Software. Except as expressly authorized in the Agreement, Customer shall not make any copies or duplicates of any Software without the prior written permission of Cisco.\n" +
            "\n" +
            "Term and Termination. The Agreement and the license granted herein shall remain effective until terminated. Customer may terminate the Agreement and the license at any time by destroying all copies of Software and any Documentation. Customer's rights under the Agreement will terminate immediately without notice from Cisco if Customer fails to comply with any provision of the Agreement. Upon termination, Customer shall destroy all copies of Software and Documentation in its possession or control. All confidentiality obligations of Customer, all restrictions and limitations imposed on the Customer under the section titled \"General Limitations\" and all limitations of liability and disclaimers and restrictions of warranty shall survive termination of this Agreement. In addition, the provisions of the sections titled \"U.S. Government End User Purchasers\" and \"General Terms Applicable to the Limited Warranty Statement " +
                    "and End User License Agreement\" shall survive termination of the Agreement.\n" +
            "\n" +
            "Customer Records. Customer grants to Cisco and its independent accountants the right to examine Customer's books, records and accounts during Customer's normal business hours to verify compliance with this Agreement. In the event such audit discloses non-compliance with this Agreement, Customer shall promptly pay to Cisco the appropriate license fees, plus the reasonable cost of conducting the audit.\n" +
            "\n" +
            "Export, Re-Export, Transfer and Use Controls. The Software, Documentation and technology or direct products thereof (hereafter referred to as Software and Technology), supplied by Cisco under the Agreement are subject to export controls under the laws and regulations of the United States (\"U.S.\") and any other applicable countries' laws and regulations. Customer shall comply with such laws and regulations governing export, re-export, import, transfer and use of Cisco Software and Technology and will obtain all required U.S. and local authorizations, permits, or licenses. Cisco and Customer each agree to provide the other information, support documents, and assistance as may reasonably be required by the other in connection with securing authorizations or licenses. Information regarding compliance with export, re-export, transfer and use may be located at the following URL: " +
                    "www.cisco.com/web/about/doing_business/legal/global_export_trade/general_export/contract_compliance.html\n" +
            "\n" +
            "U.S. Government End User Purchasers. The Software and Documentation qualify as \"commercial items,\" as that term is defined at Federal Acquisition Regulation (\"FAR\") (48 C.F.R.) 2.101, consisting of \"commercial computer software\" and \"commercial computer software documentation\" as such terms are used in FAR 12.212. Consistent with FAR 12.212 and DoD FAR Supp. 227.7202-1 through 227.7202-4, and notwithstanding any other FAR or other contractual clause to the contrary in any agreement into which the Agreement may be incorporated, Customer may provide to Government end user or, if the Agreement is direct, Government end user will acquire, the Software and Documentation with only those rights set forth in the Agreement. Use of either the Software or Documentation or both constitutes agreement by the Government that the Software and Documentation are \"commercial computer software\" and \"commercial computer " +
                    "software documentation,\" and constitutes acceptance of the rights and restrictions herein.\n" +
            "\n" +
            "Identified Components; Additional Terms. The Software may contain or be delivered with one or more components, which may include third-party components, identified by Cisco in the Documentation, readme.txt file, third-party click-accept or elsewhere (e.g. on www.cisco.com) (the \"Identified Component(s)\") as being subject to different license agreement terms, disclaimers of warranties, limited warranties or other terms and conditions (collectively, \"Additional Terms\") than those set forth herein. You agree to the applicable Additional Terms for any such Identified Component(s).\n" +
            "\n" +
            "Limited Warranty\n" +
            "\n" +
            "Subject to the limitations and conditions set forth herein, Cisco warrants that commencing from the date of shipment to Customer (but in case of resale by an Approved Source other than Cisco, commencing not more than ninety (90) days after original shipment by Cisco), and continuing for a period of the longer of (a) ninety (90) days or (b) the warranty period (if any) expressly set forth as applicable specifically to software in the warranty card accompanying the product of which the Software is a part (the \"Product\") (if any): (a) the media on which the Software is furnished will be free of defects in materials and workmanship under normal use; and (b) the Software substantially conforms to the Documentation. The date of shipment of a Product by Cisco is set forth on the packaging material in which the Product is shipped. Except for the foregoing, the Software is provided \"AS IS\". This limited warranty extends only to the " +
                    "Software purchased from an Approved Source by a Customer who is the first registered end user. Customer's sole and exclusive remedy and the entire liability of Cisco and its suppliers under this limited warranty will be (i) replacement of defective media and/or (ii) at Cisco's option, repair, replacement, or refund of the purchase price of the Software, in both cases subject to the condition that any error or defect constituting a breach of this limited warranty is reported to the Approved Source supplying the Software to Customer, within the warranty period. Cisco or the Approved Source supplying the Software to Customer may, at its option, require return of the Software and/or Documentation as a condition to the remedy. In no event does Cisco warrant that the Software is error free or that Customer will be able to operate the Software without problems or interruptions. In addition, due to the continual development of new " +
                    "techniques for intruding upon and attacking networks, Cisco does not warrant that the Software or any equipment, system or network on which the Software is used will be free of vulnerability to intrusion or attack.\n" +
            "\n" +
            "Restrictions. This warranty does not apply if the Software, Product or any other equipment upon which the Software is authorized to be used (a) has been altered, except by Cisco or its authorized representative, (b) has not been installed, operated, repaired, or maintained in accordance with instructions supplied by Cisco, (c) has been subjected to abnormal physical or electrical stress, abnormal environmental conditions, misuse, negligence, or accident; or (d) is licensed for beta, evaluation, testing or demonstration purposes. The Software warranty also does not apply to (e) any temporary Software modules; (f) any Software not posted on Cisco's Software Center; (g) any Software that Cisco expressly provides on an \"AS IS\" basis on Cisco's Software Center; (h) any Software for which an Approved Source does not receive a license fee; and (i) Software supplied by any third party which is not an Approved Source.\n" +
            "\n" +
            "DISCLAIMER OF WARRANTY\n" +
            "\n" +
            "EXCEPT AS SPECIFIED IN THIS WARRANTY SECTION, ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS, AND WARRANTIES INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTY OR CONDITION OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, SATISFACTORY QUALITY, NON-INTERFERENCE, ACCURACY OF INFORMATIONAL CONTENT, OR ARISING FROM A COURSE OF DEALING, LAW, USAGE, OR TRADE PRACTICE, ARE HEREBY EXCLUDED TO THE EXTENT ALLOWED BY APPLICABLE LAW AND ARE EXPRESSLY DISCLAIMED BY CISCO, ITS SUPPLIERS AND LICENSORS. TO THE EXTENT THAT ANY OF THE SAME CANNOT BE EXCLUDED, SUCH IMPLIED CONDITION, REPRESENTATION AND/OR WARRANTY IS LIMITED IN DURATION TO THE EXPRESS WARRANTY PERIOD REFERRED TO IN THE \"LIMITED WARRANTY\" SECTION ABOVE. BECAUSE SOME STATES OR JURISDICTIONS DO NOT ALLOW LIMITATIONS ON HOW LONG AN IMPLIED WARRANTY LASTS, THE ABOVE LIMITATION MAY NOT APPLY IN SUCH STATES. THIS WARRANTY GIVES CUSTOMER SPECIFIC LEGAL RIGHTS, " +
                    "AND CUSTOMER MAY ALSO HAVE OTHER RIGHTS WHICH VARY FROM JURISDICTION TO JURISDICTION. This disclaimer and exclusion shall apply even if the express warranty set forth above fails of its essential purpose.\n" +
            "\n" +
            "Disclaimer of Liabilities-Limitation of Liability. IF YOU ACQUIRED THE SOFTWARE IN THE UNITED STATES, LATIN AMERICA, CANADA, JAPAN OR THE CARIBBEAN, NOTWITHSTANDING ANYTHING ELSE IN THE AGREEMENT TO THE CONTRARY, ALL LIABILITY OF CISCO, ITS AFFILIATES, OFFICERS, DIRECTORS, EMPLOYEES, AGENTS, SUPPLIERS AND LICENSORS COLLECTIVELY, TO CUSTOMER, WHETHER IN CONTRACT, TORT (INCLUDING NEGLIGENCE), BREACH OF WARRANTY OR OTHERWISE, SHALL NOT EXCEED THE PRICE PAID BY CUSTOMER TO ANY APPROVED SOURCE FOR THE SOFTWARE THAT GAVE RISE TO THE CLAIM OR IF THE SOFTWARE IS PART OF ANOTHER PRODUCT, THE PRICE PAID FOR SUCH OTHER PRODUCT. THIS LIMITATION OF LIABILITY FOR SOFTWARE IS CUMULATIVE AND NOT PER INCIDENT (I.E. THE EXISTENCE OF TWO OR MORE CLAIMS WILL NOT ENLARGE THIS LIMIT).\n" +
            "\n" +
            "IF YOU ACQUIRED THE SOFTWARE IN EUROPE, THE MIDDLE EAST, AFRICA, ASIA OR OCEANIA, NOTWITHSTANDING ANYTHING ELSE IN THE AGREEMENT TO THE CONTRARY, ALL LIABILITY OF CISCO, ITS AFFILIATES, OFFICERS, DIRECTORS, EMPLOYEES, AGENTS, SUPPLIERS AND LICENSORS COLLECTIVELY, TO CUSTOMER, WHETHER IN CONTRACT, TORT (INCLUDING NEGLIGENCE), BREACH OF WARRANTY OR OTHERWISE, SHALL NOT EXCEED THE PRICE PAID BY CUSTOMER TO CISCO FOR THE SOFTWARE THAT GAVE RISE TO THE CLAIM OR IF THE SOFTWARE IS PART OF ANOTHER PRODUCT, THE PRICE PAID FOR SUCH OTHER PRODUCT. THIS LIMITATION OF LIABILITY FOR SOFTWARE IS CUMULATIVE AND NOT PER INCIDENT (I.E. THE EXISTENCE OF TWO OR MORE CLAIMS WILL NOT ENLARGE THIS LIMIT). NOTHING IN THE AGREEMENT SHALL LIMIT (I) THE LIABILITY OF CISCO, ITS AFFILIATES, OFFICERS, DIRECTORS, EMPLOYEES, AGENTS, SUPPLIERS AND LICENSORS TO CUSTOMER FOR PERSONAL INJURY OR DEATH CAUSED BY THEIR NEGLIGENCE, (II) CISCO'S LIABILITY FOR FRAUDULENT" +
            " MISREPRESENTATION, OR (III) ANY LIABILITY OF CISCO WHICH CANNOT BE EXCLUDED UNDER APPLICABLE LAW.\n" +
            "\n" +
            "Disclaimer of Liabilities-Waiver of Consequential Damages and Other Losses. IF YOU ACQUIRED THE SOFTWARE IN THE UNITED STATES, LATIN AMERICA, THE CARIBBEAN OR CANADA, REGARDLESS OF WHETHER ANY REMEDY SET FORTH HEREIN FAILS OF ITS ESSENTIAL PURPOSE OR OTHERWISE, IN NO EVENT WILL CISCO OR ITS SUPPLIERS BE LIABLE FOR ANY LOST REVENUE, PROFIT, OR LOST OR DAMAGED DATA, BUSINESS INTERRUPTION, LOSS OF CAPITAL, OR FOR SPECIAL, INDIRECT, CONSEQUENTIAL, INCIDENTAL, OR PUNITIVE DAMAGES HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY OR WHETHER ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE OR OTHERWISE AND EVEN IF CISCO OR ITS SUPPLIERS OR LICENSORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. BECAUSE SOME STATES OR JURISDICTIONS DO NOT ALLOW LIMITATION OR EXCLUSION OF CONSEQUENTIAL OR INCIDENTAL DAMAGES, THE ABOVE LIMITATION MAY NOT APPLY TO YOU.\n" +
            "\n" +
            "IF YOU ACQUIRED THE SOFTWARE IN JAPAN, EXCEPT FOR LIABILITY ARISING OUT OF OR IN CONNECTION WITH DEATH OR PERSONAL INJURY, FRAUDULENT MISREPRESENTATION, AND REGARDLESS OF WHETHER ANY REMEDY SET FORTH HEREIN FAILS OF ITS ESSENTIAL PURPOSE OR OTHERWISE, IN NO EVENT WILL CISCO, ITS AFFILIATES, OFFICERS, DIRECTORS, EMPLOYEES, AGENTS, SUPPLIERS AND LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT, OR LOST OR DAMAGED DATA, BUSINESS INTERRUPTION, LOSS OF CAPITAL, OR FOR SPECIAL, INDIRECT, CONSEQUENTIAL, INCIDENTAL, OR PUNITIVE DAMAGES HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY OR WHETHER ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE OR OTHERWISE AND EVEN IF CISCO OR ANY APPROVED SOURCE OR THEIR SUPPLIERS OR LICENSORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.\n" +
            "\n" +
            "IF YOU ACQUIRED THE SOFTWARE IN EUROPE, THE MIDDLE EAST, AFRICA, ASIA OR OCEANIA, IN NO EVENT WILL CISCO, ITS AFFILIATES, OFFICERS, DIRECTORS, EMPLOYEES, AGENTS, SUPPLIERS AND LICENSORS, BE LIABLE FOR ANY LOST REVENUE, LOST PROFIT, OR LOST OR DAMAGED DATA, BUSINESS INTERRUPTION, LOSS OF CAPITAL, OR FOR SPECIAL, INDIRECT, CONSEQUENTIAL, INCIDENTAL, OR PUNITIVE DAMAGES, HOWSOEVER ARISING, INCLUDING, WITHOUT LIMITATION, IN CONTRACT, TORT (INCLUDING NEGLIGENCE) OR WHETHER ARISING OUT OF THE USE OF OR INABILITY TO USE THE SOFTWARE, EVEN IF, IN EACH CASE, CISCO, ITS AFFILIATES, OFFICERS, DIRECTORS, EMPLOYEES, AGENTS, SUPPLIERS AND LICENSORS, HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. BECAUSE SOME STATES OR JURISDICTIONS DO NOT ALLOW LIMITATION OR EXCLUSION OF CONSEQUENTIAL OR INCIDENTAL DAMAGES, THE ABOVE LIMITATION MAY NOT FULLY APPLY TO YOU. THE FOREGOING EXCLUSION SHALL NOT APPLY TO ANY LIABILITY ARISING OUT OF OR IN " +
            "CONNECTION WITH: (I) DEATH OR PERSONAL INJURY, (II) FRAUDULENT MISREPRESENTATION, OR (III) CISCO'S LIABILITY IN CONNECTION WITH ANY TERMS THAT CANNOT BE EXCLUDED UNDER APPLICABLE LAW.\n" +
            "\n" +
            "Customer acknowledges and agrees that Cisco has set its prices and entered into the Agreement in reliance upon the disclaimers of warranty and the limitations of liability set forth herein, that the same reflect an allocation of risk between the parties (including the risk that a contract remedy may fail of its essential purpose and cause consequential loss), and that the same form an essential basis of the bargain between the parties.\n" +
            "\n" +
            "Controlling Law, Jurisdiction. If you acquired, by reference to the address on the purchase order accepted by the Approved Source, the Software in the United States, Latin America, or the Caribbean, the Agreement and warranties (\"Warranties\") are controlled by and construed under the laws of the State of California, United States of America, notwithstanding any conflicts of law provisions; and the state and federal courts of California shall have exclusive jurisdiction over any claim arising under the Agreement or Warranties. If you acquired the Software in Canada, unless expressly prohibited by local law, the Agreement and Warranties are controlled by and construed under the laws of the Province of Ontario, Canada, notwithstanding any conflicts of law provisions; and the courts of the Province of Ontario shall have exclusive jurisdiction over any claim arising under the Agreement or Warranties. If you acquired the Software in " +
            "Europe, the Middle East, Africa, Asia or Oceania (excluding Australia), unless expressly prohibited by local law, the Agreement and Warranties are controlled by and construed under the laws of England, notwithstanding any conflicts of law provisions; and the English courts shall have exclusive jurisdiction over any claim arising under the Agreement or Warranties. In addition, if the Agreement is controlled by the laws of England, no person who is not a party to the Agreement shall be entitled to enforce or take the benefit of any of its terms under the Contracts (Rights of Third Parties) Act 1999. If you acquired the Software in Japan, unless expressly prohibited by local law, the Agreement and Warranties are controlled by and construed under the laws of Japan, notwithstanding any conflicts of law provisions; and the Tokyo District Court of Japan shall have exclusive jurisdiction over any claim arising under the Agreement or Warranties. " +
            "If you acquired the Software in Australia, unless expressly prohibited by local law, the Agreement and Warranties are controlled by and construed under the laws of the State of New South Wales, Australia, notwithstanding any conflicts of law provisions; and the State and federal courts of New South Wales shall have exclusive jurisdiction over any claim arising under the Agreement or Warranties. If you acquired the Software in any other country, unless expressly prohibited by local law, the Agreement and Warranties are controlled by and construed under the laws of the State of California, United States of America, notwithstanding any conflicts of law provisions; and the state and federal courts of California shall have exclusive jurisdiction over any claim arising under the Agreement or Warranties.\n" +
            "\n" +
            "For all countries referred to above, the parties specifically disclaim the application of the UN Convention on Contracts for the International Sale of Goods. Notwithstanding the foregoing, either party may seek interim injunctive relief in any court of appropriate jurisdiction with respect to any alleged breach of such party's intellectual property or proprietary rights. If any portion hereof is found to be void or unenforceable, the remaining provisions of the Agreement and Warranties shall remain in full force and effect. Except as expressly provided herein, the Agreement constitutes the entire agreement between the parties with respect to the license of the Software and Documentation and supersedes any conflicting or additional terms contained in any Purchase Order or elsewhere, all of which terms are excluded. The Agreement has been written in the English language, and the parties agree that the English version will govern.\n" +
            "\n" +
            "Product warranty terms and other information applicable to Cisco products are available at the following URL: www.cisco.com/go/warranty\n" +
            "\n" +
            "Cisco and the Cisco logo are trademarks or registered trademarks of Cisco and/or its affiliates in the U.S. and other countries. To view a list of Cisco trademarks, go to this URL: www.cisco.com/go/trademarks. Third-party trademarks mentioned are the property of their respective owners. The use of the word partner does not imply a partnership relationship between Cisco and any other company. (1110R)\n" +
            "\n" +
            "© 1998, 2001, 2003, 2008-2014 Cisco Systems, Inc. All rights reserved.</License>\n" +
            "</EulaSection>\n" +
            "<EulaSection>\n" +
            "      <Info>supplemental end-user license agreement</Info>\n" +
            "      <License>SUPPLEMENTAL END USER LICENSE AGREEMENT FOR VIRTUAL SOFTWARE PRODUCTS\n" +
            "\n" +
            "IMPORTANT: READ CAREFULLY\n" +
            "\n" +
            "This Supplemental End User License Agreement (\"SEULA\") contains additional terms and conditions for the Software licensed under the End User License Agreement (\"EULA\") between you and Cisco (collectively, the \"Agreement\"). Capitalized terms used in this SEULA but not defined will have the meanings assigned to them in the EULA. To the extent that there is a conflict between the terms and conditions of the EULA and this SEULA, the terms and conditions of this SEULA will take precedence. In addition to the limitations set forth in the EULA on your access and use of the Software, you agree to comply at all times with the terms and conditions provided in this SEULA.\n" +
            "\n" +
            "DOWNLOADING, INSTALLING, OR USING THE SOFTWARE CONSTITUTES ACCEPTANCE OF THE AGREEMENT, AND YOU ARE BINDING YOURSELF AND THE BUSINESS ENTITY THAT YOU REPRESENT (COLLECTIVELY, \"CUSTOMER\") TO THE AGREEMENT. IF YOU DO NOT AGREE TO ALL OF THE TERMS OF THE AGREEMENT, THEN CISCO IS UNWILLING TO LICENSE THE SOFTWARE TO YOU AND (A) YOU MAY NOT DOWNLOAD, INSTALL OR USE THE SOFTWARE, AND (B) YOU MAY RETURN THE SOFTWARE (INCLUDING ANY UNOPENED CD PACKAGE AND ANY WRITTEN MATERIALS) FOR A FULL REFUND, OR, IF THE SOFTWARE AND WRITTEN MATERIALS ARE SUPPLIED AS PART OF ANOTHER PRODUCT, YOU MAY RETURN THE ENTIRE PRODUCT FOR A FULL REFUND. YOUR RIGHT TO RETURN AND REFUND EXPIRES 30 DAYS AFTER PURCHASE FROM CISCO OR AN AUTHORIZED CISCO RESELLER, AND APPLIES ONLY IF YOU ARE THE ORIGINAL END USER PURCHASER.\n" +
            "\n" +
            "Definitions\n" +
            "\"CPU\" means a central processing unit that encompasses part of a Server.\n" +
            "\"Failover Pair\"  means a primary Instance and a standby Instance with the same Software configuration where the standby Instance can take over in case of failure of the primary Instance.\n" +
            "\"Instance\" means a single copy of the Software. Each copy of the Software loaded into memory is an Instance.\n" +
            "\"Server\" means a single physical computer or device on a network that manages or provides network resources for multiple users.\n" +
            "\"Service Provider\" means a company that provides information technology services to  external end user customers.\n" +
            "\"Software\" means Cisco's Adaptive Security Virtual Appliance (\"ASAv\"), Adaptive Security Appliance 1000V Cloud Firewall Software (\"ASA 1000V\"), Nexus 1000V series switch products, Virtual Security Gateway products, or other Cisco virtual software products that Cisco includes under this SEULA.\n" +
            "\"vCPU\" means a virtual central processing resource assigned to the VM by the underlying virtualization technology.\n" +
            "\"Virtual Machine\" or \"VM\" means a software container that can run its own operating system and execute applications like a Server.\n" +
            "\n" +
            "Additional License Terms and Conditions\n" +
            "1. Cisco hereby grants Customer the right to install and use the Software on single or multiple Cisco or non-Cisco Servers or on Virtual Machines. In order to use the Software Customer may be required to input a registration number or product activation key and register each Instance online at Cisco's website in order to obtain the necessary entitlements.\n" +
            "2. Customer shall pay a unit license fee to Cisco or an authorized Cisco reseller, as applicable, for each Instance installed on a Cisco or non-Cisco Server CPU, vCPU or Virtual Machine, as determined by Cisco.\n" +
            "3. For the ASA 1000V, Customer is licensed the number of Instances equal to the number of CPUs covered by the unit license fee. If Customer deploys a Failover Pair, then the fee for the additional standby Instance is included in the fee for each primary Instance.\n" +
            "4. If Customer is a Service Provider, Customer may use the Software under the terms of this Agreement for the purpose of delivering hosted information technology services to Customer's end user customers, subject to payment of the required license fee(s).\n" +
            "5. Customer may also use the Software under the terms of this Agreement to deliver hosted information technology services to Customer affiliates, subject to payment of the required license fee(s).\n" +
            "6. If the Software is subject to Cisco's Smart Licensing program, Cisco will be able to assess if Customer is using the Software within the limits and entitlements paid for by Customer. If the Smart Licensing program is applicable, Customer will be required to enter into a separate terms of service agreement relating to Smart Licensing.</License>\n" +
            "</EulaSection>\n" +
            "</VirtualSystem>";

    private OVFHelper ovfHelper = new OVFHelper();

    @Test
    public void testGetOVFPropertiesValidOVF() throws IOException, SAXException, ParserConfigurationException {
        List<OVFPropertyTO> props = ovfHelper.getOVFPropertiesFromXmlString(ovfFileProductSection);
        Assert.assertEquals(2, props.size());
    }

    @Test(expected = SAXParseException.class)
    public void testGetOVFPropertiesInvalidOVF() throws IOException, SAXException, ParserConfigurationException {
        ovfHelper.getOVFPropertiesFromXmlString(ovfFileProductSection + "xxxxxxxxxxxxxxxxx");
    }

    @Test
    public void testGetOVFDeploymentOptionsValidOVF() throws IOException, SAXException, ParserConfigurationException {
        List<OVFConfigurationTO> options = ovfHelper.getOVFDeploymentOptionsFromXmlString(ovfFileDeploymentOptionsSection);
        Assert.assertEquals(3, options.size());
    }

    @Test
    public void testGetOVFVirtualHardwareSectionValidOVF() throws IOException, SAXException, ParserConfigurationException {
        List<OVFVirtualHardwareItemTO> items = ovfHelper.getOVFVirtualHardwareSectionFromXmlString(ovfFileVirtualHardwareSection);
        Assert.assertEquals(20, items.size());
    }

    @Test
    public void testGetOVFEulaSectionValidOVF() throws IOException, SAXException, ParserConfigurationException {
        List<OVFEulaSectionTO> eulas = ovfHelper.getOVFEulaSectionFromXmlString(eulaSections);
        Assert.assertEquals(2, eulas.size());
    }
}
