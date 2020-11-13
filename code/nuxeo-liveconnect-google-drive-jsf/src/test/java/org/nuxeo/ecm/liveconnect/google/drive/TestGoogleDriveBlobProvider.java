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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.liveconnect.google.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.liveconnect.LiveConnectFeature.SERVICE_GOOGLE_DRIVE_ID;
import static org.nuxeo.ecm.liveconnect.LiveConnectFeature.USER_ID;
import static org.nuxeo.ecm.liveconnect.LiveConnectFeature.createBlob;
import static org.nuxeo.ecm.liveconnect.google.drive.GoogleDriveBlobProvider.DEFAULT_EXPORT_MIMETYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.liveconnect.LiveConnectFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

@RunWith(FeaturesRunner.class)
@Features(LiveConnectFeature.class)
public class TestGoogleDriveBlobProvider {

    protected static final String USERNAME = USER_ID.replace("@.*", "");

    protected static final String JPEG_FILE_ID = "12341234";

    protected static final String JPEG_REV_ID = "v1abcd";

    protected static final int JPEG_SIZE = 36830;

    protected static final int JPEG_REV_SIZE = 18581;

    protected static final String GOOGLEDOC_FILE_ID = "56785678";

    protected static final String GOOGLEDOC_REV_ID = "4551";

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected BlobManager blobManager;

    @Inject
    protected DocumentBlobManager documentBlobManager;

    @Inject
    protected CoreSession session;

    @Test
    public void testSupportsUserUpdate() throws Exception {
        BlobProvider blobProvider = blobManager.getBlobProvider(SERVICE_GOOGLE_DRIVE_ID);
        assertTrue(blobProvider.supportsUserUpdate());
        assertTrue(blobProvider.supportsSync());

        // check that we can prevent user updates of blobs by configuration
        deployer.deploy("org.nuxeo.ecm.liveconnect.test:OSGI-INF/test-googledrive-config2.xml");
        blobProvider = blobManager.getBlobProvider(SERVICE_GOOGLE_DRIVE_ID);
        assertFalse(blobProvider.supportsUserUpdate());
        assertFalse(blobProvider.supportsSync());
    }

    @Test
    public void testStreamUploaded() throws Exception {
        Blob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, JPEG_FILE_ID);
        try (InputStream is = blob.getStream()) {
            assertNotNull(is);
            byte[] bytes = IOUtils.toByteArray(is);
            assertEquals(JPEG_SIZE, bytes.length);
        }
    }

    @Test
    public void testStreamRevisionUploaded() throws Exception {
        Blob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, JPEG_FILE_ID, UUID.randomUUID().toString(), JPEG_REV_ID);
        try (InputStream is = blob.getStream()) {
            assertNotNull(is);
            byte[] bytes = IOUtils.toByteArray(is);
            assertEquals(JPEG_REV_SIZE, bytes.length);
        }
    }

    @Test
    public void testStreamNative() throws Exception {
        Blob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, GOOGLEDOC_FILE_ID);
        try (InputStream is = blob.getStream()) {
            assertNull(is);
        }
    }

    @Test
    public void testStreamRevisionNative() throws Exception {
        Blob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, GOOGLEDOC_FILE_ID, UUID.randomUUID().toString(),
                GOOGLEDOC_REV_ID);
        try (InputStream is = blob.getStream()) {
            assertNull(is);
        }
    }

    @Test
    public void testExportedLinksUploaded() throws IOException {
        Blob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, JPEG_FILE_ID);
        Map<String, URI> map = blobManager.getAvailableConversions(blob, null);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testExportedLinksNative() throws IOException {
        Blob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, GOOGLEDOC_FILE_ID);
        Map<String, URI> map = blobManager.getAvailableConversions(blob, null);
        assertTrue(map.containsKey("application/pdf"));
    }

    @Test
    public void testAppLinks() throws IOException {
        ManagedBlob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, JPEG_FILE_ID);

        BlobProvider provider = blobManager.getBlobProvider(SERVICE_GOOGLE_DRIVE_ID);
        List<AppLink> appLinks = provider.getAppLinks(USERNAME, blob);

        assertEquals(2, appLinks.size());

        AppLink app = appLinks.get(0);
        assertEquals("App #1", app.getAppName());
        assertEquals("editor_16.png", app.getIcon());

        assertEquals("App #2", appLinks.get(1).getAppName());
    }

    @Test
    public void testSaveBlob() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");

        Blob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, JPEG_FILE_ID);
        doc.setPropertyValue("file:content", (Serializable) blob);

        session.createDocument(doc);
        session.save();

        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");

        assertTrue(blob instanceof ManagedBlob);
        ManagedBlob mb = (ManagedBlob) blob;
        assertEquals(SERVICE_GOOGLE_DRIVE_ID, mb.getProviderId());
        assertEquals(SERVICE_GOOGLE_DRIVE_ID + ":" + USER_ID + ":" + JPEG_FILE_ID, mb.getKey());
    }

    @Test
    public void testBlobCheckInUploaded() {
        testBlobCheckIn(JPEG_FILE_ID, JPEG_REV_ID);
    }

    @Test
    @Deploy("org.nuxeo.ecm.liveconnect.google.drive.jsf.test:test-liveconnect-disable-fulltext-config.xml")
    public void testBlobCheckInNative() throws Exception {
        DocumentModel version = testBlobCheckIn(GOOGLEDOC_FILE_ID, GOOGLEDOC_REV_ID);
        assertTrue(version.hasFacet(GoogleDriveBlobProvider.BLOB_CONVERSIONS_FACET));

        Blob blob = (Blob) version.getPropertyValue("file:content");

        // native files do not support downloading revision conversions
        assertTrue(blobManager.getAvailableConversions(blob, BlobManager.UsageHint.STREAM).isEmpty());

        // still we can get our stored conversion
        assertNotNull(documentBlobManager.getConvertedStream(blob, DEFAULT_EXPORT_MIMETYPE, version));

    }

    protected DocumentModel testBlobCheckIn(String fileId, String revisionId) {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = createBlob(SERVICE_GOOGLE_DRIVE_ID, fileId);
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(doc);
        session.save();

        // now check in
        DocumentRef verRef = session.checkIn(doc.getRef(), VersioningOption.MAJOR, null);
        DocumentModel version = session.getDocument(verRef);

        Blob verBlob = (Blob) version.getPropertyValue("file:content");
        assertTrue(verBlob instanceof ManagedBlob);
        ManagedBlob mvb = (ManagedBlob) verBlob;
        assertEquals(SERVICE_GOOGLE_DRIVE_ID, mvb.getProviderId());
        assertEquals(SERVICE_GOOGLE_DRIVE_ID + ":" + USER_ID + ":" + fileId + ":" + revisionId, mvb.getKey());

        return version;
    }

}
