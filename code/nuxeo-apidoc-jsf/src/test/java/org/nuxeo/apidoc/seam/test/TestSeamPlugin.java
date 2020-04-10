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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.seam.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.introspection.SeamComponentInfoImpl;
import org.nuxeo.apidoc.seam.introspection.SeamRuntimeSnapshot;
import org.nuxeo.apidoc.seam.plugin.SeamPlugin;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

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

    /**
     * Fakes seam components detection in unit tests.
     */
    protected void fakeInitSnapshot(DistributionSnapshot snapshot) {
        PluginSnapshot<?> psnap = snapshot.getPluginSnapshots().get(SeamPlugin.ID);
        assertNotNull(psnap);
        assertTrue(psnap instanceof SeamRuntimeSnapshot);
        List<SeamComponentInfo> components = List.of(
                buildFake("actionContextProvider", "STATELESS", "org.nuxeo.ecm.webapp.action.ActionContextProvider"),
                buildFake("adminMessageManager", "APPLICATION", "org.nuxeo.ecm.webapp.admin.AdminMessageActionBean"));
        ((SeamRuntimeSnapshot) psnap).initSeamComponents(snapshot, components);
    }

    protected SeamComponentInfo buildFake(String name, String scope, String className) {
        SeamComponentInfoImpl comp = new SeamComponentInfoImpl();
        comp.setName(name);
        comp.setScope(scope);
        comp.setClassName(className);
        comp.addInterfaceName(className);
        return comp;
    }

    @Test
    public void testPluginRuntimeSnapshot() {
        DistributionSnapshot snapshot = snapshotManager.getRuntimeSnapshot();
        fakeInitSnapshot(snapshot);

        PluginSnapshot<?> psnap = snapshot.getPluginSnapshots().get(SeamPlugin.ID);
        assertNotNull(psnap);
        assertTrue(psnap instanceof SeamRuntimeSnapshot);
        checkPluginRuntimeSnapshot((SeamRuntimeSnapshot) psnap, false);
    }

    @Test
    public void testPluginJson() throws JsonGenerationException, JsonMappingException, IOException {
        DistributionSnapshot snapshot = snapshotManager.getRuntimeSnapshot();
        fakeInitSnapshot(snapshot);

        // write to output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        snapshot.writeJson(out);

        // read back and explore plugin resources again
        ByteArrayInputStream source = new ByteArrayInputStream(out.toByteArray());
        DistributionSnapshot rsnap = snapshot.readJson(source);

        PluginSnapshot<?> psnap = rsnap.getPluginSnapshots().get(SeamPlugin.ID);
        assertNotNull(psnap);
        assertTrue(psnap instanceof SeamRuntimeSnapshot);
        checkPluginRuntimeSnapshot((SeamRuntimeSnapshot) psnap, false);
    }

    /**
     * Test a legacy NuxeoArtifact an still be resolved thanks to this old-exported json, that can also serve a
     * json-comaptibility test for the whole json, not only with plugins.
     */
    @Test
    public void testPluginJsonLegacy() throws JsonGenerationException, JsonMappingException, IOException {
        String export = getReferenceContent("seam-test-export.json");

        // read back and explore plugin resources again
        ByteArrayInputStream source = new ByteArrayInputStream(export.getBytes());
        // retrieve current snapshot just to get the reader...
        DistributionSnapshot snapshot = snapshotManager.getRuntimeSnapshot();
        DistributionSnapshot rsnap = snapshot.readJson(source);

        PluginSnapshot<?> psnap = rsnap.getPluginSnapshots().get(SeamPlugin.ID);
        assertNotNull(psnap);
        assertTrue(psnap instanceof SeamRuntimeSnapshot);
        checkPluginRuntimeSnapshot((SeamRuntimeSnapshot) psnap, true);
    }

    protected void checkPluginRuntimeSnapshot(SeamRuntimeSnapshot psnapshot, boolean legacy) {
        String version = legacy ? "10.10" : "unknown";

        List<String> itemIds = psnapshot.getItemIds();
        assertNotNull(itemIds);
        assertEquals(2, itemIds.size());
        assertEquals("seam:actionContextProvider", itemIds.get(0));
        assertEquals("seam:adminMessageManager", itemIds.get(1));

        SeamComponentInfo comp1 = psnapshot.getItem("seam:actionContextProvider");
        assertNotNull(comp1);
        assertEquals(SeamComponentInfo.TYPE_NAME, comp1.getArtifactType());
        assertEquals("org.nuxeo.ecm.webapp.action.ActionContextProvider", comp1.getClassName());
        assertEquals("/", comp1.getHierarchyPath());
        assertEquals("seam:actionContextProvider", comp1.getId());
        assertNotNull(comp1.getInterfaceNames());
        assertEquals(1, comp1.getInterfaceNames().size());
        assertEquals("org.nuxeo.ecm.webapp.action.ActionContextProvider", comp1.getInterfaceNames().get(0));
        assertEquals("actionContextProvider", comp1.getName());
        assertNull(comp1.getPrecedence());
        assertEquals("STATELESS", comp1.getScope());
        assertEquals(version, comp1.getVersion());

        SeamComponentInfo comp2 = psnapshot.getItem("seam:adminMessageManager");
        assertNotNull(comp2);
        assertEquals("org.nuxeo.ecm.webapp.admin.AdminMessageActionBean", comp2.getClassName());
        assertEquals("APPLICATION", comp2.getScope());
        assertEquals(version, comp2.getVersion());
    }

    public static String getReferenceContent(String path) throws IOException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(path);
        if (fileUrl == null) {
            throw new IllegalStateException("File not found: " + path);
        }
        String refPath = FileUtils.getFilePathFromUrl(fileUrl);
        return org.apache.commons.io.FileUtils.readFileToString(new File(refPath), StandardCharsets.UTF_8).trim();
    }

}
