/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.contentview.jsf.facelets.plugins;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectManyCheckbox;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.ui.web.renderer.NxSelectManyCheckboxListRenderer;

/**
 * @since 11.1
 */
public class SelectManyCheckboxUserAndGroupAggregateWidgetTypeHandler
        extends SelectUserAndGroupAggregateWidgetTypeHandler {

    public SelectManyCheckboxUserAndGroupAggregateWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        apply(ctx, parent, widget, HtmlSelectManyCheckbox.COMPONENT_TYPE,
                NxSelectManyCheckboxListRenderer.RENDERER_TYPE);
    }

}
