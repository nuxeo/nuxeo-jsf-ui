/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.seam.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.seam.introspection.SeamRuntimeSnapshot;
import org.nuxeo.apidoc.seam.plugin.SeamPlugin;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(SeamSnaphotFeature.class)
public class TestSeamPlugin {

    @Inject
    protected SnapshotManager snapshotManager;

    @Test
    public void testRegistration() {
        Plugin<?> p = snapshotManager.getPlugin(SeamPlugin.ID);
        assertNotNull(p);
        assertTrue(p instanceof SeamPlugin);
    }

    @Test
    public void testPlugin() {
        Plugin<?> p = snapshotManager.getPlugin(SeamPlugin.ID);
        assertNotNull(p);
        assertEquals(SeamPlugin.ID, p.getId());
        assertEquals(SeamPlugin.VIEW_TYPE, p.getViewType());
        assertEquals("Seam Components", p.getLabel());
        assertEquals(SeamPlugin.LIST_VIEW, p.getHomeView());
        assertEquals("seam", p.getStyleClass());
        assertFalse(p.isHidden());
    }

    @Test
    public void testPluginRuntimeSnapshot() {
        DistributionSnapshot snapshot = snapshotManager.getRuntimeSnapshot();
        PluginSnapshot<?> psnap = snapshot.getPluginSnapshots().get(SeamPlugin.ID);
        assertNotNull(psnap);
        assertTrue(psnap instanceof SeamRuntimeSnapshot);
        List<String> itemIds = psnap.getItemIds();
        assertNotNull(itemIds);
        // cannot extract seam components in unit tests
        assertEquals(0, itemIds.size());
    }

}
