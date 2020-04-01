/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.seam.snapshot;

import java.util.List;

import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.introspection.SeamComponentInfoImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface SeamDistributionSnapshot extends DistributionSnapshot {

    @JsonIgnore
    List<String> getSeamComponentIds();

    List<SeamComponentInfo> getSeamComponents();

    SeamComponentInfo getSeamComponent(String id);

    boolean containsSeamComponents();

    static ObjectMapper jsonMapper() {
        // FIXME
        final ObjectMapper mapper = new ObjectMapper().registerModule(
                new SimpleModule().addAbstractTypeMapping(SeamDistributionSnapshot.class, RuntimeSnapshot.class)
                .addAbstractTypeMapping(BundleInfo.class, BundleInfoImpl.class)
                .addAbstractTypeMapping(BundleGroup.class, BundleGroupImpl.class)
                .addAbstractTypeMapping(ComponentInfo.class, ComponentInfoImpl.class)
                .addAbstractTypeMapping(ExtensionInfo.class, ExtensionInfoImpl.class)
                .addAbstractTypeMapping(OperationInfo.class, OperationInfoImpl.class)
                .addAbstractTypeMapping(SeamComponentInfo.class, SeamComponentInfoImpl.class) // FIXME
                .addAbstractTypeMapping(ServiceInfo.class, ServiceInfoImpl.class)
                .addAbstractTypeMapping(DocumentationItem.class, ResourceDocumentationItem.class));
        mapper.addMixIn(OperationDocumentation.Param.class, OperationDocParamMixin.class);
        return mapper;
    }

}
