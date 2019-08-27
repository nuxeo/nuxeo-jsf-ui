/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.NoteDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.NoteCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceCreationFormPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.openqa.selenium.By;

/**
 * Coverage Navigation tests.
 */
public class ITCoverageNavigationTest extends AbstractTest {

    public static final String NXPATH_URL_FORMAT = "/nxpath/default%s@view_documents";

    public static final String WORKSPACES_TITLE = "Workspaces";

    public static final String WORKSPACES_PATH = "/default-domain/workspaces/";

    public static final String WORKSPACE_TYPE = "Workspace";

    public static final String TEST_WORKSPACE_TITLE = "ws";

    public static final String TEST_WORKSPACE_PATH = WORKSPACES_PATH + TEST_WORKSPACE_TITLE + "/";

    public static final String TEST_WORKSPACE_URL = String.format(NXPATH_URL_FORMAT, TEST_WORKSPACE_PATH);

    public final static String NOTE_TITLE = "Note with coverage";

    public final static String NOTE_TYPE = "Note";

    public final static String NOTE_DESCRIPTION = "Description of note with coverage";

    @Before
    public void before() throws UserNotConnectedException {
        DocumentBasePage page = login();
        page = page.getContentTab().goToDocument(WORKSPACES_TITLE);
        WorkspaceCreationFormPage formPage = page.getContentTab().getWorkspacesContentTab().getWorkspaceCreatePage();
        formPage.createNewWorkspace(TEST_WORKSPACE_TITLE, "");
    }

    @After
    public void after() throws UserNotConnectedException {
        DocumentBasePage page = login();
        page = page.getContentTab().goToDocument(WORKSPACES_TITLE);
        page.getContentTab().removeAllDocuments();
    }

    @Test
    public void testCoverageNavigation() throws Exception {
        login();
        createNoteWithCoverage("Algeria");

        // show coverage navigation tab
        Locator.findElement(By.xpath("//img[@alt='Browsing by coverage']")).click();

        // check tree root is present and click
        Locator.findElementWaitUntilEnabledAndClick(
                By.id("directoryTreeForm:directoryNavTree:directoryNavRecursiveAdaptor.0:directoryNavCommandLink"));

        // check Africa is present and click
        Locator.waitUntilElementPresent(By.linkText("Africa"));
        Locator.findElement(By.xpath(
                "//div[@id='directoryTreeForm:directoryNavTree:directoryNavRecursiveAdaptor.0.directoryNavRecursiveAdaptor.0:directoryNavTreeNode']/div/span")).click();

        // check Algeria is present and click
        Locator.waitUntilElementPresent(By.linkText("Algeria"));
        Locator.findElement(By.id(
                "directoryTreeForm:directoryNavTree:directoryNavRecursiveAdaptor.0.directoryNavRecursiveAdaptor.0.directoryNavRecursiveAdaptor.0:directoryNavCommandLink")).click();

        // check Algeria Note is present
        Locator.waitForTextPresent(By.id("byCoverageContentView"), NOTE_TITLE);

        logout();
    }

    protected void createNoteWithCoverage(String coverage) throws IOException {
        // create note
        NoteCreationFormPage formPage = get(NUXEO_URL+TEST_WORKSPACE_URL+"?conversationId=0NXMAIN", DocumentBasePage.class).getContentTab().getDocumentCreatePage(NOTE_TYPE,
                NoteCreationFormPage.class);
        NoteDocumentBasePage noteDocumentBasePage = formPage.createNoteDocument(NOTE_TITLE, NOTE_DESCRIPTION, false,
                null);

        EditTabSubPage editTab = noteDocumentBasePage.getEditTab();
        // set coverage
        Select2WidgetElement select2WidgetElement = new Select2WidgetElement(driver,
                "s2id_document_edit:nxl_dublincore:nxw_coverage_1_select2");
        select2WidgetElement.selectValue(coverage, true);
        editTab.save();
    }
}
