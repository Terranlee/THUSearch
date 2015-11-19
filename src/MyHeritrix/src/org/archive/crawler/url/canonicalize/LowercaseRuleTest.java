/* LowercaseRuleTest
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
 * Unit test lowercase rule.
 * @author stack
 * @version $Date: 2005-07-18 17:30:21 +0000 (Mon, 18 Jul 2005) $, $Revision: 3704 $
 */
public class LowercaseRuleTest extends TestCase {
    public void testCanonicalize() throws URIException {
        final String url = "http://WWW.aRchive.Org/index.html";
        assertTrue("Didn't lowercase" + url,
            url.toLowerCase().equals((new LowercaseRule("test")).
                canonicalize(url, UURIFactory.getInstance(url))));
    }
}
