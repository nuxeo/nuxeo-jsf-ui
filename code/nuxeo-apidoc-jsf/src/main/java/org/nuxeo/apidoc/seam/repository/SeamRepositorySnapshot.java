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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.snapshot.SeamDistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;

public class SeamRepositorySnapshot extends RepositoryDistributionSnapshot
        implements SeamDistributionSnapshot {

    public SeamRepositorySnapshot(DocumentModel doc) {
        super(doc);
    }

    public static SeamRepositorySnapshot create(DistributionSnapshot distrib, CoreSession session,
            String containerPath, String label, Map<String, Serializable> properties) {
        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName(distrib.getKey());
        if (label != null) {
            name = computeDocumentName(label);
        }
        String targetPath = new Path(containerPath).append(name).toString();

        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }

        // Set first properties passed by parameter to not override default
        // behavior
        if (properties != null) {
            properties.forEach(doc::setPropertyValue);
        }

        doc.setPathInfo(containerPath, name);
        if (label == null) {
            doc.setPropertyValue("dc:title", distrib.getKey());
            doc.setPropertyValue(PROP_KEY, distrib.getKey());
            doc.setPropertyValue(PROP_NAME, distrib.getName());
        } else {
            doc.setPropertyValue("dc:title", label);
            doc.setPropertyValue(PROP_KEY, label + "-" + distrib.getVersion());
            doc.setPropertyValue(PROP_NAME, label);
        }
        doc.setPropertyValue(PROP_LATEST_FT, distrib.isLatestFT());
        doc.setPropertyValue(PROP_LATEST_LTS, distrib.isLatestLTS());
        doc.setPropertyValue(PROP_VERSION, distrib.getVersion());

        DocumentModel ret;
        if (exist) {
            ret = session.saveDocument(doc);
        } else {
            ret = session.createDocument(doc);
        }
        return new SeamRepositorySnapshot(ret);
    }

    @Override
    public SeamComponentInfo getSeamComponent(String id) {
        String name = id.replace("seam:", "");
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc()) + " AND "
                + SeamComponentInfo.PROP_COMPONENT_NAME + " = " + NXQL.escapeString(name);
        DocumentModelList docs = getCoreSession().query(query);
        return docs.isEmpty() ? null : docs.get(0).getAdapter(SeamComponentInfo.class);
    }

    @Override
    public List<String> getSeamComponentIds() {
        List<String> result = new ArrayList<>();
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc());
        DocumentModelList docs = getCoreSession().query(query);
        for (DocumentModel doc : docs) {
            result.add(doc.getAdapter(SeamComponentInfo.class).getId());
        }
        return result;
    }

    @Override
    public List<SeamComponentInfo> getSeamComponents() {
        List<SeamComponentInfo> result = new ArrayList<>();
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc());
        DocumentModelList docs = getCoreSession().query(query);
        for (DocumentModel doc : docs) {
            result.add(doc.getAdapter(SeamComponentInfo.class));
        }
        return result;
    }

    @Override
    public boolean containsSeamComponents() {
        return getSeamComponentIds().size() > 0;
    }

}
