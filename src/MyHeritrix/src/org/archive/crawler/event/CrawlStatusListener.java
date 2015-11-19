/* Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler.event;

import java.io.File;

import org.archive.crawler.framework.CrawlController;


/**
 * Listen for CrawlStatus events.
 * 
 * Classes that implement this interface can register themselves with
 * a CrawlController to receive notifications about the events that
 * affect a crawl job's current status.
 *
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.CrawlController#addCrawlStatusListener(CrawlStatusListener)
 */

public interface CrawlStatusListener {
    /**
     * Called on crawl start.
     * @param message Start message.
     */
    public void crawlStarted(String message);
    
    /**
     * Called when a CrawlController is ending a crawl (for any reason)
     *
     * @param sExitMessage Type of exit. Should be one of the STATUS constants
     * in defined in CrawlJob.
     *
     * @see org.archive.crawler.admin.CrawlJob
     */
    public void crawlEnding(String sExitMessage);

    /**
     * Called when a CrawlController has ended a crawl and is about to exit.
     *
     * @param sExitMessage Type of exit. Should be one of the STATUS constants
     * in defined in CrawlJob.
     *
     * @see org.archive.crawler.admin.CrawlJob
     */
    public void crawlEnded(String sExitMessage);

    /**
     * Called when a CrawlController is going to be paused.
     *
     * @param statusMessage Should be
     * {@link org.archive.crawler.admin.CrawlJob#STATUS_WAITING_FOR_PAUSE
     * STATUS_WAITING_FOR_PAUSE}. Passed for convenience
     */
    public void crawlPausing(String statusMessage);

    /**
     * Called when a CrawlController is actually paused (all threads are idle).
     *
     * @param statusMessage Should be
     * {@link org.archive.crawler.admin.CrawlJob#STATUS_PAUSED}. Passed for
     * convenience
     */
    public void crawlPaused(String statusMessage);

    /**
     * Called when a CrawlController is resuming a crawl that had been paused.
     *
     * @param statusMessage Should be
     * {@link org.archive.crawler.admin.CrawlJob#STATUS_RUNNING}. Passed for
     * convenience
     */
    public void crawlResuming(String statusMessage);
    
    /**
     * Called by {@link CrawlController} when checkpointing.
     * @param checkpointDir Checkpoint dir.  Write checkpoint state here.
     * @throws Exception A fatal exception.  Any exceptions
     * that are let out of this checkpoint are assumed fatal
     * and terminate further checkpoint processing.
     */
    public void crawlCheckpoint(File checkpointDir) throws Exception;
}
