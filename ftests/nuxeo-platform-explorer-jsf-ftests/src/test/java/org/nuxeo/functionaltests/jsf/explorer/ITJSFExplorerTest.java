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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.jsf.explorer.pages.AdminCenterExplorerPage;
import org.nuxeo.functionaltests.jsf.explorer.pages.ArtifactHomePage;
import org.nuxeo.functionaltests.jsf.explorer.pages.ArtifactPage;
import org.nuxeo.functionaltests.jsf.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test explorer JSF-specific pages.
 *
 * @since 11.1
 */
public class ITJSFExplorerTest extends AbstractTest {

    @Before
    public void before() {
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

    protected ExplorerHomePage goHome() {
        open(ExplorerHomePage.URL);
        return asPage(ExplorerHomePage.class);
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
        ExplorerHomePage home = goHome();
        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        ahome.navigateTo(ahome.seamComponents);
        assertTrue(ahome.isSelected(ahome.seamComponents));
        WebElement elt = ahome.getFirstListingElement();
        WebElement link = elt.findElement(By.xpath(".//a"));
        assertEquals("actionContextProvider", link.getText());
        assertEquals("STATELESS", elt.findElement(By.xpath(".//span[@class='sticker']")).getText());
        assertEquals("org.nuxeo.ecm.webapp.action.ActionContextProvider",
                elt.findElement(By.xpath(".//td[3]")).getText());
        Locator.scrollAndForceClick(link);
        ArtifactPage apage = asPage(ArtifactPage.class);
        assertTrue(apage.isSelected(apage.seamComponents));
        assertEquals("Seam component actionContextProvider", apage.getTitle());
        assertEquals("Seam component actionContextProvider", apage.header.getText());
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
