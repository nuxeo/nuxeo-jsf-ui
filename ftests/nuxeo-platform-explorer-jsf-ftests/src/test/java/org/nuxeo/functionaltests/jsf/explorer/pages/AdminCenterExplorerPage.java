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
package org.nuxeo.functionaltests.jsf.explorer.pages;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page representing the tabs for explorer in JSF Admin Center.
 *
 * @since 11.1
 */
public class AdminCenterExplorerPage extends AdminCenterBasePage {

    @FindBy(xpath = "//div[@id=\"nxw_adminCenterSubTabs_panel\"]/ul/li[@class=\"selected\"]/form/a")
    public WebElement selectedTab;

    @Required
    @FindBy(xpath = "//a[@id='nxw_PlatformExplorerBrowse_form:nxw_PlatformExplorerBrowse']")
    public WebElement browse;

    @Required
    @FindBy(xpath = "//a[@id='nxw_PlatformExplorerXP_form:nxw_PlatformExplorerXP']")
    public WebElement extensionPoints;

    @Required
    @FindBy(xpath = "//a[@id='nxw_PlatformExplorerSeam_form:nxw_PlatformExplorerSeam']")
    public WebElement seamComponents;

    @Required
    @FindBy(xpath = "//a[@id='nxw_PlatformExplorerOp_form:nxw_PlatformExplorerOp']")
    public WebElement operations;

    @Required
    @FindBy(xpath = "//div[@id='nxw_adminCenterSubTabs_tab_content']//h3")
    public WebElement frameTitle;

    public AdminCenterExplorerPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void clickOnTab(WebElement tab) {
        clickOnTab(tab, useAjaxTabs());
    }

    public String getFrameFirstContent() {
        WebElement frame = driver.findElement(By.id("nxw_adminCenterSubTabs_tab_content"))
                                 .findElement(By.tagName("iframe"));
        driver.switchTo().frame(frame);
        ArtifactHomePage page = asPage(ArtifactHomePage.class);
        String text = page.getFirstListingElement().getText();
        driver.switchTo().defaultContent();
        return text;
    }

}
