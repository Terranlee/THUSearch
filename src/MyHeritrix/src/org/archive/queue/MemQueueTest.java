/* MemQueueTest
 *
 * $Id: MemQueueTest.java 4645 2006-09-22 16:08:03Z paul_jack $
 *
 * Created Tue Jan 20 14:17:59 PST 2004
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

package org.archive.queue;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * JUnit test suite for MemQueue
 *
 * @author <a href="mailto:me@jamesc.net">James Casey</a>
 * @version $ Id$
 */
public class MemQueueTest extends QueueTestBase {
    /**
     * Create a new MemQueueTest object
     *
     * @param testName the name of the test
     */
    public MemQueueTest(final String testName) {
        super(testName);
    }

    /**
     * run all the tests for MemQueueTest
     *
     * @param argv the command line arguments
     */
    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * return the suite of tests for MemQueueTest
     *
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(MemQueueTest.class);
    }

    /*
     * test methods
     */
    protected Queue<Object> makeQueue() {
        return new MemQueue<Object>();
    }

    // TODO - implement test methods in MemQueueTest
}

