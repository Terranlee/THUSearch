/* FPUriUniqFilterTest
 *
 * $Id: BloomUriUniqFilterTest.java 4647 2006-09-22 18:39:39Z paul_jack $
 *
 * Created on Sep 15, 2004.
 *
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;


/**
 * Test BloomUriUniqFilter.
 * @author gojomo
 */
public class BloomUriUniqFilterTest extends TestCase
implements UriUniqFilter.HasUriReceiver {
    private Logger logger =
        Logger.getLogger(BloomUriUniqFilterTest.class.getName());

    private BloomUriUniqFilter filter = null;

    /**
     * Set to true if we visited received.
     */
    private boolean received = false;

    protected void setUp() throws Exception {
        super.setUp();
        this.filter = new BloomUriUniqFilter(2000,24);
        this.filter.setDestination(this);
    }

    public void testAdding() throws URIException {
        this.filter.add(this.getUri(),
            new CandidateURI(UURIFactory.getInstance(this.getUri())));
        this.filter.addNow(this.getUri(),
            new CandidateURI(UURIFactory.getInstance(this.getUri())));
        this.filter.addForce(this.getUri(),
            new CandidateURI(UURIFactory.getInstance(this.getUri())));
        // Should only have add 'this' once.
        assertTrue("Count is off", this.filter.count() == 1);
    }

    /**
     * Test inserting.
     * @throws URIException
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void testWriting() throws URIException {
        long start = System.currentTimeMillis();
        ArrayList<UURI> list = new ArrayList<UURI>(1000);
        int count = 0;
        final int MAX_COUNT = 1000;
        for (; count < MAX_COUNT; count++) {
            assertEquals("count off",count,filter.count());
            UURI u = UURIFactory.getInstance("http://www" +
                    count + ".archive.org/" + count + "/index.html");
            assertFalse("already contained "+u.toString(),filter.bloom.contains(u.toString()));
            logger.fine("adding "+u.toString());
            filter.add(u.toString(), new CandidateURI(u));
            assertTrue("not in bloom",filter.bloom.contains(u.toString()));
            if (count > 0 && ((count % 100) == 0)) {
                list.add(u);
            }
        }
        logger.fine("Added " + count + " in " +
                (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (Iterator i = list.iterator(); i.hasNext();) {
            UURI uuri = (UURI)i.next();
            filter.add(uuri.toString(), new CandidateURI(uuri));
        }
        logger.fine("Readded subset " + list.size() + " in " +
                (System.currentTimeMillis() - start));

        assertTrue("Count is off: " + filter.count(),
            filter.count() == MAX_COUNT);
    }

    public void testNote() {
        filter.note(this.getUri());
        assertFalse("Receiver was called", this.received);
    }

// FORGET CURRENTLY UNSUPPORTED IN BloomUriUniqFilter
//    public void testForget() throws URIException {
//        this.filter.forget(this.getUri(),
//                new CandidateURI(UURIFactory.getInstance(this.getUri())));
//        assertTrue("Didn't forget", this.filter.count() == 0);
//    }

    public void receive(CandidateURI item) {
        this.received = true;
    }

    public String getUri() {
        return "http://www.archive.org";
    }
}
