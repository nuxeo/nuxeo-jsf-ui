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
package org.nuxeo.apidoc.seam.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.apidoc.seam.adapters.SeamComponentInfoDocAdapter;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.snapshot.SeamDistributionSnapshot;
import org.nuxeo.apidoc.seam.snapshot.SeamSnapshotFilter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public class SeamSnapshotPersister extends SnapshotPersister {

    public static final String Seam_Root_NAME = "Seam";

    // FIXME
    @Override
    public DistributionSnapshot persist(SeamDistributionSnapshot snapshot, CoreSession session, String label,
            SeamSnapshotFilter filter, Map<String, Serializable> properties) {
        SeamDistributionSnapshot distribContainer = super.persist(snapshot, session, label, filter, properties);

        DocumentModel seamContainer = getSubRoot(session, distribContainer.getDoc(), Seam_Root_NAME);
        persistSeamComponents(snapshot, snapshot.getSeamComponents(), session, label, seamContainer, filter);

        return distribContainer;
    }

    public void persistSeamComponents(SeamDistributionSnapshot snapshot, List<SeamComponentInfo> seamComponents,
            CoreSession session, String label, DocumentModel parent, SeamSnapshotFilter filter) {
        for (SeamComponentInfo seamComponent : seamComponents) {
            if (filter == null || filter.includeSeamComponent(seamComponent)) {
                persistSeamComponent(snapshot, seamComponent, session, label, parent);
            }
        }
    }

    public void persistSeamComponent(DistributionSnapshot snapshot, SeamComponentInfo seamComponent,
            CoreSession session, String label, DocumentModel parent) {
        SeamComponentInfoDocAdapter.create(seamComponent, session, parent.getPathAsString());
    }

}
