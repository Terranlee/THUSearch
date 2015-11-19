/* PaddingStringBufferTest
 *
 * $Id: PaddingStringBufferTest.java 4644 2006-09-20 22:40:21Z paul_jack $
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

package org.archive.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test suite for PaddingStringBuffer
 *
 * @author <a href="mailto:me@jamesc.net">James Casey</a>
 * @version $Id: PaddingStringBufferTest.java 4644 2006-09-20 22:40:21Z paul_jack $
 */
public class PaddingStringBufferTest extends TestCase {
    /**
     * Create a new PaddingStringBufferTest object
     *
     * @param testName the name of the test
     */
    public PaddingStringBufferTest(final String testName) {
        super(testName);
    }

    /**
     * run all the tests for PaddingStringBufferTest
     *
     * @param argv the command line arguments
     */
    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * return the suite of tests for PaddingStringBufferTest
     *
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(PaddingStringBufferTest.class);
    }

    public void setUp() {
        buf = new PaddingStringBuffer();
    }

    /** first check that padTo works ok, since all depends on it */
    public void testPadTo() {
        PaddingStringBuffer retBuf;
        assertEquals("nothing in buffer", "", buf.toString());
        retBuf = buf.padTo(5);
        assertEquals("retBuf same as buf", retBuf, buf);
        assertEquals("5 spaces", "     ", buf.toString());

        // now do a smaller value - nothing should happen
        buf.padTo(4);
        assertEquals("5 spaces", "     ", buf.toString());

        // now pad tro a greater length
        buf.padTo(10);
        assertEquals("10 spaces", "          ", buf.toString());
    }

    /** test that append(String) works correctly */
    public void testAppendString() {
        // a buf to hold the return buffer
        PaddingStringBuffer retBuf;
        assertEquals("nothing in buffer", "", buf.toString());
        retBuf = buf.append("foo");
        assertEquals("foo in buffer", "foo", buf.toString());
        assertEquals("retBuf good", retBuf.toString(), buf.toString());
        retBuf = buf.append("bar");
        assertEquals("foobar in buffer", "foobar", buf.toString());
        assertEquals("retBuf good", retBuf.toString(), buf.toString());
    }

    /** check the reset method clears the buffer */
    public void testReset() {
        // append something into the buffer
        assertEquals("nothing in buffer", "", buf.toString());
        buf.append("foo");
        assertEquals("buffer is 'foo'", "foo", buf.toString());
        buf.reset();
        assertEquals("nothing in buffer after reset", "", buf.toString());
    }

    /** test the raAppend(String) works in the simple cases */
    public void testRaAppend() {
        // a buf to hold the return buffer
        PaddingStringBuffer retBuf;
        assertEquals("nothing in buffer", "", buf.toString());
        retBuf = buf.raAppend(5, "foo");
        assertEquals("foo in buffer", "  foo", buf.toString());
        assertEquals("retBuf good", retBuf.toString(), buf.toString());
        retBuf = buf.raAppend(9, "bar");
        assertEquals("foobar in buffer", "  foo bar", buf.toString());
        assertEquals("retBuf good", retBuf.toString(), buf.toString());

        // now check with out-of-range columns - should just append
        buf = new PaddingStringBuffer();
        buf.raAppend(-1, "foo");
        assertEquals("no padding for -1", "foo", buf.toString());
        buf = new PaddingStringBuffer();
        buf.raAppend(0, "foo");
        assertEquals("no padding for 0", "foo", buf.toString());

    }

    /** test the newline() */
    public void testNewline(){
        assertEquals("nothing should be in the buffer", "", buf.toString());
        buf.newline();
        assertTrue("should contain newline", buf.toString().indexOf('\n')!=-1);
        assertEquals("line position should be 0",0,buf.linePos);
    }

    /** check what happens when we right append, but the string is longer
     * than the space */
    public void testRaAppendWithTooLongString() {
        buf.raAppend(3,"foobar");
        assertEquals("no padding when padding col less than string length",
                "foobar", buf.toString());
        buf.reset();
    }

    /** check it all works with the length == the length of the string */
    public void testRaAppendWithExactLengthString() {
        buf.raAppend(6, "foobar");
        buf.raAppend(12, "foobar");
        assertEquals("no padding with exact length string",
                "foobarfoobar", buf.toString());
    }

    /** check that append(int) works */
    public void testAppendInt() {
        buf.append((int)1);
        assertEquals("buffer is '1'", "1", buf.toString());
        buf.append((int)234);
        assertEquals("buffer is '1234'", "1234", buf.toString());
    }

    /** check that raAppend(int) works */
    public void testRaAppendInt() {
        // right-append '1' to column 5
        buf.raAppend(5, (int)1);
        assertEquals("buf is '    1'", "    1", buf.toString());
        // try appending a too-long int

        buf.raAppend(6,(int)123);
        assertEquals("'123' appended", "    1123", buf.toString());
    }

    /** check that  append(long) works */
    public void testAppendLong() {
        buf.append((long)1);
        assertEquals("buffer is '1'", "1", buf.toString());
        buf.append((long)234);
        assertEquals("buffer is '1234'", "1234", buf.toString());
    }

    /** check that raAppend(long) works */
    public void testRaAppendLong() {
        // right-append '1' to column 5
        buf.raAppend(5, (long) 1);
        assertEquals("buf is '    1'", "    1", buf.toString());
        // try appending a too-long int

        buf.raAppend(6, (long) 123);
        assertEquals("'123' appended", "    1123", buf.toString());
    }

    /** a temp buffer for testing with */
    private PaddingStringBuffer buf;
}

