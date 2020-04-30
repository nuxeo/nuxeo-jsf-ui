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
package org.nuxeo.apidoc.seam.browse;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.browse.Distribution;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.plugin.SeamPlugin;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "seam")
public class SeamApiBrowser extends DefaultObject {

    protected String distributionId;

    protected boolean embeddedMode = false;

    @Override
    protected void initialize(Object... args) {
        distributionId = (String) args[0];
        if (args.length > 1) {
            Boolean embed = (Boolean) args[1];
            embeddedMode = embed != null && embed;
        }
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getService(SnapshotManager.class);
    }

    @GET
    @Produces("text/html")
    @Path(SeamPlugin.LIST_VIEW)
    public Object listSeamComponents() {
        return dolistSeamComponents(SeamPlugin.LIST_VIEW, false);
    }

    @GET
    @Produces("text/html")
    @Path(SeamPlugin.LIST_VIEW_SIMPLE)
    public Object listSeamComponentsSimple() {
        return dolistSeamComponents(SeamPlugin.LIST_VIEW_SIMPLE, true);
    }

    protected Object dolistSeamComponents(String view, boolean hideNav) {
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        @SuppressWarnings("unchecked")
        PluginSnapshot<SeamComponentInfo> seamSnapshot = (PluginSnapshot<SeamComponentInfo>) snapshot.getPluginSnapshots()
                                                                                                     .get(SeamPlugin.ID);
        List<SeamComponentInfo> seamComponents = seamSnapshot.getItems();
        return getView(view).arg("seamComponents", seamComponents)
                            .arg(Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID))
                            .arg("hideNav", Boolean.valueOf(hideNav));
    }

    @Path(SeamPlugin.ITEM_VIEW + "/{componentId}")
    public Resource viewSeamComponent(@PathParam("componentId") String componentId) {
        return ctx.newObject(SeamPlugin.ITEM_VIEW_TYPE, componentId);
    }

}
