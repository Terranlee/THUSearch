/* MaxLinkHopsSelfTest
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

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.scope.ClassicScope;


/**
 * Test the max-link-hops setting.
 *
 * @author stack
 * @version $Id: MaxLinkHopsSelfTest.java 4931 2007-02-21 18:48:17Z gojomo $
 */
public class MaxLinkHopsSelfTest
    extends SelfTestCase
{
    /**
     * Files to find as a list.
     */
    private static final List<File> FILES_TO_FIND =
        Arrays.asList(new File[] {new File("2.html"),
            new File("3.html"), new File("4.html"), new File("5.html")});

    /**
     * Files not to find as a list.
     */
    private static final List FILES_NOT_TO_FIND =
        Arrays.asList(new File[] {new File("1.html"), new File("6.html")});

    /**
     * Assumption is that the setting for max-link-hops is less than this
     * number.
     */
    private static final int MAXLINKHOPS = 5;


    /**
     * Test the max-link-hops setting is being respected.
     */
    public void stestMaxLinkHops()
        throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        assertInitialized();
        CrawlScope scope =
           (CrawlScope)getCrawlJob().getSettingsHandler()
           .getModule(CrawlScope.ATTR_NAME);
        int maxLinkHops =
            ((Integer)scope.getAttribute(ClassicScope.ATTR_MAX_LINK_HOPS))
            .intValue();
        assertTrue("max-link-hops incorrect", MAXLINKHOPS == maxLinkHops);

        // Make sure file we're NOT supposed to find is actually on disk.
        assertTrue("File present on disk", filesExist(FILES_NOT_TO_FIND));

        // Ok.  The file not to find exists.  Lets see if it made it into arc.
        testFilesInArc(FILES_TO_FIND);
    }
}

