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
package com.cloud.api.query.dao;

import java.util.EnumSet;
import java.util.List;

import com.cloud.agent.api.storage.OVFPropertyTO;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.TemplateOVFPropertyResponse;
import org.apache.cloudstack.api.response.TemplateResponse;

import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchCriteria;

public interface TemplateJoinDao extends GenericDao<TemplateJoinVO, Long> {

    TemplateResponse newTemplateResponse(EnumSet<ApiConstants.DomainDetails> detailsView, ResponseView view, TemplateJoinVO tmpl);

    TemplateResponse newIsoResponse(TemplateJoinVO tmpl);

    TemplateResponse newUpdateResponse(TemplateJoinVO tmpl);

    TemplateResponse setTemplateResponse(EnumSet<ApiConstants.DomainDetails> detailsView, ResponseView view, TemplateResponse tmplData, TemplateJoinVO tmpl);

    List<TemplateJoinVO> newTemplateView(VirtualMachineTemplate tmpl);

    List<TemplateJoinVO> newTemplateView(VirtualMachineTemplate tmpl, long zoneId, boolean readyOnly);

    List<TemplateJoinVO> searchByTemplateZonePair( Boolean showRemoved, String... pairs);

    List<TemplateJoinVO> listActiveTemplates(long storeId);

    Pair<List<TemplateJoinVO>, Integer> searchIncludingRemovedAndCount(final SearchCriteria<TemplateJoinVO> sc, final Filter filter);

    List<TemplateJoinVO> findByDistinctIds(Long... ids);

    default TemplateOVFPropertyResponse createTemplateOVFPropertyResponse(OVFPropertyTO ovfPropertyTO) {
        TemplateOVFPropertyResponse propertyResponse = new TemplateOVFPropertyResponse();

        propertyResponse.setKey(ovfPropertyTO.getKey());
        propertyResponse.setType(ovfPropertyTO.getType());
        propertyResponse.setValue(ovfPropertyTO.getValue());
        propertyResponse.setQualifiers(ovfPropertyTO.getQualifiers());
        propertyResponse.setUserConfigurable(ovfPropertyTO.isUserConfigurable());
        propertyResponse.setLabel(ovfPropertyTO.getLabel());
        propertyResponse.setDescription(ovfPropertyTO.getDescription());
        propertyResponse.setPassword(ovfPropertyTO.isPassword());
        propertyResponse.setCategory(ovfPropertyTO.getCategory());
        propertyResponse.setIndex(ovfPropertyTO.getIndex());
        propertyResponse.setObjectName("ovfproperty");
        return propertyResponse;
    }
}
