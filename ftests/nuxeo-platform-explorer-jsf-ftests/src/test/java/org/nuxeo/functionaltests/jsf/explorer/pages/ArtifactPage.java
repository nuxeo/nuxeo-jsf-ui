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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page representing a selected artifact.
 *
 * @since 11.1
 */
public class ArtifactPage extends ArtifactHomePage {

    @Required
    @FindBy(xpath = "//section/article[@role='contentinfo']/h1")
    public WebElement header;

    @Required
    @FindBy(xpath = "//div[@class='implementation']")
    public WebElement implementation;

    @Required
    @FindBy(xpath = "//div[@class='implementation']//a[@class='javadoc']")
    public WebElement javadocLink;

    @FindBy(xpath = "//ul[@class='interfaces']")
    public WebElement interfaces;

    @FindBy(xpath = "//ul[@class='interfaces']//a[@class='javadoc']")
    public WebElement firstInterfaceLink;

    public ArtifactPage(WebDriver driver) {
        super(driver);
    }

    public void checkReference() {
        assertTrue(isSelected(seamComponents));
        checkTitle("Seam Component actionContextProvider");
        assertEquals("Seam Component actionContextProvider", header.getText());
        checkImplementationText("org.nuxeo.ecm.webapp.action.ActionContextProvider");
        checkJavadocLink("/javadoc/org/nuxeo/ecm/webapp/action/ActionContextProvider.html");
        checkInterfacesText(null);
        checkFirstInterfaceLink(null);
    }

    public void checkAlternative() {
        assertTrue(isSelected(seamComponents));
        checkTitle("Seam Component clipboardActions");
        assertEquals("Seam Component clipboardActions", header.getText());
        checkImplementationText("org.nuxeo.ecm.webapp.clipboard.ClipboardActionsBean");
        checkJavadocLink("/javadoc/org/nuxeo/ecm/webapp/clipboard/ClipboardActionsBean.html");
        checkInterfacesText("org.nuxeo.ecm.webapp.clipboard.ClipboardActions");
        checkFirstInterfaceLink("/javadoc/org/nuxeo/ecm/webapp/clipboard/ClipboardActions.html");
    }

    public void checkImplementationText(String expected) {
        assertEquals(expected, implementation.getText());
    }

    public void checkJavadocLink(String expected) {
        checkJavadocLink(expected, javadocLink);
    }

    public void checkInterfacesText(String expected) {
        try {
            assertEquals(expected, interfaces.getText());
        } catch (NoSuchElementException e) {
            assertNull(expected);
        }
    }

    public void checkFirstInterfaceLink(String expected) {
        checkJavadocLink(expected, firstInterfaceLink);
    }

    protected void checkJavadocLink(String expected, WebElement link) {
        try {
            String href = link.getAttribute("href");
            assertFalse("Actual href: " + href, StringUtils.isBlank(expected));
            assertTrue("Actual href: " + href, href.endsWith(expected));
        } catch (NoSuchElementException e) {
            assertNull(expected);
        }
    }

}
