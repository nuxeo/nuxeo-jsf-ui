/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.service;

import static org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager.CATEGORY_XML_ATTRIBUTE;
import static org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager.JSF_CATEGORY;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.core.service.LayoutStoreImpl;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Holds Registry classes forwarding contributions to {@link LayoutStore} registries.
 *
 * @since 11.5
 */
public abstract class WebLayoutRegistry {

    protected static final String TARGET_COMPONENT = "org.nuxeo.ecm.platform.forms.layout.LayoutStore";

    private WebLayoutRegistry() {
    }

    protected static class ForwardingRegistry implements Registry {

        protected final String targetPoint;

        public ForwardingRegistry(String targetPoint) {
            super();
            this.targetPoint = targetPoint;
        }

        public Registry getTargetRegistry() {
            return Framework.getRuntime()
                            .getComponentManager()
                            .getExtensionPointRegistry(TARGET_COMPONENT, targetPoint)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    String.format("Unknown target registry %s--%s", TARGET_COMPONENT, targetPoint)));
        }

        @Override
        public void tag(String id) {
            getTargetRegistry().tag(id);
        }

        @Override
        public boolean isTagged(String id) {
            return getTargetRegistry().isTagged(id);
        }

        @Override
        public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
            element.setAttribute(CATEGORY_XML_ATTRIBUTE, JSF_CATEGORY);
            getTargetRegistry().register(ctx, xObject, element, tag);
        }

        @Override
        public void unregister(String tag) {
            getTargetRegistry().unregister(tag);
        }

    }

    public static final class WidgetTypeRegistry extends ForwardingRegistry {

        public WidgetTypeRegistry() {
            super(LayoutStoreImpl.WIDGET_TYPES_EP_NAME);
        }

    }

    public static final class LayoutTypeRegistry extends ForwardingRegistry {

        public LayoutTypeRegistry() {
            super(LayoutStoreImpl.LAYOUT_TYPES_EP_NAME);
        }

    }

    public static final class LayoutRegistry extends ForwardingRegistry {

        public LayoutRegistry() {
            super(LayoutStoreImpl.LAYOUTS_EP_NAME);
        }

    }

    public static final class WidgetRegistry extends ForwardingRegistry {

        public WidgetRegistry() {
            super(LayoutStoreImpl.WIDGETS_EP_NAME);
        }

    }

}
