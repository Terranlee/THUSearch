/* SelftestCrawlJobHandler
 *
 * Created on Feb 4, 2004
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestResult;

import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlURIDispositionListener;


/**
 * An override to gain access to end-of-crawljob message.
 *
 *
 * @author stack
 * @version $Id: SelfTestCrawlJobHandler.java 4667 2006-09-26 20:38:48Z paul_jack $
 */

public class SelfTestCrawlJobHandler extends CrawlJobHandler
implements CrawlURIDispositionListener {
    /**
     * Name of the selftest webapp.
     */
    private static final String SELFTEST_WEBAPP = "selftest";

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.admin.SelftestCrawlJobHandler");

    /**
     * Name of selftest to run.
     *
     * If set, run this test only.  Otherwise run them all.
     */
    private String selfTestName = null;
    
    private String selfTestUrl = null;


    private SelfTestCrawlJobHandler() {
        this(null, null, null);
    }

    public SelfTestCrawlJobHandler(final File jobsDir,
            final String selfTestName, final String url) {
        // No need to load jobs or profiles
        super(jobsDir, false, false);
        this.selfTestName = selfTestName;
        this.selfTestUrl = url;
    }
    
    @Override
    public void crawlStarted(String message) {
    	super.crawlStarted(message);
    	this.getCurrentJob().getController().
    		addCrawlURIDispositionListener(this);
    }

    public void crawlEnded(String sExitMessage)  {
        TestResult result = null;
        try {
            super.crawlEnded(sExitMessage);

            // At crawlEnded time, there is no current job.  Get the selftest
            // job by pulling from the completedCrawlJobs queue.
            List completedCrawlJobs = getCompletedJobs();
            if (completedCrawlJobs == null || completedCrawlJobs.size() <= 0) {
                logger.severe("Selftest job did not complete.");
            } else {
                CrawlJob job = (CrawlJob)completedCrawlJobs.
                    get(completedCrawlJobs.size()-1);
                Test test = null;
                if (this.selfTestName != null &&
                        this.selfTestName.length() > 0) {
                    // Run single selftest only.
                    // Get class for the passed single selftest.
                    // Assume test to run is in this package.
                    String thisClassName = this.getClass().getName();
                    String pkg = thisClassName.
                        substring(0, thisClassName.lastIndexOf('.'));
                    // All selftests end in 'SelfTest'.
                    String selftestClass = pkg + '.' + this.selfTestName +
                        "SelfTest";
                    // Need to make a list.  Make an array first.
                    List<Class<?>> classList = new ArrayList<Class<?>>();
                    classList.add(Class.forName(selftestClass));
                    test = AllSelfTestCases.suite(this.selfTestUrl,
                        job, job.getDirectory(), Heritrix.getHttpServer().
                        getWebappPath(SELFTEST_WEBAPP), classList);
                } else {
                    // Run all tests.
                    test = AllSelfTestCases.suite(this.selfTestUrl,
                        job, job.getDirectory(), Heritrix.getHttpServer().
                        getWebappPath(SELFTEST_WEBAPP));
                }
                result = junit.textui.TestRunner.run(test);
            }
        } catch (Exception e) {
            logger.info("Failed running selftest analysis: " + e.getMessage());
        } finally  {
            // TODO: This technique where I'm calling shutdown directly means
            // we bypass the running of other crawlended handlers.  Means
            // that such as the statistics tracker have no chance to run so
            // reports are never generated.  Fix -- but preserve 0 or 1 exit
            // code.
            logger.info((new Date()).toString() + " Selftest " +
                (result != null && result.wasSuccessful()? "PASSED": "FAILED"));
            stop();
            Heritrix.shutdown(((result !=  null) && result.wasSuccessful())?
                0: 1);
        }
    }

	public void crawledURIDisregard(CrawlURI curi) {
		// TODO Auto-generated method stub
	}

	public void crawledURIFailure(CrawlURI curi) {
		// TODO Auto-generated method stub
	}

	public void crawledURINeedRetry(CrawlURI curi) {
		// TODO Auto-generated method stub
	}

	public void crawledURISuccessful(CrawlURI curi) {
		// If curi ends in 'Checkpoint/index.html', then run a Checkpoint.
		if (curi.toString().endsWith("/Checkpoint/")) {
			this.getCurrentJob().getController().requestCrawlCheckpoint();
		}
	}
}