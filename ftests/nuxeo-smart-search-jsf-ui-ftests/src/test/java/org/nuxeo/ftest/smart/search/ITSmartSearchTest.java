/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ftest.smart.search;

import java.io.IOException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ftest.smart.search.pages.SmartFolderContentTab;
import org.nuxeo.ftest.smart.search.pages.SmartFolderCreationPage;
import org.nuxeo.ftest.smart.search.pages.SmartSearchResultsSubPage;
import org.nuxeo.ftest.smart.search.pages.SmartSearchSubPage;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.HomePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.PermissionsSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;

/**
 * @since 8.1
 */
public class ITSmartSearchTest extends AbstractTest {

    protected final static String WS_ROOT_TITLE = "Workspaces";

    protected final static String SAVED_SEARCHES_TITLE = "Searches";

    protected final static String SMART_FOLDER_DOCTYPE_TITLE = "Smart Folder";

    protected final static String MANAGE_PERMISSION_LABEL = "Manage everything";

    protected String wsTitle;

    protected String docTitle;

    @Before
    public void setUp() throws UserNotConnectedException, IOException {
        DocumentBasePage base = login();

        // create test user if not there
        UsersTabSubPage usersTab = base.getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (!usersTab.isUserFound(TEST_USERNAME)) {
            usersTab.getUserCreatePage().createUser(TEST_USERNAME, TEST_USERNAME, "lastname1", "company1", "devnull@nuxeo.com",
                    TEST_PASSWORD, "members");
        }
        base = usersTab.exitAdminCenter();

        // create test ws
        wsTitle = "Test Smart Workspace " + new Date().getTime();
        DocumentBasePage ws = base.createWorkspace(wsTitle, null);

        // allow test user to create doc in it
        PermissionsSubPage permissionsSubPage = ws.getPermissionsTab();
        if (!permissionsSubPage.hasPermission(MANAGE_PERMISSION_LABEL, TEST_USERNAME)) {
            permissionsSubPage.grantPermission(MANAGE_PERMISSION_LABEL, TEST_USERNAME);
        }
        // create a test file under workspace
        docTitle = "Test Smart Doc " + new Date().getTime();
        ws.createFile(docTitle, null, false, null, null, null);
        logout();
    }

    @After
    public void tearDown() throws UserNotConnectedException {
        // remove test user
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        usersTab = usersTab.viewUser(TEST_USERNAME).deleteUser();
        usersTab.exitAdminCenter();
        // remove test ws
        asPage(ContentTabSubPage.class).goToDocument(WS_ROOT_TITLE);
        asPage(ContentTabSubPage.class).removeDocument(wsTitle);
        logout();
    }

    @Test
    public void testSmartFolderCreation() throws UserNotConnectedException, IOException {
        loginAsTestUser();
        asPage(ContentTabSubPage.class).goToDocument(WS_ROOT_TITLE);
        asPage(ContentTabSubPage.class).goToDocument(wsTitle);

        // create a smart folder
        SmartFolderCreationPage create = asPage(ContentTabSubPage.class).getDocumentCreatePage(
                SMART_FOLDER_DOCTYPE_TITLE, SmartFolderCreationPage.class);
        String sfTitle = "Test Smart Folder " + new Date().getTime();
        create.create(sfTitle, "Title", "CONTAINS", "Smart Doc");

        SmartFolderContentTab content = asPage(SmartFolderContentTab.class);
        assertEquals("dc:title LIKE '%Smart Doc%'", content.getQuery());
        assertEquals(1, content.getNumberOfDocuments());
        assertEquals(1, content.getListResults().size());
        assertEquals(docTitle, content.getListResults().get(0).findElement(By.className("documentTitle")).getText());
        logout();
    }

    @Test
    public void testSmartSearch() throws UserNotConnectedException, IOException {
        loginAsTestUser().goToSearchPage().selectSearch("Smart Search");
        SmartSearchSubPage ssp = asPage(SmartSearchSubPage.class);

        ssp.filterWith("Title", "CONTAINS", "Smart Doc");
        SmartSearchResultsSubPage rp = asPage(SmartSearchResultsSubPage.class);
        assertEquals(1, rp.getNumberOfDocumentInCurrentPage());
        assertEquals(docTitle, rp.getListResults().get(0).findElement(By.className("documentTitle")).getText());

        // create a smart folder from this search
        String saveAsPath = "//input[contains(@id, 'nxw_saveSearch_link')]";
        assertEquals(1, driver.findElements(By.xpath(saveAsPath)).size());
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        driver.findElement(By.xpath(saveAsPath)).click();
        arm.end();

        WebElement fancybox = Locator.findElementWithTimeout(By.id("nxw_saveSearch_after_view_box"));
        String sfTitle = "Test Smart Folder Created From Search " + new Date().getTime();
        fancybox.findElement(By.xpath(".//input[@type='text']")).sendKeys(sfTitle);
        arm.begin();
        fancybox.findElement(By.xpath(".//input[@value='Save']")).click();
        arm.end();

        SearchPage sp = asPage(SearchPage.class);
        assertEquals(sfTitle, sp.getSelectedSearch());

        // check that search was saved in home
        HomePage home = sp.goToHomePage();
        if (home.useAjaxTabs()) {
            arm.begin();
            driver.findElement(By.id("nxw_homeTabs_panel")).findElement(By.linkText(SAVED_SEARCHES_TITLE)).click();
            arm.end();
        } else {
            driver.findElement(By.id("nxw_homeTabs_panel")).findElement(By.linkText(SAVED_SEARCHES_TITLE)).click();
        }

        assertEquals(1, driver.findElements(By.linkText(sfTitle)).size());
        driver.findElement(By.linkText(sfTitle)).click();

        SmartFolderContentTab content = asPage(SmartFolderContentTab.class);
        assertEquals("dc:title LIKE '%Smart Doc%'", content.getQuery());
        assertEquals(1, content.getNumberOfDocuments());
        assertEquals(1, content.getListResults().size());
        assertEquals(docTitle, content.getListResults().get(0).findElement(By.className("documentTitle")).getText());

        // go to personal ws and delete saved search
        ContentTabSubPage persoContent = asPage(HomePage.class).switchToPersonalWorkspace().getContentTab();
        // check that it's visible here too
        assertEquals(1, driver.findElement(By.id("document_content")).findElements(By.linkText(sfTitle)).size());
        persoContent.removeDocument(sfTitle);

        logout();
    }

}
