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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.nuxeo.functionaltests.explorer.pages.ListingFragment;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents the Seam components listing on explorer.
 *
 * @since 11.1
 */
public class SeamListingFragment extends ListingFragment {

    public SeamListingFragment(WebDriver driver) {
        super(driver);
    }

    public WebElement getListingItemScope(WebElement item) {
        return item.findElement(By.xpath(".//span[contains(@class, 'scope')]"));
    }

    public List<WebElement> getListingItemClasses(WebElement item) {
        return item.findElements(By.xpath(".//a[@class='javadoc']"));
    }

    @Override
    public SeamListingFragment filterOn(String filterText) {
        super.filterOn(filterText);
        return asPage(SeamListingFragment.class);
    }

    public void checkListing(int expectedSize, String firstLinkText, String firstLinkURLEnd, String firstItemScope,
            List<String> firstClasses, List<String> firstJavadocURLEnds) {
        if (expectedSize >= 0) {
            assertEquals(expectedSize, getItems().size());
        } else {
            assertTrue(getItems().size() > 0);
        }
        WebElement item = getFirstItem();
        WebElement link = getListingItemLink(item);
        assertEquals(firstLinkText, link.getText());
        String href = link.getAttribute("href");
        assertTrue("Actual href: " + href, href.endsWith(firstLinkURLEnd));
        assertEquals(firstItemScope, getListingItemScope(item).getText());
        List<WebElement> classElements = getListingItemClasses(item);
        assertEquals(firstClasses.size(), classElements.size());
        assertEquals(firstJavadocURLEnds.size(), classElements.size());
        for (int i = 0; i < classElements.size(); i++) {
            assertEquals(firstClasses.get(i), classElements.get(i).getText());
            String jhref = classElements.get(i).getAttribute("href");
            assertTrue("Actual href: " + jhref, jhref.endsWith(firstJavadocURLEnds.get(i)));
        }
    }

}
