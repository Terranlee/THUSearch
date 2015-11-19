/*
 * TestDomianScope
 *
 * $Id: DomainScopeTest.java 4651 2006-09-25 18:31:13Z paul_jack $
 *
 * Created on May 17, 2004
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

package org.archive.crawler.scope;

import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.httpclient.URIException;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;

/**
 * Test the domain scope focus filter.
 *
 * @author Igor Ranitovic
 */
public class DomainScopeTest extends TestCase {

    private ArrayList<UURI> testSeeds;
    private ArrayList<UURI> urlsInScope;
    private ArrayList<UURI> urlsOutOfScope;

    private TestUnitDomainScope dc;

    /**
     * Since testing only focus filter overwrite all other filter to return
     * false.
     *
     * Also override seedsIterator so the test seeds are used.
     */
    @SuppressWarnings("deprecation")
    private class TestUnitDomainScope extends DomainScope {

        private static final long serialVersionUID = 2509499903112690451L;

        public TestUnitDomainScope(String name) {
            super(name);
        }
        
        /* Force test seeds to be used. 
         * @see org.archive.crawler.framework.CrawlScope#seedsIterator()
         */
        public Iterator<UURI> seedsIterator() {
            return testSeeds.iterator();
        }
        
        protected boolean additionalFocusAccepts(Object o) {
            return false;
        }

        protected boolean transitiveAccepts(Object o) {
            return false;
        }

        protected boolean excludeAccepts(Object o) {
            return false;
        }
    }

    public void setUp() throws URIException {
        testSeeds = new ArrayList<UURI>();
        urlsInScope = new ArrayList<UURI>();
        urlsOutOfScope = new ArrayList<UURI>();
        dc = new TestUnitDomainScope("TESTCASE");

        // Add seeds
        addURL(testSeeds, "http://www.a.com/");
        addURL(testSeeds, "http://b.com/");
        addURL(testSeeds, "http://www11.c.com");
        addURL(testSeeds, "http://www.x.y.z.com/index.html");
        addURL(testSeeds, "http://www.1.com/index.html");
        addURL(testSeeds, "http://www.a_b.com/index.html");


        // Add urls in domain scope
        addURL(urlsInScope, "http://www.a.com/");
        addURL(urlsInScope, "http://www1.a.com/");
        addURL(urlsInScope, "http://a.com/");
        addURL(urlsInScope, "http://a.a.com/");

        addURL(urlsInScope, "http://www.b.com/");
        addURL(urlsInScope, "http://www1.b.com/");
        addURL(urlsInScope, "http://b.com/");
        addURL(urlsInScope, "http://b.b.com/");

        addURL(urlsInScope, "http://www.c.com/");
        addURL(urlsInScope, "http://www1.c.com/");
        addURL(urlsInScope, "http://c.com/");
        addURL(urlsInScope, "http://c.c.com/");

        addURL(urlsInScope, "http://www.x.y.z.com/");
        addURL(urlsInScope, "http://www1.x.y.z.com/");
        addURL(urlsInScope, "http://x.y.z.com/");
        addURL(urlsInScope, "http://xyz.x.y.z.com/");
        addURL(urlsInScope, "http://1.com/index.html");
        addURL(urlsInScope, "http://a_b.com/index.html");

        // Add urls out of scope
        addURL(urlsOutOfScope, "http://a.co");
        addURL(urlsOutOfScope, "http://a.comm");
        addURL(urlsOutOfScope, "http://aa.com");
        addURL(urlsOutOfScope, "http://z.com");
        addURL(urlsOutOfScope, "http://y.z.com");
    }

    public void addURL(ArrayList<UURI> list, String url) throws URIException {
        list.add(UURIFactory.getInstance(url));
    }

    public void testInScope() throws URIException {
        for (Iterator i = this.urlsInScope.iterator(); i.hasNext();) {
            Object url = i.next();
            assertTrue("Should be in domain scope: " + url, dc.accepts(url));
        }
    }

    public void testOutOfScope() throws URIException {
        for (Iterator i = this.urlsOutOfScope.iterator(); i.hasNext();) {
            Object url = i.next();
            assertFalse(
                "Should not be in domain scope: " + url,
                dc.accepts(url));
        }
    }
}
