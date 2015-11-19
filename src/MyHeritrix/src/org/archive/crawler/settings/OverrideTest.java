/* OverrideTest
 *
 * $Id: OverrideTest.java 2168 2004-05-28 22:33:09Z stack-sf $
 *
 * Created on Feb 20, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or any later version.
 *
 * Heritrix is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along with
 * Heritrix; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.archive.crawler.settings;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.Processor;

/**
 * Test the concept of overrides.
 *
 * As this test is testing a concept, it involves more than one class to be
 * tested. Thus the name of this test doesn't match a class name.
 *
 * @author John Erik Halse
 *
 */
public class OverrideTest extends SettingsFrameworkTestCase {

    /*
     * @see SettingsFrameworkTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see SettingsFrameworkTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testOverridingOfGlobalAttribute()
            throws AttributeNotFoundException, MBeanException,
            ReflectionException, InvalidAttributeValueException {

        final String MODULE_NAME = "module1";
        ModuleType module1 = new ModuleType(MODULE_NAME);
        ModuleType module2 = new Processor(MODULE_NAME, "Descr");

        // Set up override
        MapType headers = (MapType) getSettingsHandler().getOrder()
                .getAttribute(CrawlOrder.ATTR_HTTP_HEADERS);
        headers.addElement(getGlobalSettings(), module1);
        headers.setAttribute(getPerDomainSettings(), module2);

        // Read back values to see if we get the right ones
        ModuleType getMod;
        getMod = (ModuleType) headers.getAttribute(getGlobalSettings(),
                MODULE_NAME);
        assertSame("Wrong global value", module1, getMod);
        assertEquals("Wrong class type", module1.getClass().getName(), headers
                .getAttributeInfo(getGlobalSettings(), MODULE_NAME).getType());

        getMod = (ModuleType) headers.getAttribute(getPerDomainSettings(),
                MODULE_NAME);
        assertSame("Wrong domain value", module2, getMod);
        assertEquals("Wrong class type", module2.getClass().getName(), headers
                .getAttributeInfo(getPerDomainSettings(), MODULE_NAME)
                .getType());

        getMod = (ModuleType) headers.getAttribute(getPerHostSettings(),
                MODULE_NAME);
        assertSame("Wrong host value", module2, getMod);
        assertEquals("Wrong class type", module2.getClass().getName(), headers
                .getAttributeInfo(getPerHostSettings(), MODULE_NAME).getType());
    }

    public void testOverridingOfNonGlobalAttribute()
            throws AttributeNotFoundException, MBeanException,
            ReflectionException, InvalidAttributeValueException {
        final String MODULE_NAME = "module1";
        ModuleType module1 = new ModuleType(MODULE_NAME);
        ModuleType module2 = new Processor(MODULE_NAME, "Descr");

        // Set up override
        MapType headers = (MapType) getSettingsHandler().getOrder()
                .getAttribute(CrawlOrder.ATTR_HTTP_HEADERS);
        headers.addElement(getPerDomainSettings(), module1);
        headers.setAttribute(getPerHostSettings(), module2);

        // Read back values to see if we get the right ones
        ModuleType getMod;
        try {
            getMod = (ModuleType) headers.getAttribute(getGlobalSettings(),
                    MODULE_NAME);
            fail("Global value should not exist");
        } catch (AttributeNotFoundException e) {
            // OK! this should throw an exception;
        }

        getMod = (ModuleType) headers.getAttribute(getPerDomainSettings(),
                MODULE_NAME);
        assertSame("Wrong domain value", module1, getMod);
        assertEquals("Wrong class type", module1.getClass().getName(), headers
                .getAttributeInfo(getPerDomainSettings(), MODULE_NAME)
                .getType());

        getMod = (ModuleType) headers.getAttribute(getPerHostSettings(),
                MODULE_NAME);
        assertSame("Wrong host value", module2, getMod);
        assertEquals("Wrong class type", module2.getClass().getName(), headers
                .getAttributeInfo(getPerHostSettings(), MODULE_NAME).getType());
    }

}