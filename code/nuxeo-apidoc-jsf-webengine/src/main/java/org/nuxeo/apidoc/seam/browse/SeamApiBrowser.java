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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

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

    @GET
    @Produces("text/html")
    @Path("listSeamComponents")
    public Object listSeamComponents() {
        return dolistSeamComponents("listSeamComponents", false);
    }

    @GET
    @Produces("text/html")
    @Path("listSeamComponentsSimple")
    public Object listSeamComponentsSimple() {
        return dolistSeamComponents("listSeamComponentsSimple", true);
    }

    protected Object dolistSeamComponents(String view, boolean hideNav) {
        // FIXME
        getSnapshotManager().initSeamContext(getContext().getRequest());

        SeamDistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<SeamComponentInfo> seamComponents = snap.getSeamComponents();
        return getView(view).arg("seamComponents", seamComponents)
                .arg(SeamDistribution.DIST_ID, ctx.getProperty(SeamDistribution.DIST_ID))
                .arg("hideNav", Boolean.valueOf(hideNav));
    }

    @Path("viewSeamComponent/{componentId}")
    public Resource viewSeamComponent(@PathParam("componentId") String componentId) {
        return ctx.newObject("seamComponent", componentId);
    }

}
