/* CanonicalizerTest
 * 
 * Created on Oct 7, 2004
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
package org.archive.crawler.url;

import java.io.File;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.crawler.url.canonicalize.FixupQueryStr;
import org.archive.crawler.url.canonicalize.LowercaseRule;
import org.archive.crawler.url.canonicalize.StripSessionIDs;
import org.archive.crawler.url.canonicalize.StripUserinfoRule;
import org.archive.crawler.url.canonicalize.StripWWWRule;
import org.archive.net.UURIFactory;
import org.archive.util.TmpDirTestCase;

/**
 * Test canonicalization.
 * @author stack
 * @version $Date: 2006-09-26 20:38:48 +0000 (Tue, 26 Sep 2006) $, $Revision: 4667 $
 */
public class CanonicalizerTest extends TmpDirTestCase {
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
        this.rules.addElement(null, new LowercaseRule("lowercase"));
        this.rules.addElement(null, new StripUserinfoRule("userinfo"));
        this.rules.addElement(null, new StripWWWRule("www"));
        this.rules.addElement(null, new StripSessionIDs("ids"));
        this.rules.addElement(null, new FixupQueryStr("querystr"));
    }
    
    public void testCanonicalize() throws URIException {
        final String scheme = "http://";
        final String nonQueryStr = "archive.org/index.html";
        final String result = scheme + nonQueryStr;
        assertTrue("Mangled original", result.equals(
            Canonicalizer.canonicalize(UURIFactory.getInstance(result),
                this.rules.iterator(UURIFactory.getInstance(result)))));
        String tmp = scheme + "www." + nonQueryStr;
        assertTrue("Mangled www", result.equals(
            Canonicalizer.canonicalize(UURIFactory.getInstance(tmp),
                this.rules.iterator(UURIFactory.getInstance(result)))));
        tmp = scheme + "www." + nonQueryStr +
            "?jsessionid=01234567890123456789012345678901";
        assertTrue("Mangled sessionid", result.equals(
            Canonicalizer.canonicalize(UURIFactory.getInstance(tmp),
                this.rules.iterator(UURIFactory.getInstance(result)))));
        tmp = scheme + "www." + nonQueryStr +
            "?jsessionid=01234567890123456789012345678901";
        assertTrue("Mangled sessionid", result.equals(
             Canonicalizer.canonicalize(UURIFactory.getInstance(tmp),
                   this.rules.iterator(UURIFactory.getInstance(result)))));       
    }
}
