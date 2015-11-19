/* StripUserinfoRuleTest
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

import org.apache.commons.httpclient.URIException;
import org.archive.net.UURIFactory;

import junit.framework.TestCase;

/**
 * Test stripping of userinfo from an url.
 * @author stack
 * @version $Date: 2005-07-18 17:30:21 +0000 (Mon, 18 Jul 2005) $, $Revision: 3704 $
 */
public class StripUserinfoRuleTest extends TestCase {
    public void testCanonicalize() throws URIException {
        String url = "http://WWW.aRchive.Org/index.html";
        final String expectedResult = url;
        String result = (new StripUserinfoRule("test")).
            canonicalize(url, UURIFactory.getInstance(url));
        assertTrue("Mangled no userinfo " + result,
            url.equals(result));
        url = "http://stack:password@WWW.aRchive.Org/index.html";
        result = (new StripUserinfoRule("test")).
            canonicalize(url, UURIFactory.getInstance(url));
        assertTrue("Didn't strip userinfo " + result,
            expectedResult.equals(result));
        url = "http://stack:pass@@@@@@word@WWW.aRchive.Org/index.html";
        result = (new StripUserinfoRule("test")).
            canonicalize(url, 
                UURIFactory.getInstance("http://archive.org"));
        assertTrue("Didn't get to last @ " + result,
            expectedResult.equals(result));
        url = "ftp://stack:pass@@@@@@word@archive.org/index.html";
        result = (new StripUserinfoRule("test")).
            canonicalize(url,
                UURIFactory.getInstance("http://archive.org"));
        assertTrue("Didn't get to last @ " + result,
            "ftp://archive.org/index.html".equals(result));
    }
}
