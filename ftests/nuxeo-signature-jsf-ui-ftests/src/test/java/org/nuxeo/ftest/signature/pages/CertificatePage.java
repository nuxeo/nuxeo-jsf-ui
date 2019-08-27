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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.signature.pages;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Home > Certificate page representation.
 *
 * @since 9.1
 */
public class CertificatePage extends AbstractPage {

    // make sure page is loaded
    @Required
    @FindBy(xpath = "//div[@id='nxw_userCenterSubTabs_tab_content']//h2")
    protected WebElement title;

    @FindBy(id = "certificateForm:firstPassword")
    protected WebElement firstPassword;

    @FindBy(id = "certificateForm:secondPassword")
    protected WebElement secondPassword;

    @FindBy(xpath = "//input[@value='Generate Certificate']")
    protected WebElement generateButton;

    @FindBy(xpath = "//div[@id='nxw_userCenterSubTabs_tab_content']//div//span")
    protected WebElement infoMessage;

    @FindBy(id = "nxl_cert:nxw_certWidget")
    protected WebElement certificateDetails;

    public CertificatePage(WebDriver driver) {
        super(driver);
    }

    public CertificatePage generateCertificate(String password) {
        firstPassword.sendKeys(password);
        secondPassword.sendKeys(password);
        Locator.waitUntilEnabledAndClick(generateButton);
        return asPage(CertificatePage.class);
    }

    public String getCertificationInfoMessage() {
        return infoMessage.getText();
    }

    public String getCertificateDetails() {
        return certificateDetails.getText();
    }

}
