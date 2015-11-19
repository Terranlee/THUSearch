/* FixupQueryStrTest
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
 * Test we strip trailing question mark.
 * @author stack
 * @version $Date: 2006-09-01 22:44:50 +0000 (Fri, 01 Sep 2006) $, $Revision: 4591 $
 */
public class FixupQueryStrTest extends TestCase {

    public void testCanonicalize() throws URIException {
        final String url = "http://WWW.aRchive.Org/index.html";
        assertTrue("Mangled " + url,
            url.equals((new FixupQueryStr("test")).
                canonicalize(url, UURIFactory.getInstance(url))));
        assertTrue("Failed to strip '?' " + url,
            url.equals((new FixupQueryStr("test")).
                canonicalize(url + "?", UURIFactory.getInstance(url))));
        assertTrue("Failed to strip '?&' " + url,
            url.equals((new FixupQueryStr("test")).
                canonicalize(url + "?&", UURIFactory.getInstance(url))));
        assertTrue("Failed to strip extraneous '&' " + url,
            (url + "?x=y").equals((new FixupQueryStr("test")).
                canonicalize(url + "?&x=y", UURIFactory.getInstance(url))));
        String tmp = url + "?x=y";
        assertTrue("Mangled x=y " + tmp,
            tmp.equals((new FixupQueryStr("test")).
                canonicalize(tmp, UURIFactory.getInstance(url))));
        String tmp2 = tmp + "&";
        String fixed = new FixupQueryStr("test").
            canonicalize(tmp2, UURIFactory.getInstance(url));
        assertTrue("Mangled " + tmp2, tmp.equals(fixed));
    }
}
