/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.driver;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.image.BaseImageStoreDriverImpl;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.dao.VMTemplateDao;

//http-read-only based image store
public class SampleImageStoreDriverImpl extends BaseImageStoreDriverImpl {
    @Inject
    EndPointSelector selector;
    @Inject
    VMTemplateDao imageDataDao;

    public SampleImageStoreDriverImpl() {
    }


    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public String createEntityExtractUrl(DataStore store, String installPath, ImageFormat format) {
        // TODO Auto-generated method stub
        return null;
    }


}
