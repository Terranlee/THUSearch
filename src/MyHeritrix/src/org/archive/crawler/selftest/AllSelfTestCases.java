/* AllTests
 *
 * Created on Jan 29, 2004
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.archive.crawler.admin.CrawlJob;


/**
 * All registered heritrix selftests.
 *
 * @author stack
 * @version $Id: AllSelfTestCases.java 4931 2007-02-21 18:48:17Z gojomo $
 */
public class AllSelfTestCases
{
    /**
     * All known selftests as a list.
     *
     * Gets initialized by the static block that immediately follows.
     */
    private static List allKnownSelftests;
    static {
        // List of all known selftests.
        Class [] tmp = {
                BackgroundImageExtractionSelfTestCase.class,
                FramesSelfTestCase.class,
                MaxLinkHopsSelfTest.class,
                CharsetSelfTest.class,
                AuthSelfTest.class,
                BadURIsStopPageParsingSelfTest.class,
                // Works locally but not on builds.archive.org.
                // FlashParseSelfTest.class
                CheckpointSelfTest.class,
            };
        AllSelfTestCases.allKnownSelftests =
            Collections.unmodifiableList(Arrays.asList(tmp));
    }

    /**
     * Run all known tests in the selftest suite.
     *
     * Each unit test to run as part of selftest needs to be added here.
     *
     * @param selftestURL Base url to selftest webapp.
     * @param job The completed selftest job.
     * @param jobDir Job output directory.  Has the seed file, the order file
     * and logs.
     * @param htdocs Expanded webapp directory location.
     *
     * @return Suite of all selftests.
     */
    public static Test suite(final String selftestURL, final CrawlJob job,
            final File jobDir, final File htdocs)
    {
        return suite(selftestURL, job, jobDir, htdocs,
             AllSelfTestCases.allKnownSelftests);
    }

    /**
     * Run list of passed tests.
     *
     * This method is exposed so can run something less than all of the
     * selftests.
     *
     * @param selftestURL Base url to selftest webapp.
     * @param job The completed selftest job.
     * @param jobDir Job output directory.  Has the seed file, the order file
     * and logs.
     * @param htdocs Expanded webapp directory location.
     * @param selftests List of selftests to run.
     *
     * @return Suite of all selftests.
     */
    public static Test suite(final String selftestURL, final CrawlJob job,
            final File jobDir, final File htdocs, final List selftests) {
        TestSuite suite =
            new TestSuite("Test(s) for org.archive.crawler.selftest");
        for (Iterator i = selftests.iterator(); i.hasNext();) {
            suite.addTest(new AltTestSuite((Class)i.next(),"stest"));
        }

        return new TestSetup(suite) {
                protected void setUp() throws Exception {
                    SelfTestCase.initialize(selftestURL, job, jobDir, htdocs);
                }
        };
    }
}
