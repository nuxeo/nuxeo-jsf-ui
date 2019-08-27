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
package org.nuxeo.lang.ext.incomplete.test;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * @since 9.1
 */
public class ITAdditionalLangTest extends AbstractTest {

    @Test
    public void testAdditionalLanguages() throws UserNotConnectedException {
        login();
        asPage(DocumentBasePage.class).goToHomePage().goTo("Preferences");
        WebElement form = driver.findElement(By.id("userpreferencesButtons"));
        Locator.findElementWaitUntilEnabledAndClick(form, By.id("userPreferencesDropDownMenu"));
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.findElementWaitUntilEnabledAndClick(form, By.id("userpreferencesButtons:editUserButton"));
        arm.end();

        Select select = new Select(driver.findElement(By.id("editUser:nxl_userpreferences_1:nxw_locale")));
        List<String> options = select.getOptions().stream().map(WebElement::getText).collect(toList());

        // check that some incomplete translations are listed here
        assertContains(options, "azərbaycan (Azərbaycan)");
        assertContains(options, "euskara (Espainia)");
        assertContains(options, "italiano (Italia)");

        logout();
    }

    private void assertContains(List<String> list, String actual) {
        if (!list.contains(actual)) {
            throw new AssertionError("expected:<" + actual + "> to exist in:<" + list + ">");
        }
    }

}
