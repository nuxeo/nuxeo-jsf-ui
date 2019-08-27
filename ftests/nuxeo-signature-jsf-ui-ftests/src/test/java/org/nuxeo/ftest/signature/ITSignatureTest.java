/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.signature;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ftest.signature.pages.CertificatePage;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;
import org.openqa.selenium.By;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.NXDOC_URL_FORMAT;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 9.1
 */
public class ITSignatureTest extends AbstractTest {

    public static final String TEST_WORKSPACE_TITLE = "ws";

    protected static String wsId;

    // define a specific test password to use more than 8 characters
    protected static String ALT_TEST_PASSWORD = "testtest";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, ALT_TEST_PASSWORD, "John", "Doe", "Nuxeo", "jdoe@localhost", "members");
        wsId = RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE);
        RestHelper.addPermission(wsId, TEST_USERNAME, SecurityConstants.WRITE);
    }

    @After
    public void after() {
        RestHelper.cleanup();
        wsId = null;
    }

    @Test
    public void testSignPDF() throws Exception {
        login(TEST_USERNAME, ALT_TEST_PASSWORD);
        open(String.format(NXDOC_URL_FORMAT, wsId));

        // import PDF file
        FileCreationFormPage creationPage = asPage(DocumentBasePage.class).getContentTab().getDocumentCreatePage(
                FILE_TYPE, FileCreationFormPage.class);
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource("samples/original.pdf");
        assertNotNull(fileUrl);
        String filePath = fileUrl.getPath();
        assertNotNull(filePath);
        FileDocumentBasePage filePage = creationPage.createFileDocument("original", null, filePath);

        // create certificate
        filePage.clickOnDocumentTabLink(driver.findElement(By.linkText("Signature")));
        Locator.findElementWaitUntilEnabledAndClick(By.linkText("Go to Certificate Management"));
        CertificatePage certPage = asPage(CertificatePage.class);
        assertEquals(TEST_USERNAME + ", You do not have a certificate.", certPage.getCertificationInfoMessage());
        certPage = certPage.generateCertificate("test");
        assertEquals("Your password is too short, it must be at least 8 characters.",
                certPage.getErrorFeedbackMessage());
        certPage = certPage.generateCertificate(ALT_TEST_PASSWORD);
        assertEquals("Your certificate password should be different from your login password.",
                certPage.getErrorFeedbackMessage());
        certPage = certPage.generateCertificate("Certificate");
        String details = certPage.getCertificateDetails();
        assertNotNull(details);
        assertTrue(details.contains("CN=John Doe,"));

        // sign PDF
        String fileId = getCurrentDocumentId();
        open(String.format(NXDOC_URL_FORMAT, fileId));
        asPage(DocumentBasePage.class).clickOnDocumentTabLink(driver.findElement(By.linkText("Signature")));
        driver.findElement(By.id("sign_form:signingReason")).sendKeys("test reason");
        driver.findElement(By.id("sign_form:password")).sendKeys("Certificate");
        Locator.findElementWaitUntilEnabledAndClick(By.id("sign_form:SignDoc"));

        String tabContent = driver.findElement(By.id("nxw_documentTabs_tab_content")).getText();
        assertNotNull(tabContent);
        assertTrue(tabContent.contains("Signed by"));

        logout();
    }

}
