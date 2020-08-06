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

package com.cloud.storage.template;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.cloud.agent.api.storage.OVFConfigurationTO;
import com.cloud.agent.api.storage.OVFPropertyTO;
import com.cloud.agent.api.storage.OVFVirtualHardwareItemTO;
import com.cloud.agent.api.storage.OVFVirtualHardwareSectionTO;
import org.apache.cloudstack.api.net.NetworkPrerequisiteTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.cloud.agent.api.storage.OVFHelper;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.script.Script;
import org.xml.sax.SAXException;

/**
 * processes the content of an OVA for registration of a template
 */
public class OVAProcessor extends AdapterBase implements Processor {
    private static final Logger LOGGER = Logger.getLogger(OVAProcessor.class);
    StorageLayer _storage;

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException {
        return process(templatePath, format, templateName, 0);
    }

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName, long processTimeout) throws InternalErrorException {
        if (! conversionChecks(format)){
            return null;
        }

        LOGGER.info("Template processing. templatePath: " + templatePath + ", templateName: " + templateName);
        String templateFilePath = templatePath + File.separator + templateName + "." + ImageFormat.OVA.getFileExtension();
        if (!_storage.exists(templateFilePath)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Unable to find the vmware template file: " + templateFilePath);
            }
            return null;
        }

        String templateFileFullPath = unpackOva(templatePath, templateName, processTimeout);

        setFileSystemAccessRights(templatePath);

        FormatInfo info = createFormatInfo(templatePath, templateName, templateFilePath, templateFileFullPath);

        // The intention is to use the ova file as is for deployment and use processing result only for
        // - property assessment and
        // - reconsiliation of
        // - - disks,
        // - - networks and
        // - - compute dimensions.
        return info;
    }

    private FormatInfo createFormatInfo(String templatePath, String templateName, String templateFilePath, String templateFileFullPath) throws InternalErrorException {
        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.OVA;
        info.filename = templateName + "." + ImageFormat.OVA.getFileExtension();
        info.size = _storage.getSize(templateFilePath);
        info.virtualSize = getTemplateVirtualSize(templatePath, info.filename);
        validateOva(templateFileFullPath, info);

        return info;
    }

    /**
     * side effect; properties are added to the info
     *
     * @throws InternalErrorException on an invalid ova contents
     */
    private void validateOva(String templateFileFullPath, FormatInfo info) throws InternalErrorException {
        String ovfFilePath = getOVFFilePath(templateFileFullPath);
        OVFHelper ovfHelper = new OVFHelper();
        Document doc = ovfHelper.getDocumentFromFile(ovfFilePath);

        List<DatadiskTO> disks = ovfHelper.getOVFVolumeInfoFromFile(ovfFilePath, doc);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Found %d disks in template %s", CollectionUtils.isNotEmpty(disks) ? disks.size() : 0, ovfFilePath));
        }
        if (CollectionUtils.isNotEmpty(disks)) {
            info.disks = disks;
        }

        List<NetworkPrerequisiteTO> nets = ovfHelper.getNetPrerequisitesFromDocument(doc);
        if (CollectionUtils.isNotEmpty(nets)) {
            LOGGER.info("Found " + nets.size() + " prerequisite networks");
            info.networks = nets;
        } else if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("no net prerequisites found in template %s", ovfFilePath));
        }

        List<OVFPropertyTO> ovfProperties = ovfHelper.getConfigurableOVFPropertiesFromDocument(doc);
        if (CollectionUtils.isNotEmpty(ovfProperties)) {
            LOGGER.info("Found " + ovfProperties.size() + " configurable OVF properties");
            info.ovfProperties = ovfProperties;
        } else if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("no ovf properties found in template %s", ovfFilePath));
        }

        OVFVirtualHardwareSectionTO hardwareSection = ovfHelper.getVirtualHardwareSectionFromDocument(doc);
        List<OVFConfigurationTO> configurations = hardwareSection.getConfigurations();
        if (CollectionUtils.isNotEmpty(configurations)) {
            LOGGER.info("Found " + configurations.size() + " deployment option configurations");
        }
        List<OVFVirtualHardwareItemTO> hardwareItems = hardwareSection.getCommonHardwareItems();
        if (CollectionUtils.isNotEmpty(hardwareItems)) {
            LOGGER.info("Found " + hardwareItems.size() + " virtual hardware items");
        }
        info.hardwareSection = hardwareSection;

        // FR37 TODO add any user queries that are required for this OVA
    }

    private void setFileSystemAccessRights(String templatePath) {
        Script command;
        String result;

        command = new Script("chmod", 0, LOGGER);
        command.add("-R");
        command.add("666", templatePath);
        result = command.execute();
        if (result != null) {
            LOGGER.warn("Unable to set permissions for files in " + templatePath + " due to " + result);
        }
        command = new Script("chmod", 0, LOGGER);
        command.add("777", templatePath);
        result = command.execute();
        if (result != null) {
            LOGGER.warn("Unable to set permissions for " + templatePath + " due to " + result);
        }
    }

    private String unpackOva(String templatePath, String templateName, long processTimeout) throws InternalErrorException {
        LOGGER.info("Template processing - untar OVA package. templatePath: " + templatePath + ", templateName: " + templateName);
        String templateFileFullPath = templatePath + File.separator + templateName + "." + ImageFormat.OVA.getFileExtension();
        File templateFile = new File(templateFileFullPath);
        Script command = new Script("tar", processTimeout, LOGGER);
        command.add("--no-same-owner");
        command.add("--no-same-permissions");
        command.add("-xf", templateFileFullPath);
        command.setWorkDir(templateFile.getParent());
        String result = command.execute();
        if (result != null) {
            LOGGER.info("failed to untar OVA package due to " + result + ". templatePath: " + templatePath + ", templateName: " + templateName);
            throw new InternalErrorException("failed to untar OVA package");
        }
        return templateFileFullPath;
    }

    private boolean conversionChecks(ImageFormat format) {
        if (format != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("We currently don't handle conversion from " + format + " to OVA.");
            }
            return false;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("We are handling format " + format + ".");
        }
        return true;
    }

    @Override
    public long getVirtualSize(File file) {
        try {
            long size = getTemplateVirtualSize(file.getParent(), file.getName());
            return size;
        } catch (Exception e) {
            LOGGER.info("[ignored]"
                    + "failed to get virtual template size for ova: " + e.getLocalizedMessage());
        }
        return file.length();
    }

    /**
     * gets the virtual size from the OVF file meta data.
     *
     * @return the accumulative virtual size of the disk definitions in the OVF
     * @throws InternalErrorException
     */
    public long getTemplateVirtualSize(String templatePath, String templateName) throws InternalErrorException {
        long virtualSize = 0;
        String templateFileFullPath = templatePath.endsWith(File.separator) ? templatePath : templatePath + File.separator;
        templateFileFullPath += templateName.endsWith(ImageFormat.OVA.getFileExtension()) ? templateName : templateName + "." + ImageFormat.OVA.getFileExtension();
        String ovfFileName = getOVFFilePath(templateFileFullPath);
        if (ovfFileName == null) {
            String msg = "Unable to locate OVF file in template package directory: " + templatePath;
            LOGGER.error(msg);
            throw new InternalErrorException(msg);
        }
        try {
            Document ovfDoc = null;
            ovfDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(ovfFileName));
            NodeList diskElements = ovfDoc.getElementsByTagName("Disk");
            for (int i = 0; i < diskElements.getLength(); i++) {
                Element disk = (Element)diskElements.item(i);
                long diskSize = Long.parseLong(disk.getAttribute("ovf:capacity"));
                String allocationUnits = disk.getAttribute("ovf:capacityAllocationUnits");
                diskSize = OVFHelper.getDiskVirtualSize(diskSize, allocationUnits, ovfFileName);
                virtualSize += diskSize;
            }
            return virtualSize;
        } catch (InternalErrorException | IOException | NumberFormatException | ParserConfigurationException | SAXException e) {
            String msg = "getTemplateVirtualSize: Unable to parse OVF XML document " + templatePath + " to get the virtual disk " + templateName + " size due to " + e;
            LOGGER.error(msg);
            throw new InternalErrorException(msg);
        }
    }

    public Pair<Long, Long> getDiskDetails(String ovfFilePath, String diskName) throws InternalErrorException {
        long virtualSize = 0;
        long fileSize = 0;
        String fileId = null;
        try {
            Document ovfDoc = null;
            ovfDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(ovfFilePath));
            NodeList disks = ovfDoc.getElementsByTagName("Disk");
            NodeList files = ovfDoc.getElementsByTagName("File");
            for (int j = 0; j < files.getLength(); j++) {
                Element file = (Element)files.item(j);
                if (file.getAttribute("ovf:href").equals(diskName)) {
                    fileSize = Long.parseLong(file.getAttribute("ovf:size"));
                    fileId = file.getAttribute("ovf:id");
                    break;
                }
            }
            for (int i = 0; i < disks.getLength(); i++) {
                Element disk = (Element)disks.item(i);
                if (disk.getAttribute("ovf:fileRef").equals(fileId)) {
                    virtualSize = Long.parseLong(disk.getAttribute("ovf:capacity"));
                    String allocationUnits = disk.getAttribute("ovf:capacityAllocationUnits");
                    virtualSize = OVFHelper.getDiskVirtualSize(virtualSize, allocationUnits, ovfFilePath);
                    break;
                }
            }
            return new Pair<Long, Long>(virtualSize, fileSize);
        } catch (InternalErrorException | IOException | NumberFormatException | ParserConfigurationException | SAXException e) {
            String msg = "getDiskDetails: Unable to parse OVF XML document " + ovfFilePath + " to get the virtual disk " + diskName + " size due to " + e;
            LOGGER.error(msg);
            throw new InternalErrorException(msg);
        }
    }

    private String getOVFFilePath(String srcOVAFileName) {
        File file = new File(srcOVAFileName);
        assert (_storage != null);
        String[] files = _storage.listFiles(file.getParent());
        if (files != null) {
            for (String fileName : files) {
                if (fileName.toLowerCase().endsWith(".ovf")) {
                    File ovfFile = new File(fileName);
                    return file.getParent() + File.separator + ovfFile.getName();
                }
            }
        }
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            throw new ConfigurationException("Unable to get storage implementation");
        }

        return true;
    }
}