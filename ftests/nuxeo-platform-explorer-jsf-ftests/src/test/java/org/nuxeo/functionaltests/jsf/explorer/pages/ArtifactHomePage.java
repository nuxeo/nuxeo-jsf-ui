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

import org.nuxeo.functionaltests.explorer.pages.AbstractExplorerPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page representing home for a given artifact type.
 *
 * @since 11.1
 */
public class ArtifactHomePage extends AbstractExplorerPage {

    // don't require it to reuse it in admin center pages, where tabs are not included
    @FindBy(linkText = "Seam Components")
    public WebElement seamComponents;

    public ArtifactHomePage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void check() {
        // NOOP
    }

    public WebElement getFirstListingElement() {
        return driver.findElement(By.xpath("//div[@class='tabscontent']//table/tbody//tr"));
    }

    public boolean isSelected(WebElement element) {
        WebElement parent = element.findElement(By.xpath("./.."));
        return "selected".equals(parent.getAttribute("class"));
    }

}
