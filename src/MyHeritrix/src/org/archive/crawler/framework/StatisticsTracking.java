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
package org.archive.crawler.framework;

import java.util.Iterator;
import java.util.Map;

import org.archive.crawler.framework.exceptions.FatalConfigurationException;

/**
 * An interface for objects that want to collect statistics on
 * running crawls. An implementation of this is referenced in the
 * crawl order and loaded when the crawl begins.
 *
 * <p>It will be given a reference to the relevant CrawlController.
 * The CrawlController will contain any additional configuration
 * information needed.
 *
 * <p>Any class that implements this interface can be specified as a
 * statistics tracker in a crawl order.  The CrawlController will
 * then create and initialize a copy of it and call it's start()
 * method.
 *
 * <p>This interface also specifies several methods to access data that
 * the CrawlController or the URIFrontier may be interested in at
 * run time but do not want to have keep track of for themselves.
 * {@link org.archive.crawler.framework.AbstractTracker AbstractTracker}
 * implements these. If there are more then one StatisticsTracking
 * classes defined in the crawl order only the first one will be
 * used to access this data.
 *
 * <p>It is recommended that it register for
 * {@link org.archive.crawler.event.CrawlStatusListener CrawlStatus} events and
 * {@link org.archive.crawler.event.CrawlURIDispositionListener CrawlURIDisposition}
 * events to be able to properly monitor a crawl. Both are registered with the
 * CrawlController.
 *
 * @author Kristinn Sigurdsson
 *
 * @see AbstractTracker
 * @see org.archive.crawler.event.CrawlStatusListener
 * @see org.archive.crawler.event.CrawlURIDispositionListener
 * @see org.archive.crawler.framework.CrawlController
 */
public interface StatisticsTracking extends Runnable {
    /** Seed successfully crawled */
    public static final String SEED_DISPOSITION_SUCCESS =
        "Seed successfully crawled";
    /** Failed to crawl seed */
    public static final String SEED_DISPOSITION_FAILURE =
        "Failed to crawl seed";
    /** Failed to crawl seed, will retry */
    public static final String SEED_DISPOSITION_RETRY =
        "Failed to crawl seed, will retry";
    /** Seed was disregarded */
    public static final String SEED_DISPOSITION_DISREGARD =
        "Seed was disregarded";
    /** Seed has not been processed */
    public static final String SEED_DISPOSITION_NOT_PROCESSED =
        "Seed has not been processed";
    
    /**
     * Do initialization.
     *
     * The CrawlController will call this method before calling the start()
     * method.
     *
     * @param c The {@link CrawlController CrawlController} running the crawl
     * that this class is to gather statistics on.
     * @throws FatalConfigurationException
     */
    public void initialize(CrawlController c)
    throws FatalConfigurationException;

    /**
     * Returns how long the current crawl has been running (excluding any time
     * spent paused/suspended/stopped) since it began.
     *
     * @return The length of time - in msec - that this crawl has been running.
     */
    public long crawlDuration();

    /**
     * Start the tracker's crawl timing. 
     */
    public void noteStart();
    

    /**
     * Returns the total number of uncompressed bytes processed. Stored
     * data may be much smaller due to compression or duplicate-reduction
     * policies. 
     * 
     * @return The total number of uncompressed bytes written to disk
     * @deprecated misnomer; use totalBytesCrawled instead
     */
    public long totalBytesWritten();
    
    /**
     * Returns the total number of uncompressed bytes crawled. Stored
     * data may be much smaller due to compression or duplicate-reduction
     * policies. 
     * 
     * @return The total number of uncompressed bytes crawled
     */
    public long totalBytesCrawled();
    
    /**
     * Total amount of time spent actively crawling so far.<p>
     * Returns the total amount of time (in milliseconds) that has elapsed from
     * the start of the crawl and until the current time or if the crawl has
     * ended until the the end of the crawl <b>minus</b> any
     * time spent paused.
     * @return Total amount of time (in msec.) spent crawling so far.
     */
    public long getCrawlerTotalElapsedTime();
    
    /**
     * Returns an estimate of recent document download rates
     * based on a queue of recently seen CrawlURIs (as of last snapshot).
     *
     * @return The rate per second of documents gathered during the last
     * snapshot
     */
    public double currentProcessedDocsPerSec();
    
    /**
     * Returns the number of documents that have been processed
     * per second over the life of the crawl (as of last snapshot)
     *
     * @return  The rate per second of documents gathered so far
     */
    public double processedDocsPerSec();
    
    /**
     * Calculates the rate that data, in kb, has been processed
     * over the life of the crawl (as of last snapshot.)
     *
     * @return The rate per second of KB gathered so far
     */
    public long processedKBPerSec();

    /**
     * Calculates an estimate of the rate, in kb, at which documents
     * are currently being processed by the crawler.  For more
     * accurate estimates set a larger queue size, or get
     * and average multiple values (as of last snapshot).
     *
     * @return The rate per second of KB gathered during the last snapshot
     */
    public int currentProcessedKBPerSec();
    
    /**
     * Get the number of active (non-paused) threads.
     * 
     * @return The number of active (non-paused) threads
     */
    public int activeThreadCount();
    
    /**
     * Number of <i>successfully</i> processed URIs.
     *
     * <p>If crawl not running (paused or stopped) this will return the value
     * of the last snapshot.
     *
     * @return The number of successully fetched URIs
     *
     * @see org.archive.crawler.framework.Frontier#succeededFetchCount()
     */
    public long successfullyFetchedCount();
    
    /**
     * @return Total number of URIs (processed + queued +
     * currently being processed)
     */
    public long totalCount();
    
    public float congestionRatio();
    public long deepestUri();
    public long averageDepth();
    
    /**
     * Get a SeedRecord iterator for the job being monitored. If job is no 
     * longer running, stored values will be returned. If job is running, 
     * current seed iterator will be fetched and stored values will be updated.
     * <p>
     * Sort order is:<br>
     * No status code (not processed)<br>
     * Status codes smaller then 0 (largest to smallest)<br>
     * Status codes larger then 0 (largest to smallest)<br>
     * <p>
     * <b>Note:</b> This iterator will iterate over a list of 
     * <i>SeedRecords</i>.
     * @return the seed iterator
     */
    public Iterator getSeedRecordsSortedByStatusCode();

    /**
     * @return legend of progress-statistics
     */
    public String progressStatisticsLegend();

    /**
     * @return line of progress-statistics
     */
    public String getProgressStatisticsLine();
    
    /**
     * @return Map of progress-statistics.
     */
    public Map getProgressStatistics();
}