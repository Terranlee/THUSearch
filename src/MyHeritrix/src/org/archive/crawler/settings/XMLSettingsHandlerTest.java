/*
 * XMLSettingsHandlerTest
 *
 * $Id: XMLSettingsHandlerTest.java 6326 2009-05-28 01:40:33Z gojomo $
 *
 * Created on Jan 28, 2004
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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.scope.ClassicScope;
import org.archive.crawler.settings.refinements.Criteria;
import org.archive.crawler.settings.refinements.PortnumberCriteria;
import org.archive.crawler.settings.refinements.Refinement;
import org.archive.crawler.settings.refinements.RegularExpressionCriteria;
import org.archive.crawler.settings.refinements.TimespanCriteria;
import org.archive.net.UURIFactory;

/**
 * Tests the handling of settings files.
 *
 * @author John Erik Halse
 *
 */
public class XMLSettingsHandlerTest extends SettingsFrameworkTestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test for void writeSettingsObject(CrawlerSettings)
     */
    public void testWriteSettingsObjectCrawlerSettings()
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {

        // Write a crawl order file
        CrawlerSettings settings = getGlobalSettings();
        XMLSettingsHandler handler = getSettingsHandler();
        handler.registerValueErrorHandler(this);
        handler.getOrder().setAttribute(new ClassicScope());
        handler.writeSettingsObject(settings);
        assertTrue("Order file was not written", getOrderFile().exists());

        // Get a module to alter a setting on
        ComplexType scope = settings.getModule(CrawlScope.ATTR_NAME);
        assertNotNull("Could not get module scope", scope);

        // Alter two settings in a per host file
        CrawlerSettings perHost = getPerHostSettings();
        Integer newHops = new Integer(500);
        String newFrom = "newfrom";
        scope.setAttribute(perHost, new Attribute(
            ClassicScope.ATTR_MAX_LINK_HOPS, newHops));
        CrawlOrder order = handler.getOrder();
        ComplexType httpHeaders = (ComplexType) order
            .getAttribute(CrawlOrder.ATTR_HTTP_HEADERS);
        httpHeaders.setAttribute(perHost, new Attribute(CrawlOrder.ATTR_FROM,
                newFrom));

        // Write the per host file
        handler.writeSettingsObject(perHost);
        assertTrue("Per host file was not written", handler.settingsToFilename(
                perHost).exists());

        // Create a new handler for testing that changes was written to disk
        XMLSettingsHandler newHandler = new XMLSettingsHandler(getOrderFile());
        newHandler.initialize();

        // Read perHost
        CrawlerSettings newPerHost = newHandler.getSettingsObject(perHost
                .getScope());
        assertNotNull("Per host scope could not be read", newPerHost);

        ComplexType newScope = newHandler.getModule(CrawlScope.ATTR_NAME);
        assertNotNull(newScope);
        Integer r1 = (Integer) newScope.getAttribute(newPerHost,
            ClassicScope.ATTR_MAX_LINK_HOPS);
        assertEquals(newHops, r1);

        ComplexType newHttpHeaders = (ComplexType) newHandler.getOrder()
                .getAttribute(newPerHost, CrawlOrder.ATTR_HTTP_HEADERS);
        assertNotNull(newHttpHeaders);

        String r2 = (String) newHttpHeaders.getAttribute(newPerHost,
                CrawlOrder.ATTR_FROM);
        assertEquals(newFrom, r2);
    }

    /**
     * Test the copying of the entire settings directory.
     *
     * @throws IOException
     */
    public void testCopySettings() throws IOException {
        //String testScope = "www.archive.org";

        // Write the files
        XMLSettingsHandler handler = getSettingsHandler();
        handler.writeSettingsObject(getGlobalSettings());
        handler.writeSettingsObject(getPerHostSettings());

        // Copy to new location
        File newOrderFile = new File(getTmpDir(), "SETTINGS_new_order.xml");
        String newSettingsDir = "SETTINGS_new_per_host_settings";
        handler.copySettings(newOrderFile, newSettingsDir);

        // Check if new files where created.
        assertTrue("Order file was not written", newOrderFile.exists());

        assertTrue("New settings dir not set", handler.settingsToFilename(
                getPerHostSettings()).getAbsolutePath().matches(
                ".*" + newSettingsDir + ".*"));
        assertTrue("Per host file was not written", handler.settingsToFilename(
                getPerHostSettings()).exists());
    }

    public void testGetSettings() {
        XMLSettingsHandler handler = getSettingsHandler();
        CrawlerSettings order = handler.getSettingsObject(null);
        CrawlerSettings perHost = handler.getSettings("localhost.localdomain");
        assertNotNull("Didn't get any file", perHost);
        assertSame("Did not get same file", order, perHost);
    }

    public void testGetSettingsObject() {
        String testScope = "audio.archive.org";

        XMLSettingsHandler handler = getSettingsHandler();
        assertNotNull("Couldn't get orderfile", handler.getSettingsObject(null));
        assertNull("Got nonexisting per host file", handler
                .getSettingsObject(testScope));
        assertNotNull("Couldn't create per host file", handler
                .getOrCreateSettingsObject(testScope));
        assertNotNull("Couldn't get per host file", handler
                .getSettingsObject(testScope));
    }

    public void testDeleteSettingsObject() {
        XMLSettingsHandler handler = getSettingsHandler();
        File file = handler.settingsToFilename(getPerHostSettings());
        handler.writeSettingsObject(getPerHostSettings());
        assertTrue("Per host file was not written", file.exists());
        handler.deleteSettingsObject(getPerHostSettings());
        assertFalse("Per host file was not deleted", file.exists());
    }

    public void testReadWriteRefinements() throws ParseException,
            InvalidAttributeValueException, AttributeNotFoundException,
            MBeanException, ReflectionException, URIException {
        XMLSettingsHandler handler = getSettingsHandler();
        CrawlerSettings global = getGlobalSettings();
        CrawlerSettings per = getPerHostSettings();
        ComplexType headers = (ComplexType) handler.getOrder().getAttribute(
                CrawlOrder.ATTR_HTTP_HEADERS);

        String globalFrom = (String) headers.getAttribute(CrawlOrder.ATTR_FROM);
        String refinedGlobalFrom = "refined@global.address";
        String refinedPerFrom = "refined@per.address";

        // Create a refinement on the global level
        Refinement globalRefinement = new Refinement(global, "test",
                "Refinement test");
        Criteria timespanCriteria = new TimespanCriteria("2300", "2300");
        globalRefinement.addCriteria(timespanCriteria);
        Criteria regexpCriteria = new RegularExpressionCriteria(".*www.*");
        globalRefinement.addCriteria(regexpCriteria);
        handler.writeSettingsObject(global);

        // Override an attribute on the global refinement
        CrawlerSettings globalRefinementSetting = globalRefinement
                .getSettings();
        headers.setAttribute(globalRefinementSetting, new Attribute(
                CrawlOrder.ATTR_FROM, refinedGlobalFrom));
        handler.writeSettingsObject(globalRefinementSetting);

        // Create a refinement on a per level
        Refinement perRefinement = new Refinement(per, "test2",
                "Refinement test2");
        Criteria portCriteria = new PortnumberCriteria("10");
        perRefinement.addCriteria(portCriteria);
        handler.writeSettingsObject(per);

        // Override an attribute on the per refinement
        CrawlerSettings perRefinementSetting = perRefinement.getSettings();
        headers.setAttribute(perRefinementSetting, new Attribute(
                CrawlOrder.ATTR_FROM, refinedPerFrom));
        handler.writeSettingsObject(perRefinementSetting);

        // Create a new handler for testing that changes was written to disk
        XMLSettingsHandler newHandler = new XMLSettingsHandler(getOrderFile());
        newHandler.initialize();
        CrawlerSettings newGlobal = newHandler.getSettingsObject(null);
        assertNotNull("Global scope could not be read", newGlobal);
        CrawlerSettings newPer = newHandler.getSettingsObject(per.getScope());
        assertNotNull("Per host scope could not be read", newPer);

        ComplexType newHeaders = (ComplexType) newHandler.getOrder()
                .getAttribute(CrawlOrder.ATTR_HTTP_HEADERS);
        assertNotNull(newHeaders);

        String newFrom1 = (String) newHeaders.getAttribute(
                CrawlOrder.ATTR_FROM, getMatchDomainURI());
        String newFrom2 = (String) newHeaders.getAttribute(
                CrawlOrder.ATTR_FROM, getMatchHostURI());
        CrawlURI matchHostAndPortURI = new CrawlURI(
            UURIFactory.getInstance("http://www.archive.org:10/index.html"));
        String newFrom3 = (String) newHeaders.getAttribute(
                CrawlOrder.ATTR_FROM, matchHostAndPortURI);

        //Check that we got what we expected
        assertEquals(globalFrom, newFrom1);
        assertEquals(refinedGlobalFrom, newFrom2);
        assertEquals(refinedPerFrom, newFrom3);
    }
    
    public void testToResourcePath() {
        assertTrue(
            XMLSettingsHandler.toResourcePath(new File("/usr/local/bin"))
            .startsWith("/usr/local/bin"));
        assertTrue(
            XMLSettingsHandler.toResourcePath(new File("/home/user1/Test.java"))
            .startsWith("/home/user1/Test.java"));
        if(File.separatorChar=='\\') {
            // run these only on relevant platform (Windows)
            assertTrue(
                XMLSettingsHandler.toResourcePath(new File("C:\\Windows\\System32"))
                .startsWith("/Windows/System32"));
            assertTrue(
                XMLSettingsHandler.toResourcePath(new File("Z:\\some.dir\\another.dir\\some.file.ext"))
                .startsWith("/some.dir/another.dir/some.file.ext"));
        }
    }
}