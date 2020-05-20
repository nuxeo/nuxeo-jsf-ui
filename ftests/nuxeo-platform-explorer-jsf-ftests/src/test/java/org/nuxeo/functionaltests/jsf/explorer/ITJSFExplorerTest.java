/*
 * (C) Copyright 2014-2020 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.jsf.explorer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.seam.plugin.SeamPlugin;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.JavaScriptErrorCollector;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.jsf.explorer.pages.AdminCenterExplorerPage;
import org.nuxeo.functionaltests.jsf.explorer.pages.ArtifactHomePage;
import org.nuxeo.functionaltests.jsf.explorer.pages.ArtifactPage;
import org.nuxeo.functionaltests.jsf.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.jsf.explorer.pages.SeamListingFragment;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.openqa.selenium.By;

/**
 * Test explorer JSF-specific pages.
 *
 * @since 11.1
 */
public class ITJSFExplorerTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.deleteUser(TEST_USERNAME);
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    protected void doLogin() {
        getLoginPage().login(TEST_USERNAME, TEST_PASSWORD);
    }

    protected void doLogout() {
        // logout avoiding JS error check
        driver.get(NUXEO_URL + "/logout");
    }

    protected void open(String url) {
        JavaScriptErrorCollector.from(driver).ignore(ignores).checkForErrors();
        driver.get(NUXEO_URL + url);
        check404();
    }

    protected void check404() {
        assertFalse("404 on URL: '" + driver.getCurrentUrl(),
                driver.getTitle().contains(String.valueOf(HttpStatus.SC_NOT_FOUND)));
    }

    protected ExplorerHomePage goHome() {
        open(ExplorerHomePage.URL);
        return asPage(ExplorerHomePage.class);
    }

    protected ArtifactPage goToArtifact(String id) {
        open(String.format("%s%s/%s/%s/%s", ExplorerHomePage.URL, ApiBrowserConstants.DISTRIBUTION_ALIAS_CURRENT,
                SeamPlugin.VIEW_TYPE, SeamPlugin.ITEM_VIEW, id));
        return asPage(ArtifactPage.class);
    }

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() throws UserNotConnectedException {
        doLogin();
        goHome();
        doLogout();
    }

    @Test
    public void testSeamComponents() throws UserNotConnectedException {
        doLogin();

        // navigate directly to seam component page to check for web context init
        goToArtifact("seam:actionContextProvider").checkReference();

        ExplorerHomePage home = goHome();
        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        ahome.navigateTo(ahome.seamComponents);
        assertTrue(ahome.isSelected(ahome.seamComponents));

        SeamListingFragment listing = asPage(SeamListingFragment.class);
        listing.checkListing(-1, "actionContextProvider", "/seam/viewSeamComponent/seam:actionContextProvider",
                "STATELESS", List.of("org.nuxeo.ecm.webapp.action.ActionContextProvider"),
                List.of("/javadoc/org/nuxeo/ecm/webapp/action/ActionContextProvider.html"));

        listing = listing.filterOn("clipboardActions");
        listing.checkListing(-1, "clipboardActions", "/seam/viewSeamComponent/seam:clipboardActions", "SESSION",
                List.of("org.nuxeo.ecm.webapp.clipboard.ClipboardActionsBean",
                        "org.nuxeo.ecm.webapp.clipboard.ClipboardActions"),
                List.of("/javadoc/org/nuxeo/ecm/webapp/clipboard/ClipboardActionsBean.html",
                        "/javadoc/org/nuxeo/ecm/webapp/clipboard/ClipboardActions.html"));

        listing.navigateToFirstItem();
        asPage(ArtifactPage.class).checkAlternative();

        doLogout();
    }

    @Test
    public void testAdminCenter() throws UserNotConnectedException {
        // log in as admin this time
        DocumentBasePage page = login();
        page.getAdminCenter();

        // tab is here but cannot click on it: need to fix NXP-28911 first, so navigate directly to second tab
        driver.findElement(By.linkText("Platform Explorer"));
        driver.get(NUXEO_URL
                + "/nxadmin/default/default-domain@view_admin?tabIds=NUXEO_ADMIN%3APlatformExplorer%3APlatformExplorerXP");
        AdminCenterExplorerPage amPage = asPage(AdminCenterExplorerPage.class);

        assertEquals(amPage.selectedTab, amPage.extensionPoints);
        assertEquals("actions\nActionService - org.nuxeo.ecm.platform.actions.ActionService",
                amPage.getFrameFirstContent());

        amPage.clickOnTab(amPage.seamComponents);
        assertEquals(amPage.selectedTab, amPage.seamComponents);
        assertEquals("STATELESS actionContextProvider org.nuxeo.ecm.webapp.action.ActionContextProvider",
                amPage.getFrameFirstContent());

        amPage.clickOnTab(amPage.operations);
        assertEquals(amPage.selectedTab, amPage.operations);
        assertEquals("acceptComment\nCHAIN acceptComment", amPage.getFrameFirstContent());

        doLogout();
    }

}
