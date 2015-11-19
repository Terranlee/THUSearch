/* RegexRuleTest
 * 
 * Created on Oct 6, 2004
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
package org.archive.crawler.url.canonicalize;

import java.io.File;

import javax.management.InvalidAttributeValueException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.net.UURIFactory;
import org.archive.util.TmpDirTestCase;


/**
 * Test the regex rule.
 * @author stack
 * @version $Date: 2005-07-18 17:30:21 +0000 (Mon, 18 Jul 2005) $, $Revision: 3704 $
 */
public class RegexRuleTest extends TmpDirTestCase {
    private File orderFile;
    protected XMLSettingsHandler settingsHandler;
    private MapType rules = null;
    
    protected void setUp() throws Exception {
        super.setUp();
        this.orderFile = new File(getTmpDir(), this.getClass().getName() +
            ".order.xml");
        this.settingsHandler = new XMLSettingsHandler(orderFile);
        this.settingsHandler.initialize();
        this.rules = (MapType)(settingsHandler.getSettingsObject(null)).
            getModule(CrawlOrder.ATTR_NAME).
               getAttribute(CrawlOrder.ATTR_RULES);
    }
    
    public void testCanonicalize()
    throws URIException, InvalidAttributeValueException {
        final String url = "http://www.aRchive.Org/index.html";
        RegexRule rr = new RegexRule("Test " + this.getClass().getName());
        this.rules.addElement(null, rr);
        rr.canonicalize(url, UURIFactory.getInstance(url));
        String product = rr.canonicalize(url, null);
        assertTrue("Default doesn't work.",  url.equals(product));
    }

    public void testSessionid()
    throws InvalidAttributeValueException {
        final String urlBase = "http://joann.com/catalog.jhtml";
        final String urlMinusSessionid = urlBase + "?CATID=96029";
        final String url = urlBase +
		    ";$sessionid$JKOFFNYAAKUTIP4SY5NBHOR50LD3OEPO?CATID=96029";
        RegexRule rr = new RegexRule("Test",
            "^(.+)(?:;\\$sessionid\\$[A-Z0-9]{32})(\\?.*)+$",
        	"$1$2");
        this.rules.addElement(null, rr);
        String product = rr.canonicalize(url, null);
        assertTrue("Failed " + url, urlMinusSessionid.equals(product));
    }
    
    public void testNullFormat()
    throws InvalidAttributeValueException {
        final String urlBase = "http://joann.com/catalog.jhtml";
        final String url = urlBase +
            ";$sessionid$JKOFFNYAAKUTIP4SY5NBHOR50LD3OEPO";
        RegexRule rr = new RegexRule("Test",
            "^(.+)(?:;\\$sessionid\\$[A-Z0-9]{32})$",
            "$1$2");
        this.rules.addElement(null, rr);
        String product = rr.canonicalize(url, null);
        assertTrue("Failed " + url, urlBase.equals(product));
    }
}
