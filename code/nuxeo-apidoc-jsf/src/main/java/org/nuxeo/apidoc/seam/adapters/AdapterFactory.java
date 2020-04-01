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
package org.nuxeo.apidoc.seam.adapters;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * Factory for DocumentModel adapters.
 */
public class AdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> adapterClass) {

        if (doc == null) {
            return null;
        }

        String adapterClassName = adapterClass.getSimpleName();

        if (adapterClassName.equals(SeamComponentInfo.class.getSimpleName())) {
            if (doc.getType().equals(SeamComponentInfo.TYPE_NAME)) {
                return new SeamComponentInfoDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(NuxeoArtifact.class.getSimpleName())) {
            if (doc.getType().equals(SeamComponentInfo.TYPE_NAME)) {
                return new SeamComponentInfoDocAdapter(doc);
            }
        }

        return null;
    }

}
