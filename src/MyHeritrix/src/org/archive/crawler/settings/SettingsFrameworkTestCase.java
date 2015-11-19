/* SettingsFrameworkTestCase
 *
 * $Id: SettingsFrameworkTestCase.java 6082 2008-12-09 02:03:13Z gojomo $
 *
 * Created on Feb 2, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.crawler.settings;

import java.io.File;

import javax.management.Attribute;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.settings.Constraint.FailedCheck;
import org.archive.net.UURIFactory;
import org.archive.util.TmpDirTestCase;

/** Set up a couple of settings to test different functions of the settings
 * framework.
 *
 * @author John Erik Halse
 */
public abstract class SettingsFrameworkTestCase extends TmpDirTestCase implements
        ValueErrorHandler {
    private File orderFile;
    private File settingsDir;
    private CrawlerSettings globalSettings;
    private CrawlerSettings perDomainSettings;
    private CrawlerSettings perHostSettings;
    protected XMLSettingsHandler settingsHandler;
    private CrawlURI unMatchedURI;
    private CrawlURI matchDomainURI;
    private CrawlURI matchHostURI;

    /*
     * @see TmpDirTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        cleanUpOldFiles("SETTINGS"); // preemptive cleanup just in case
        orderFile = new File(getTmpDir(), "SETTINGS_order.xml");
        String settingsDirName = "SETTINGS_per_host_settings";
        settingsDir = new File(orderFile, settingsDirName);
        settingsHandler = new XMLSettingsHandler(orderFile);
        settingsHandler.initialize();
        settingsHandler.getOrder().setAttribute(
          new Attribute(CrawlOrder.ATTR_SETTINGS_DIRECTORY, settingsDirName));

        globalSettings = settingsHandler.getSettingsObject(null);
        perDomainSettings = settingsHandler.getOrCreateSettingsObject("archive.org");
        perHostSettings = settingsHandler.getOrCreateSettingsObject("www.archive.org");

        new ServerCache(getSettingsHandler());

        unMatchedURI = new CrawlURI(
            UURIFactory.getInstance("http://localhost.com/index.html"));

        matchDomainURI = new CrawlURI(
            UURIFactory.getInstance("http://audio.archive.org/index.html"));

        matchHostURI = new CrawlURI(
            UURIFactory.getInstance("http://www.archive.org/index.html"));

        // Write legit email and url so we avoid warnings if tests are reading
        // and writing order files.
        MapType httpHeaders = (MapType)globalSettings.
            getModule(CrawlOrder.ATTR_NAME).
                getAttribute(CrawlOrder.ATTR_HTTP_HEADERS);
        httpHeaders.setAttribute(globalSettings,
            new Attribute(CrawlOrder.ATTR_USER_AGENT,
                "unittest (+http://testing.one.two.three)"));
        httpHeaders.setAttribute(globalSettings,
                new Attribute(CrawlOrder.ATTR_FROM,
                    "unittestingtesting@one.two.three"));
    }

    /*
     * @see TmpDirTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        cleanUpOldFiles("SETTINGS");
    }

    /**
     * @return global settings
     */
    public CrawlerSettings getGlobalSettings() {
        return globalSettings;
    }

    /**
     * @return per domain settings
     */
    public CrawlerSettings getPerDomainSettings() {
        return perDomainSettings;
    }

    /**
     * @return per host settings
     */
    public CrawlerSettings getPerHostSettings() {
        return perHostSettings;
    }

    /**
     * @return settings handler
     */
    public XMLSettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /**
     * @return the order file
     */
    public File getOrderFile() {
        return orderFile;
    }

    /**
     * @return the settings directory
     */
    public File getSettingsDir() {
        return settingsDir;
    }

    /**
     * @return a uri matching the domain settings
     */
    public CrawlURI getMatchDomainURI() {
        return matchDomainURI;
    }

    /**
     * @return a uri matching the per host settings
     */
    public CrawlURI getMatchHostURI() {
        return matchHostURI;
    }

    /**
     * @return a uri that doesn't match any settings object except globals.
     */
    public CrawlURI getUnMatchedURI() {
        return unMatchedURI;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.ValueErrorHandler#handleValueError(org.archive.crawler.settings.Constraint.FailedCheck)
     */
    public void handleValueError(FailedCheck error) {
        // TODO Auto-generated method stub
    }

}
