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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.plugin.AbstractPluginSnapshot;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.plugin.SeamPlugin;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SeamRuntimeSnapshot extends AbstractPluginSnapshot<SeamComponentInfo>
        implements PluginSnapshot<SeamComponentInfo> {

    protected boolean seamInitialized = false;

    protected List<SeamComponentInfo> seamComponents = new ArrayList<>();

    public SeamRuntimeSnapshot(String pluginId) {
        super(pluginId);
    }

    @JsonCreator
    private SeamRuntimeSnapshot(@JsonProperty("items") List<SeamComponentInfo> seamComponents) {
        this(SeamPlugin.ID);
        this.seamComponents.addAll(seamComponents);
    }

    @Override
    public String getPluginId() {
        return SeamPlugin.ID;
    }

    @SuppressWarnings("unchecked")
    public void initSeamComponents(DistributionSnapshot snapshot, HttpServletRequest request) {
        if (seamInitialized) {
            return;
        }
        // use reflection to call SeamRuntimeIntrospector, if available
        try {
            Class<?> klass = Class.forName("org.nuxeo.apidoc.seam.SeamRuntimeIntrospector");
            Method method = klass.getDeclaredMethod("listNuxeoComponents", HttpServletRequest.class);
            initSeamComponents(snapshot, (List<SeamComponentInfo>) method.invoke(null, request));
        } catch (ReflectiveOperationException e) {
            // ignore, no Seam
            seamInitialized = true;
        }
    }

    public void initSeamComponents(DistributionSnapshot snapshot, List<SeamComponentInfo> components) {
        if (seamInitialized) {
            return;
        }
        seamComponents = components;
        for (SeamComponentInfo seamComp : seamComponents) {
            ((SeamComponentInfoImpl) seamComp).setVersion(snapshot.getVersion());
        }
        seamInitialized = true;
    }

    @Override
    public List<String> getItemIds() {
        return getItems().stream().map(SeamComponentInfo::getId).collect(Collectors.toList());
    }

    @Override
    public List<SeamComponentInfo> getItems() {
        return Collections.unmodifiableList(seamComponents);
    }

    @Override
    public SeamComponentInfo getItem(String id) {
        return getItems().stream().filter(sci -> sci.getId().equals(id)).findFirst().get();
    }

}
