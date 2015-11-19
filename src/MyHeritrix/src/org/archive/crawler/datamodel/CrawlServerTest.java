/* 
 * Copyright (C) 2008 Internet Archive.
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
 *
 * CrawlServerTest.java
 *
 * Created on May 21, 2008
 *
 * $Id:$
 */
package org.archive.crawler.datamodel;

import junit.framework.TestCase;

/**
 * CrawlServer class unit tests. 
 */
public class CrawlServerTest extends TestCase {


    public void testGetServerKey() throws Exception {
        CandidateURI cauri = CandidateURI.fromString("https://www.example.com");
        assertEquals(
                "bad https key",
                "www.example.com:443",
                CrawlServer.getServerKey(cauri));
    }
    
}
