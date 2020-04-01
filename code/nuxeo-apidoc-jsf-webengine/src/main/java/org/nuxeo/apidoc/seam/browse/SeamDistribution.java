/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.browse.Distribution;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

//@Path("/distribution/seam")
//// needed for 5.4.1
//@WebObject(type = "distribution")
public class SeamDistribution extends Distribution {

    protected static final Log log = LogFactory.getLog(SeamDistribution.class);

    @Override
    public String getNavigationPoint() {
        String navPoint = super.getNavigationPoint();
        if (navPoint != null) {
            return navPoint;
        }
        String currentUrl = getContext().getURL();
        if (currentUrl.contains("/listSeamComponents")) {
            navPoint = "listSeamComponents";
        } else if (currentUrl.contains("/viewSeamComponent")) {
            navPoint = "viewSeamComponent";
        }
        return navPoint;
    }

    public static boolean showSeamComponent() {
        return !(Framework.isBooleanPropertyTrue("org.nuxeo.apidoc.hide.seam.components") || isSiteMode());
    }

}
