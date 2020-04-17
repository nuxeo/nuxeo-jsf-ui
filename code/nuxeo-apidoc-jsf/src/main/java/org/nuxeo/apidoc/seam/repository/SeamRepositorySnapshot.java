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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;

public class SeamRepositorySnapshot implements PluginSnapshot<SeamComponentInfo> {

    protected DocumentModel doc;

    public SeamRepositorySnapshot(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public List<String> getItemIds() {
        List<String> result = new ArrayList<>();
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, doc);
        DocumentModelList docs = doc.getCoreSession().query(query);
        for (DocumentModel doc : docs) {
            result.add(doc.getAdapter(SeamComponentInfo.class).getId());
        }
        return result;
    }

    @Override
    public List<SeamComponentInfo> getItems() {
        List<SeamComponentInfo> result = new ArrayList<>();
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, doc);
        DocumentModelList docs = doc.getCoreSession().query(query);
        for (DocumentModel doc : docs) {
            result.add(doc.getAdapter(SeamComponentInfo.class));
        }
        return result;
    }

    @Override
    public SeamComponentInfo getItem(String id) {
        String name = id.replace("seam:", "");
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, doc) + " AND "
                + SeamComponentInfo.PROP_COMPONENT_NAME + " = " + NXQL.escapeString(name);
        DocumentModelList docs = doc.getCoreSession().query(query);
        return docs.isEmpty() ? null : docs.get(0).getAdapter(SeamComponentInfo.class);
    }

}
