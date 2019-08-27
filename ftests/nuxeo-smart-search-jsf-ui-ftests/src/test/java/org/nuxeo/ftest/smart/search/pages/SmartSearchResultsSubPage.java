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

import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.1
 */
public class SmartSearchResultsSubPage {

    private static final String SEARCH_RESULTS_XPATH = ".//table/tbody/tr";

    @Required
    @FindBy(xpath = "//div[@id='nxw_searchContentView']//div[contains(@id, 'nxw_searchContentView_resultsPanel')]/form")
    protected WebElement resultForm;

    public int getNumberOfDocumentInCurrentPage() {
        List<WebElement> result = resultForm.findElements(By.xpath(SEARCH_RESULTS_XPATH));
        return result.size();
    }

    /**
     * @return the list of results of the search.
     */
    public List<WebElement> getListResults() {
        try {
            return resultForm.findElements(By.xpath(SEARCH_RESULTS_XPATH));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
