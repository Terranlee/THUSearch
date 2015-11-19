/* QueueTestBase
 *
 * $Id: QueueTestBase.java 4645 2006-09-22 16:08:03Z paul_jack $
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

import java.util.NoSuchElementException;

import org.archive.util.TmpDirTestCase;

/**
 * JUnit test suite for Queue.  It's an abstract class which is implemented by
 * each queue implementation
 *
 * @author <a href="mailto:me@jamesc.net">James Casey</a>
 * @version $Id: QueueTestBase.java 4645 2006-09-22 16:08:03Z paul_jack $
 */
public abstract class QueueTestBase extends TmpDirTestCase {
    /**
     * Create a new PaddingStringBufferTest object
     *
     * @param testName the name of the test
     */
    public QueueTestBase(final String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        queue = makeQueue();
    }

    public void tearDown() {
        if(queue != null) {
            queue.release();
        }
    }

    /**
     * The abstract subclass constructor.  The subclass should create an
     * instance of the object it wishes to have tested
     *
     * @return the Queue object to be tested
     */
    protected abstract Queue<Object> makeQueue();

    /*
     * test methods
     */

    /** test that queue puts things on, and they stay there :) */
    public void testQueue() {
        assertEquals("no items in new queue", 0, queue.length());
        assertTrue("queue is empty", queue.isEmpty());
        queue.enqueue("foo");
        assertEquals("now one item in queue", 1, queue.length());
        assertFalse("queue not empty", queue.isEmpty());
    }

    /** test that dequeue works */
    public void testDequeue() {
        assertEquals("no items in new queue", 0, queue.length());
        assertTrue("queue is empty", queue.isEmpty());
        queue.enqueue("foo");
        queue.enqueue("bar");
        queue.enqueue("baz");
        assertEquals("now three items in queue", 3, queue.length());
        assertEquals("foo dequeued", "foo", queue.dequeue());
        assertEquals("bar dequeued", "bar", queue.dequeue());
        assertEquals("baz dequeued", "baz", queue.dequeue());

        assertEquals("no items in new queue", 0, queue.length());
        assertTrue("queue is empty", queue.isEmpty());

    }

    /** check what happens we dequeue on empty */
    public void testDequeueEmptyQueue() {
        assertTrue("queue is empty", queue.isEmpty());

        try {
            queue.dequeue();
        } catch (NoSuchElementException e) {
            return;
        }
        fail("Expected a NoSuchElementException on dequeue of empty queue");
    }
    /*
     * member variables
     */

    /** the queue object to be tested */
    protected Queue<Object> queue;
}