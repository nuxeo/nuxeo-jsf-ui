/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.seam.plugin;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.plugin.AbstractPlugin;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginDescriptor;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.seam.adapters.SeamComponentInfoDocAdapter;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.introspection.SeamComponentInfoImpl;
import org.nuxeo.apidoc.seam.introspection.SeamRuntimeSnapshot;
import org.nuxeo.apidoc.seam.repository.SeamRepositorySnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Plugin handling retrieval, display and persistence of Seam components.
 *
 * @since 11.1
 */
public class SeamPlugin extends AbstractPlugin<SeamComponentInfo> implements Plugin<SeamComponentInfo> {

    public static final String ID = "seam";

    public static final String Seam_Root_NAME = "Seam";

    public static final String VIEW_TYPE = "seam";

    public static final String LIST_VIEW = "listSeamComponents";

    public static final String LIST_VIEW_SIMPLE = "listSeamComponentsSimple";

    public static final String ITEM_VIEW = "viewSeamComponent";

    public static final String ITEM_VIEW_TYPE = "seamComponent";

    public static final String PROPERTY_HIDE = "org.nuxeo.apidoc.hide.seam.components";

    protected boolean seamInitialized = false;

    public SeamPlugin(PluginDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public ObjectMapper enrishJsonMapper(ObjectMapper parent) {
        parent.registerModule(
                new SimpleModule().addAbstractTypeMapping(SeamComponentInfo.class, SeamComponentInfoImpl.class));
        return parent;
    }

    @Override
    public void persist(DistributionSnapshot snapshot, CoreSession session, DocumentModel root, SnapshotFilter filter) {
        DocumentModel seamContainer = getOrCreateSubRoot(session, root, Seam_Root_NAME);
        @SuppressWarnings("unchecked")
        PluginSnapshot<SeamComponentInfo> seamSnapshot = (PluginSnapshot<SeamComponentInfo>) snapshot.getPluginSnapshots()
                                                                                                     .get(getId());
        persistSeamComponents(seamSnapshot.getItems(), session, seamContainer, filter);
    }

    protected void persistSeamComponents(List<SeamComponentInfo> seamComponents, CoreSession session,
            DocumentModel parent, SnapshotFilter filter) {
        for (SeamComponentInfo seamComponent : seamComponents) {
            if (filter == null || includeSeamComponent(filter, seamComponent)) {
                persistSeamComponent(seamComponent, session, parent);
            }
        }
    }

    protected boolean includeSeamComponent(SnapshotFilter filter, SeamComponentInfo seamComponent) {
        for (String iface : seamComponent.getInterfaceNames()) {
            for (String pprefix : filter.getPackagesPrefixes()) {
                if (iface.startsWith(pprefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void persistSeamComponent(SeamComponentInfo seamComponent, CoreSession session, DocumentModel parent) {
        SeamComponentInfoDocAdapter.create(seamComponent, session, parent.getPathAsString());
    }

    @Override
    public void initWebContext(DistributionSnapshot snapshot, HttpServletRequest request) {
        // run init on the plugin snapshot held by given distribution snapshot
        ((SeamRuntimeSnapshot) snapshot.getPluginSnapshots().get(ID)).initSeamComponents(snapshot, request);
    }

    @Override
    public PluginSnapshot<SeamComponentInfo> getRuntimeSnapshot(DistributionSnapshot snapshot) {
        return new SeamRuntimeSnapshot(getId());
    }

    @Override
    public PluginSnapshot<SeamComponentInfo> getRepositorySnapshot(DocumentModel root) {
        return new SeamRepositorySnapshot(getId(), root);
    }

    @Override
    public String getView(String url) {
        String navPoint = null;
        if (ApiBrowserConstants.check(url, SeamPlugin.LIST_VIEW)
                || ApiBrowserConstants.check(url, SeamPlugin.ITEM_VIEW)) {
            navPoint = SeamPlugin.LIST_VIEW;
        }
        return navPoint;
    }

    @Override
    public boolean isHidden() {
        return Framework.isBooleanPropertyTrue(PROPERTY_HIDE)
                || Framework.isBooleanPropertyTrue(ApiBrowserConstants.PROPERTY_SITE_MODE);
    }

}
