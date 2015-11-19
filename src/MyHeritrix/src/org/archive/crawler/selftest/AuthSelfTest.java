/* AuthSelfTest
 *
 * Created on Feb 17, 2004
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
 * Test authentications, both basic/digest auth and html form logins.
 *
 * @author stack
 * @version $Id: AuthSelfTest.java 4931 2007-02-21 18:48:17Z gojomo $
 */
public class AuthSelfTest
    extends SelfTestCase
{
    private static final File BASIC = new File("basic");
    private static final File FORM = new File("form");
    private static final File GET = new File(FORM, "get");
    private static final File POST = new File(FORM, "post");

    /**
     * Files to find as a list.
     */
    private static final List<File> FILES_TO_FIND =
        Arrays.asList(new File[] {
                BASIC,
                new File(BASIC, "basic-loggedin.html"),
                FORM,
                new File(POST, "success.jsp"),
                new File(POST, "post-loggedin.html"),
                new File(GET, "success.jsp"),
                new File(GET, "get-loggedin.html")
        });


    /**
     * Test the max-link-hops setting is being respected.
     */
    public void stestAuth() {
        assertInitialized();
        testFilesInArc(FILES_TO_FIND);
    }
}

