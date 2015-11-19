/* PathologicalPathFilterTest
 *
 * $Id: PathologicalPathFilterTest.java 4652 2006-09-25 18:41:10Z paul_jack $
 *
 * Created on Feb 23, 2004
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
package org.archive.crawler.filter;

import junit.framework.TestCase;

/**
 * @author John Erik Halse
 * @deprecated  The tested class is deprecated, so this test will
 * eventually go away.
 */
public class PathologicalPathFilterTest extends TestCase {
    public final void testAccepts() {
        PathologicalPathFilter filter = new PathologicalPathFilter("Filter");

        String uri = "http://www.archive.org/";
        assertFalse(filter.accepts(uri));

        uri = "http://www.archive.org/img/img/img.gif";
        assertFalse(filter.accepts(uri));

        uri = "http://www.archive.org/img/img/img/img.gif";
        assertTrue(filter.accepts(uri));

        uri = "http://www.archive.org/img/doc/img/doc/img/doc/img.gif";
        assertTrue(filter.accepts(uri));
    }
}
