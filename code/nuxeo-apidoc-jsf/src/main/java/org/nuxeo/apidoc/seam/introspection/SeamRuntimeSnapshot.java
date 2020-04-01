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
package org.nuxeo.apidoc.seam.introspection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.introspection.ServerInfo;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.snapshot.SeamDistributionSnapshot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SeamRuntimeSnapshot extends RuntimeSnapshot implements SeamDistributionSnapshot {

    protected boolean seamInitialized = false;

    protected List<SeamComponentInfo> seamComponents = new ArrayList<>();

    public SeamRuntimeSnapshot() {
        super();
    }

    public static SeamRuntimeSnapshot build() {
        return new SeamRuntimeSnapshot();
    }

    @JsonCreator
    private SeamRuntimeSnapshot(@JsonProperty("serverInfo") ServerInfo serverInfo,
            @JsonProperty("creationDate") Date created,
            @JsonProperty("seamComponents") List<SeamComponentInfo> seamComponents,
            @JsonProperty("operations") List<OperationInfo> operations) {
        this.serverInfo = serverInfo;
        this.created = created;
        index();
        this.seamComponents.addAll(seamComponents);
        this.operations.addAll(operations);
    }

    @SuppressWarnings("unchecked")
    public void initSeamComponents(HttpServletRequest request) {
        if (seamInitialized) {
            return;
        }
        // use reflection to call SeamRuntimeIntrospector, if available
        try {
            // SeamRuntimeIntrospector.listNuxeoComponents(request);
            Class<?> klass = Class.forName("org.nuxeo.apidoc.seam.SeamRuntimeIntrospector");
            Method method = klass.getDeclaredMethod("listNuxeoComponents", HttpServletRequest.class);
            seamComponents = (List<SeamComponentInfo>) method.invoke(null, request);
        } catch (ReflectiveOperationException e) {
            // ignore, no Seam
        }
        for (SeamComponentInfo seamComp : seamComponents) {
            ((SeamComponentInfoImpl) seamComp).setVersion(getVersion());
        }
        seamInitialized = true;
    }

    @Override
    public SeamComponentInfo getSeamComponent(String id) {
        for (SeamComponentInfo sci : getSeamComponents()) {
            if (sci.getId().equals(id)) {
                return sci;
            }
        }
        return null;
    }

    @Override
    @JsonIgnore
    public List<String> getSeamComponentIds() {
        List<String> ids = new ArrayList<>();
        for (SeamComponentInfo sci : getSeamComponents()) {
            ids.add(sci.getId());
        }
        return ids;
    }

    @Override
    public List<SeamComponentInfo> getSeamComponents() {
        return seamComponents;
    }

    @Override
    public boolean containsSeamComponents() {
        return getSeamComponentIds().size() > 0;
    }

}
