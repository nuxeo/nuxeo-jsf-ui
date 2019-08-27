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
package org.nuxeo.ftest.smart.search.pages;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.pages.search.AbstractSearchSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * @since 8.1
 */
public class SmartSearchSubPage extends AbstractSearchSubPage {

    public SmartSearchSubPage(WebDriver driver) {
        super(driver);
    }

    public SmartSearchSubPage selectCriterion(String text, String operator, String value) {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        Select crit = new Select(driver.findElement(By.xpath("//select[contains(@id, '_rowSelector')]")));
        arm.begin();
        crit.selectByVisibleText(text);
        arm.end();
        Select op = new Select(driver.findElement(By.xpath("//select[contains(@id, 'conditional_operator')]")));
        arm.begin();
        op.selectByVisibleText(operator);
        arm.end();

        // implement simple input use case only
        WebElement input = driver.findElement(By.xpath("//input[contains(@id, 'query_condition')]"));
        input.sendKeys(value);

        return asPage(SmartSearchSubPage.class);
    }

    public SmartSearchSubPage filterWith(String text, String operator, String value) {
        selectCriterion(text, operator, value);
        WebElement addButton = driver.findElement(By.xpath("//input[contains(@id, '_addToQuery')]"));
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        addButton.click();
        arm.end();
        filter();
        return asPage(SmartSearchSubPage.class);
    }

}
