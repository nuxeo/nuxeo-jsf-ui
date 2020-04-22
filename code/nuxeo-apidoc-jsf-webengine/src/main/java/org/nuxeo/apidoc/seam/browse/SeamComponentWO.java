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

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.browse.NuxeoArtifactWebObject;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.plugin.SeamPlugin;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = SeamPlugin.ITEM_VIEW_TYPE)
public class SeamComponentWO extends NuxeoArtifactWebObject {

    protected SeamComponentInfo getTargetComponentInfo() {
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession());
        @SuppressWarnings("unchecked")
        PluginSnapshot<SeamComponentInfo> seamSnapshot = (PluginSnapshot<SeamComponentInfo>) snapshot.getPluginSnapshots()
                                                                                                     .get(SeamPlugin.ID);
        return seamSnapshot.getItem(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetComponentInfo();
    }

}
