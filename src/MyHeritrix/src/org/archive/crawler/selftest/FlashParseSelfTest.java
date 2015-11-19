/* FlashParseSelfTest
 * 
 * $Id: FlashParseSelfTest.java 4931 2007-02-21 18:48:17Z gojomo $
 *
 * Created on Mar 10, 2004
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
package org.archive.crawler.selftest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Simple selftest for flash extractor.
 *
 * @author stack
 */
public class FlashParseSelfTest extends SelfTestCase
{
    /**
     * Files to find as a list.
     */
    private static final List<File> FILES_TO_FIND =
        Arrays.asList(new File[]
            {new File("success.html")});

    public void stestFilesFound() {
        assertInitialized();
        testFilesInArc(FILES_TO_FIND);
    }
}
