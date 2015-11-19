/* StripWWWRuleTest
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
 * Test stripping 'www' if present.
 * @author stack
 * @version $Date: 2006-09-18 20:32:47 +0000 (Mon, 18 Sep 2006) $, $Revision: 4634 $
 */
public class StripWWWNRuleTest extends TestCase {

    public void testCanonicalize() throws URIException {
        String url = "http://WWW.aRchive.Org/index.html";
        String expectedResult = "http://aRchive.Org/index.html";
        String result = (new StripWWWNRule("test")).
            canonicalize(url, UURIFactory.getInstance(url));
        assertTrue("Failed " + result, expectedResult.equals(result));
        url = "http://www001.aRchive.Org/index.html";
        result = (new StripWWWNRule("test")).
            canonicalize(url, UURIFactory.getInstance(url));
        assertTrue("Failed " + result, expectedResult.equals(result));
        url = "http://www3.aRchive.Org/index.html";
        result = (new StripWWWNRule("test")).
            canonicalize(url, UURIFactory.getInstance(url));
        assertTrue("Failed " + result, expectedResult.equals(result));
    }
}