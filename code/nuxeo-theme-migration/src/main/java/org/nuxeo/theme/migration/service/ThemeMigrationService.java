/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.migration.service;

import org.nuxeo.ecm.web.resources.core.ResourceDescriptor;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Compat service displaying migration information for old extension points.
 *
 * @since 7.4
 */
public class ThemeMigrationService extends DefaultComponent {

    protected static final String XP = "org.nuxeo.theme.services.ThemeService";

    protected static final String WR_XP = "org.nuxeo.ecm.platform.WebResources";

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("resources".equals(extensionPoint)) {
            if (contribution instanceof ResourceDescriptor) {
                ResourceDescriptor r = (ResourceDescriptor) contribution;
                String message = String.format(
                        "Resource '%s' on component %s should be contributed to extension point '%s' as of 7.4.",
                        r.getName(), contributor.getName(), WR_XP);
                DeprecationLogger.log(message, "7.4");
                addRuntimeMessage(Level.ERROR, message);
            } else {
                String message = String.format("Warning: unknown contribution to target extension point '%s' of '%s'. "
                        + "Check your extension in component %s", extensionPoint, XP, contributor.getName());
                DeprecationLogger.log(message, "7.4");
                addRuntimeMessage(Level.WARNING, message);
            }
        } else {
            String message = String.format(
                    "Warning: target extension point '%s' of '%s' is unknown as it has been removed since 7.4. "
                            + "Check your extension in component %s",
                    extensionPoint, XP, contributor.getName());
            DeprecationLogger.log(message, "7.4");
            addRuntimeMessage(Level.WARNING, message);
        }
    }

}
