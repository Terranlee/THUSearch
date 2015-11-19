/* Copyright (C) 2009 Internet Archive 
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
 * Created on Jul 16, 2003
 *
 */
package org.archive.crawler.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.Closure;
import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.deciderules.recrawl.IdenticalDigestDecideRule;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.AbstractTracker;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.util.CrawledBytesHistotable;
import org.archive.net.UURI;
import org.archive.util.ArchiveUtils;
import org.archive.util.MimetypeUtils;
import org.archive.util.ObjectIdentityCache;
import org.archive.util.PaddingStringBuffer;
import org.archive.util.Supplier;

/**
 * This is an implementation of the AbstractTracker. It is designed to function
 * with the WUI as well as performing various logging activity.
 * <p>
 * At the end of each snapshot a line is written to the
 * 'progress-statistics.log' file.
 * <p>
 * The header of that file is as follows:
 * <pre> [timestamp] [discovered]    [queued] [downloaded] [doc/s(avg)]  [KB/s(avg)] [dl-failures] [busy-thread] [mem-use-KB]</pre>
 * First there is a <b>timestamp</b>, accurate down to 1 second.
 * <p>
 * <b>discovered</b>, <b>queued</b>, <b>downloaded</b> and <b>dl-failures</b>
 * are (respectively) the discovered URI count, pending URI count, successfully
 * fetched count and failed fetch count from the frontier at the time of the
 * snapshot.
 * <p>
 * <b>KB/s(avg)</b> is the bandwidth usage.  We use the total bytes downloaded
 * to calculate average bandwidth usage (KB/sec). Since we also note the value
 * each time a snapshot is made we can calculate the average bandwidth usage
 * during the last snapshot period to gain a "current" rate. The first number is
 * the current and the average is in parenthesis.
 * <p>
 * <b>doc/s(avg)</b> works the same way as doc/s except it show the number of
 * documents (URIs) rather then KB downloaded.
 * <p>
 * <b>busy-threads</b> is the total number of ToeThreads that are not available
 * (and thus presumably busy processing a URI). This information is extracted
 * from the crawl controller.
 * <p>
 * Finally mem-use-KB is extracted from the run time environment
 * (<code>Runtime.getRuntime().totalMemory()</code>).
 * <p>
 * In addition to the data collected for the above logs, various other data
 * is gathered and stored by this tracker.
 * <ul>
 *   <li> Successfully downloaded documents per fetch status code
 *   <li> Successfully downloaded documents per document mime type
 *   <li> Amount of data per mime type
 *   <li> Successfully downloaded documents per host
 *   <li> Amount of data per host
 *   <li> Disposition of all seeds (this is written to 'reports.log' at end of
 *        crawl)
 *   <li> Successfully downloaded documents per host per source
 * </ul>
 *
 * @author Parker Thompson
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.framework.StatisticsTracking
 * @see org.archive.crawler.framework.AbstractTracker
 */
public class StatisticsTracker extends AbstractTracker
implements CrawlURIDispositionListener, Serializable {
    private static final long serialVersionUID = 8004878315916392305L;

    /**
     * Messages from the StatisticsTracker.
     */
    private final static Logger logger =
        Logger.getLogger(StatisticsTracker.class.getName());
    
    // TODO: Need to be able to specify file where the object will be
    // written once the CrawlEnded event occurs

    protected long lastPagesFetchedCount = 0;
    protected long lastProcessedBytesCount = 0;

    /*
     * Snapshot data.
     */
    protected long discoveredUriCount = 0;
    protected long queuedUriCount = 0;
    protected long finishedUriCount = 0;

    protected long downloadedUriCount = 0;
    protected long downloadFailures = 0;
    protected long downloadDisregards = 0;
    protected double docsPerSecond = 0;
    protected double currentDocsPerSecond = 0;
    protected int currentKBPerSec = 0;
    protected long totalKBPerSec = 0;
    protected int busyThreads = 0;
    protected long totalProcessedBytes = 0;
    protected float congestionRatio = 0; 
    protected long deepestUri;
    protected long averageDepth;
    
    /*
     * Cumulative data
     */
    /** tally sizes novel, verified (same hash), vouched (not-modified) */ 
    protected CrawledBytesHistotable crawledBytes = new CrawledBytesHistotable();

    protected long notModifiedUriCount = 0;
    protected long dupByHashUriCount = 0;
    protected long novelUriCount = 0;
    
    /** Keep track of the file types we see (mime type -> count) */
    protected ConcurrentMap<String,AtomicLong> mimeTypeDistribution
     = new ConcurrentHashMap<String,AtomicLong>();
    protected ConcurrentMap<String,AtomicLong> mimeTypeBytes
     = new ConcurrentHashMap<String,AtomicLong>();
    
    /** Keep track of fetch status codes */
    protected ConcurrentMap<String,AtomicLong> statusCodeDistribution
     = new ConcurrentHashMap<String,AtomicLong>();
    
    /** reusable Supplier for initial zero AtomicLong instances */
    private static final Supplier<AtomicLong> ATOMIC_ZERO_SUPPLIER = 
        new Supplier<AtomicLong>() {
            public AtomicLong get() {
                return new AtomicLong(0); 
            }};

    /** Keep track of hosts. 
     * 
     * <p>They're transient because usually bigmaps that get reconstituted
     * on recover from checkpoint.
     */
    protected transient ObjectIdentityCache<String,AtomicLong> hostsDistribution = null;
    protected transient ObjectIdentityCache<String,AtomicLong> hostsBytes = null;
    protected transient ObjectIdentityCache<String,AtomicLong> hostsLastFinished = null;

    /** Keep track of URL counts per host per seed */
    protected transient 
        ObjectIdentityCache<String,ConcurrentMap<String,AtomicLong>> sourceHostDistribution = null;

    /**
     * Record of seeds' latest actions.
     */
    protected transient ObjectIdentityCache<String,SeedRecord> processedSeedsRecords;

    // seeds tallies: ONLY UPDATED WHEN SEED REPORT WRITTEN
    private int seedsCrawled;
    private int seedsNotCrawled;
    // sExitMessage: only set at crawl-end
    private String sExitMessage = "Before crawl end";


    public StatisticsTracker(String name) {
        super( name, "A statistics tracker thats integrated into " +
            "the web UI and that creates the progress-statistics log.");
    }

    public void initialize(CrawlController c)
    throws FatalConfigurationException {
        super.initialize(c);
        try {
            this.sourceHostDistribution = c.getBigMap("sourceHostDistribution",
            	ConcurrentMap.class);
            this.hostsDistribution = c.getBigMap("hostsDistribution",
                AtomicLong.class);
            this.hostsBytes = c.getBigMap("hostsBytes", AtomicLong.class);
            this.hostsLastFinished = c.getBigMap("hostsLastFinished",
                AtomicLong.class);
            this.processedSeedsRecords = c.getBigMap("processedSeedsRecords",
                SeedRecord.class);
        } catch (Exception e) {
            throw new FatalConfigurationException("Failed setup of" +
                " StatisticsTracker: " + e);
        }
        controller.addCrawlURIDispositionListener(this);
    }

    protected void finalCleanup() {
        super.finalCleanup();
        if (this.hostsBytes != null) {
            this.hostsBytes.close();
            this.hostsBytes = null;
        }
        if (this.hostsDistribution != null) {
            this.hostsDistribution.close();
            this.hostsDistribution = null;
        }
        if (this.hostsLastFinished != null) {
            this.hostsLastFinished.close();
            this.hostsLastFinished = null;
        }
        if (this.processedSeedsRecords != null) {
            this.processedSeedsRecords.close();
            this.processedSeedsRecords = null;
        }
        if (this.sourceHostDistribution != null) {
            this.sourceHostDistribution.close();
            this.sourceHostDistribution = null;
        }

    }

    protected synchronized void progressStatisticsEvent(final EventObject e) {
        // This method loads "snapshot" data.
        discoveredUriCount = discoveredUriCount();
        downloadedUriCount = successfullyFetchedCount();
        finishedUriCount = finishedUriCount();
        queuedUriCount = queuedUriCount();
        downloadFailures = failedFetchAttempts();
        downloadDisregards = disregardedFetchAttempts();
        totalProcessedBytes = totalBytesCrawled();
        congestionRatio = congestionRatio();
        deepestUri = deepestUri();
        averageDepth = averageDepth();
        
        if (finishedUriCount() == 0) {
            docsPerSecond = 0;
            totalKBPerSec = 0;
        } else if (getCrawlerTotalElapsedTime() < 1000) {
            return; // Not enough time has passed for a decent snapshot.
        } else {
            docsPerSecond = (double) downloadedUriCount /
                (double)(getCrawlerTotalElapsedTime() / 1000);
            // Round to nearest long.
            totalKBPerSec = (long)(((totalProcessedBytes / 1024) /
                 ((getCrawlerTotalElapsedTime()) / 1000)) + .5 );
        }

        busyThreads = activeThreadCount();

        if(shouldrun ||
            (System.currentTimeMillis() - lastLogPointTime) >= 1000) {
            // If shouldrun is false there is a chance that the time interval
            // since last time is too small for a good sample.  We only want
            // to update "current" data when the interval is long enough or
            // shouldrun is true.
            currentDocsPerSecond = 0;
            currentKBPerSec = 0;

            // Note time.
            long currentTime = System.currentTimeMillis();
            long sampleTime = currentTime - lastLogPointTime;

            // if we haven't done anyting or there isn't a reasonable sample
            // size give up.
            if (sampleTime >= 1000) {
                // Update docs/sec snapshot
                long currentPageCount = successfullyFetchedCount();
                long samplePageCount = currentPageCount - lastPagesFetchedCount;

                currentDocsPerSecond =
                    (double) samplePageCount / (double)(sampleTime / 1000);

                lastPagesFetchedCount = currentPageCount;

                // Update kbytes/sec snapshot
                long currentProcessedBytes = totalProcessedBytes;
                long sampleProcessedBytes =
                    currentProcessedBytes - lastProcessedBytesCount;

                currentKBPerSec =
                    (int)(((sampleProcessedBytes/1024)/(sampleTime/1000)) + .5);

                lastProcessedBytesCount = currentProcessedBytes;
            }
        }

        if (this.controller != null) {
            this.controller.logProgressStatistics(getProgressStatisticsLine());
        }
        lastLogPointTime = System.currentTimeMillis();
        super.progressStatisticsEvent(e);
    }

    /**
     * Return one line of current progress-statistics
     * 
     * @param now
     * @return String of stats
     */
    public String getProgressStatisticsLine(Date now) {
        return new PaddingStringBuffer()
            .append(ArchiveUtils.getLog14Date(now))
            .raAppend(32, discoveredUriCount)
            .raAppend(44, queuedUriCount)
            .raAppend(57, downloadedUriCount)
            .raAppend(74, ArchiveUtils.
                doubleToString(currentDocsPerSecond, 2) +
                "(" + ArchiveUtils.doubleToString(docsPerSecond, 2) + ")")
            .raAppend(85, currentKBPerSec + "(" + totalKBPerSec + ")")
            .raAppend(99, downloadFailures)
            .raAppend(113, busyThreads)
            .raAppend(126, (Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()) / 1024)
            .raAppend(140, Runtime.getRuntime().totalMemory() / 1024)
            .raAppend(153, ArchiveUtils.doubleToString(congestionRatio, 2))
            .raAppend(165, deepestUri)
            .raAppend(177, averageDepth)
            .toString();
    }
    
    public Map<String,Number> getProgressStatistics() {
        Map<String,Number> stats = new HashMap<String,Number>();
        stats.put("discoveredUriCount", new Long(discoveredUriCount));
        stats.put("queuedUriCount", new Long(queuedUriCount));
        stats.put("downloadedUriCount", new Long(downloadedUriCount));
        stats.put("currentDocsPerSecond", new Double(currentDocsPerSecond));
        stats.put("docsPerSecond", new Double(docsPerSecond));
        stats.put("totalKBPerSec", new Long(totalKBPerSec));
        stats.put("totalProcessedBytes", new Long(totalProcessedBytes));
        stats.put("currentKBPerSec", new Long(currentKBPerSec));
        stats.put("downloadFailures", new Long(downloadFailures));
        stats.put("busyThreads", new Integer(busyThreads));
        stats.put("congestionRatio", new Double(congestionRatio));
        stats.put("deepestUri", new Long(deepestUri));
        stats.put("averageDepth", new Long(averageDepth));
        stats.put("totalMemory", new Long(Runtime.getRuntime().totalMemory()));
        stats.put("freeMemory", new Long(Runtime.getRuntime().freeMemory()));
        return stats;
    }

    /**
     * Return one line of current progress-statistics
     * 
     * @return String of stats
     */
    public String getProgressStatisticsLine() {
        return getProgressStatisticsLine(new Date());
    }
    
    public double processedDocsPerSec(){
        return docsPerSecond;
    }

    public double currentProcessedDocsPerSec(){
        return currentDocsPerSecond;
    }

    public long processedKBPerSec(){
        return totalKBPerSec;
    }

    public int currentProcessedKBPerSec(){
        return currentKBPerSec;
    }

    /** Returns a HashMap that contains information about distributions of
     *  encountered mime types.  Key/value pairs represent
     *  mime type -> count.
     * <p>
     * <b>Note:</b> All the values are wrapped with a {@link AtomicLong AtomicLong}
     * @return mimeTypeDistribution
     */
    public Map<String,AtomicLong> getFileDistribution() {
        return mimeTypeDistribution;
    }


    /**
     * Increment a counter for a key in a given HashMap. Used for various
     * aggregate data.
     * 
     * As this is used to change Maps which depend on StatisticsTracker
     * for their synchronization, this method should only be invoked
     * from a a block synchronized on 'this'. 
     *
     * @param map The HashMap
     * @param key The key for the counter to be incremented, if it does not
     *               exist it will be added (set to 1).  If null it will
     *            increment the counter "unknown".
     */
    protected static void incrementMapCount(ConcurrentMap<String,AtomicLong> map, 
            String key) {
    	incrementMapCount(map,key,1);
    }

    /**
     * Increment a counter for a key in a given cache. Used for various
     * aggregate data.
     * 
     * @param cache the ObjectIdentityCache
     * @param key The key for the counter to be incremented, if it does not
     *               exist it will be added (set to 1).  If null it will
     *            increment the counter "unknown".
     */
    protected static void incrementCacheCount(ObjectIdentityCache<String,AtomicLong> cache, 
            String key) {
        incrementCacheCount(cache,key,1);
    }
    
    /**
     * Increment a counter for a key in a given cache by an arbitrary amount.
     * Used for various aggregate data. The increment amount can be negative.
     *
     *
     * @param cache
     *            The ObjectIdentityCache
     * @param key
     *            The key for the counter to be incremented, if it does not exist
     *            it will be added (set to equal to <code>increment</code>).
     *            If null it will increment the counter "unknown".
     * @param increment
     *            The amount to increment counter related to the <code>key</code>.
     */
    protected static void incrementCacheCount(ObjectIdentityCache<String,AtomicLong> cache, 
            String key, long increment) {
        if (key == null) {
            key = "unknown";
        }
        AtomicLong lw = cache.getOrUse(key, ATOMIC_ZERO_SUPPLIER);
        lw.addAndGet(increment);
    }
    
    /**
     * Increment a counter for a key in a given HashMap by an arbitrary amount.
     * Used for various aggregate data. The increment amount can be negative.
     *
     * @param map
     *            The Map or ConcurrentMap
     * @param key
     *            The key for the counter to be incremented, if it does not exist
     *            it will be added (set to equal to <code>increment</code>).
     *            If null it will increment the counter "unknown".
     * @param increment
     *            The amount to increment counter related to the <code>key</code>.
     */
    protected static void incrementMapCount(ConcurrentMap<String,AtomicLong> map, 
            String key, long increment) {
        if (key == null) {
            key = "unknown";
        }
        AtomicLong lw = map.get(key);
        if(lw == null) {
            lw = new AtomicLong();
            AtomicLong prevVal = map.putIfAbsent(key, lw);
            if(prevVal != null) {
                lw = prevVal;
            }
        } 
        lw.addAndGet(increment);
    }

    /**
     * Sort the entries of the given HashMap in descending order by their
     * values, which must be longs wrapped with <code>AtomicLong</code>.
     * <p>
     * Elements are sorted by value from largest to smallest. Equal values are
     * sorted in an arbitrary, but consistent manner by their keys. Only items
     * with identical value and key are considered equal.
     *
     * If the passed-in map requires access to be synchronized, the caller
     * should ensure this synchronization. 
     * 
     * @param mapOfAtomicLongValues
     *            Assumes values are wrapped with AtomicLong.
     * @return a sorted set containing the same elements as the map.
     */
    public TreeMap<String,AtomicLong> getReverseSortedCopy(
            final Map<String,AtomicLong> mapOfAtomicLongValues) {
        TreeMap<String,AtomicLong> sortedMap = 
          new TreeMap<String,AtomicLong>(new Comparator<String>() {
            public int compare(String e1, String e2) {
                long firstVal = mapOfAtomicLongValues.get(e1).get();
                long secondVal = mapOfAtomicLongValues.get(e2).get();
                if (firstVal < secondVal) {
                    return 1;
                }
                if (secondVal < firstVal) {
                    return -1;
                }
                // If the values are the same, sort by keys.
                return e1.compareTo(e2);
            }
        });
        try {
            sortedMap.putAll(mapOfAtomicLongValues);
        } catch (UnsupportedOperationException e) {
            for (String key: mapOfAtomicLongValues.keySet()) {
                sortedMap.put(key, mapOfAtomicLongValues.get(key));
            }
        }
        return sortedMap;
    }
    
    /**
     * Sort the entries of the given ObjectIdentityCache in descending order by their
     * values, which must be longs wrapped with <code>AtomicLong</code>.
     * <p>
     * Elements are sorted by value from largest to smallest. Equal values are
     * sorted in an arbitrary, but consistent manner by their keys. Only items
     * with identical value and key are considered equal.
     *
     * If the passed-in map requires access to be synchronized, the caller
     * should ensure this synchronization. 
     * 
     * @param mapOfAtomicLongValues
     *            Assumes values are wrapped with AtomicLong.
     * @return a sorted set containing the same elements as the map.
     */
    public TreeMap<String,AtomicLong> getReverseSortedCopy(
            final ObjectIdentityCache<String,AtomicLong> mapOfAtomicLongValues) {
        TreeMap<String,AtomicLong> sortedMap = 
          new TreeMap<String,AtomicLong>(new Comparator<String>() {
            public int compare(String e1, String e2) {
                long firstVal = mapOfAtomicLongValues.get(e1).get();
                long secondVal = mapOfAtomicLongValues.get(e2).get();
                if (firstVal < secondVal) {
                    return 1;
                }
                if (secondVal < firstVal) {
                    return -1;
                }
                // If the values are the same, sort by keys.
                return e1.compareTo(e2);
            }
        });
        for (String key: mapOfAtomicLongValues.keySet()) {
            sortedMap.put(key, mapOfAtomicLongValues.get(key));
        }
        return sortedMap;
    }

    /**
     * Return a HashMap representing the distribution of status codes for
     * successfully fetched curis, as represented by a hashmap where key -&gt;
     * val represents (string)code -&gt; (integer)count.
     * 
     * <b>Note: </b> All the values are wrapped with a
     * {@link AtomicLong AtomicLong}
     * 
     * @return statusCodeDistribution
     */
    public Map<String,AtomicLong> getStatusCodeDistribution() {
        return statusCodeDistribution;
    }
    
    /**
     * Returns the time (in millisec) when a URI belonging to a given host was
     * last finished processing. 
     * 
     * @param host The host to look up time of last completed URI.
     * @return Returns the time (in millisec) when a URI belonging to a given 
     * host was last finished processing. If no URI has been completed for host
     * -1 will be returned. 
     */
    public AtomicLong getHostLastFinished(String host){
        AtomicLong fini = hostsLastFinished.getOrUse(host, ATOMIC_ZERO_SUPPLIER);
        return fini;
    }

    /**
     * Returns the accumulated number of bytes downloaded from a given host.
     * @param host name of the host
     * @return the accumulated number of bytes downloaded from a given host
     */
    public long getBytesPerHost(String host){
        return ((AtomicLong)hostsBytes.get(host)).get();
    }

    /**
     * Returns the accumulated number of bytes from files of a given file type.
     * @param filetype Filetype to check.
     * @return the accumulated number of bytes from files of a given mime type
     */
    public long getBytesPerFileType(String filetype){
        return ((AtomicLong)mimeTypeBytes.get(filetype)).get();
    }

    /**
     * Get the total number of ToeThreads (sleeping and active)
     *
     * @return The total number of ToeThreads
     */
    public int threadCount() {
        return this.controller != null? controller.getToeCount(): 0;
    }

    /**
     * @return Current thread count (or zero if can't figure it out).
     */ 
    public int activeThreadCount() {
        return this.controller != null? controller.getActiveToeCount(): 0;
        // note: reuse of old busy value seemed misleading: anyone asking
        // for thread count when paused or stopped still wants accurate reading
    }

    /**
     * This returns the number of completed URIs as a percentage of the total
     * number of URIs encountered (should be inverse to the discovery curve)
     *
     * @return The number of completed URIs as a percentage of the total
     * number of URIs encountered
     */
    public int percentOfDiscoveredUrisCompleted() {
        long completed = finishedUriCount();
        long total = discoveredUriCount();

        if (total == 0) {
            return 0;
        }

        return (int) (100 * completed / total);
    }

    /**
     * Number of <i>discovered</i> URIs.
     *
     * <p>If crawl not running (paused or stopped) this will return the value of
     * the last snapshot.
     *
     * @return A count of all uris encountered
     *
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public long discoveredUriCount() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().discoveredUriCount() : discoveredUriCount;
    }

    /**
     * Number of URIs that have <i>finished</i> processing.
     *
     * @return Number of URIs that have finished processing
     *
     * @see org.archive.crawler.framework.Frontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
            controller.getFrontier().finishedUriCount() : finishedUriCount;
    }

    /**
     * Get the total number of failed fetch attempts (connection failures -> give up, etc)
     *
     * @return The total number of failed fetch attempts
     */
    public long failedFetchAttempts() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
            controller.getFrontier().failedFetchCount() : downloadFailures;
    }

    /**
     * Get the total number of failed fetch attempts (connection failures -> give up, etc)
     *
     * @return The total number of failed fetch attempts
     */
    public long disregardedFetchAttempts() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().disregardedUriCount() : downloadDisregards;
    }

    public long successfullyFetchedCount() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().succeededFetchCount() : downloadedUriCount;
    }
    
    public long totalCount() {
        return queuedUriCount() + activeThreadCount() +
            successfullyFetchedCount();
    }

    /**
     * Ratio of number of threads that would theoretically allow
     * maximum crawl progress (if each was as productive as current
     * threads), to current number of threads.
     * 
     * @return float congestion ratio 
     */
    public float congestionRatio() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
            controller.getFrontier().congestionRatio() : congestionRatio;
    }
    
    /**
     * Ordinal position of the 'deepest' URI eligible 
     * for crawling. Essentially, the length of the longest
     * frontier internal queue. 
     * 
     * @return long URI count to deepest URI
     */
    public long deepestUri() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
            controller.getFrontier().deepestUri() : deepestUri;
    }
    
    /**
     * Average depth of the last URI in all eligible queues.
     * That is, the average length of all eligible queues.
     * 
     * @return long average depth of last URIs in queues 
     */
    public long averageDepth() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
            controller.getFrontier().averageDepth() : averageDepth;
    }
    
    /**
     * Number of URIs <i>queued</i> up and waiting for processing.
     *
     * <p>If crawl not running (paused or stopped) this will return the value
     * of the last snapshot.
     *
     * @return Number of URIs queued up and waiting for processing.
     *
     * @see org.archive.crawler.framework.Frontier#queuedUriCount()
     */
    public long queuedUriCount() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().queuedUriCount() : queuedUriCount;
    }

    /** @deprecated use totalBytesCrawled */ 
    public long totalBytesWritten() {
        // return totalBytesCrawled(); 
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().totalBytesWritten() : totalProcessedBytes;
    }
    
    public long totalBytesCrawled() {
        return shouldrun ?
            crawledBytes.getTotal() : totalProcessedBytes;
    }
    
    public String crawledBytesSummary() {
        return crawledBytes.summary();
    }

    /**
     * If the curi is a seed, we insert into the processedSeedsRecords map.
     *
     * @param curi The CrawlURI that may be a seed.
     * @param disposition The dispositino of the CrawlURI.
     */
    private void handleSeed(final CrawlURI curi, final String disposition) {
        if(curi.isSeed()){
            SeedRecord sr = processedSeedsRecords.getOrUse(
                    curi.toString(),
                    new Supplier<SeedRecord>() {
                        public SeedRecord get() {
                            return new SeedRecord(curi, disposition);
                        }});
            sr.updateWith(curi,disposition); 
        }
    }

    public void crawledURISuccessful(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_SUCCESS);
        // save crawled bytes tally
        crawledBytes.accumulate(curi);

        // save crawled docs tally
        if(curi.getFetchStatus()==HttpStatus.SC_NOT_MODIFIED) {
            notModifiedUriCount++;
        } else if (IdenticalDigestDecideRule.hasIdenticalDigest(curi)) {
            dupByHashUriCount++;
        } else {
            novelUriCount++;
        }
        
        // Save status codes
        incrementMapCount(statusCodeDistribution,
            Integer.toString(curi.getFetchStatus()));

        // Save mime types
        String mime = MimetypeUtils.truncate(curi.getContentType());
        incrementMapCount(mimeTypeDistribution, mime);
        incrementMapCount(mimeTypeBytes, mime, curi.getContentSize());

        // Save hosts stats.
        saveHostStats(curi.getFetchStatus() == FetchStatusCodes.S_DNS_SUCCESS ? "dns:" :
                this.controller.getServerCache().getHostFor(curi).getHostName(),
                curi.getContentSize());
        
        if (curi.containsKey(CrawlURI.A_SOURCE_TAG)){
            saveSourceStats(curi.getString(CrawlURI.A_SOURCE_TAG), 
                    this.controller.getServerCache().getHostFor(curi).
                    getHostName()); 
        }
    }
         
    protected void saveSourceStats(String source, String hostname) {
        synchronized(sourceHostDistribution) {
            ConcurrentMap<String,AtomicLong> hostUriCount = 
                sourceHostDistribution.getOrUse(
                        source,
                        new Supplier<ConcurrentMap<String,AtomicLong>>() {
                            public ConcurrentMap<String, AtomicLong> get() {
                                return new ConcurrentHashMap<String,AtomicLong>();
                            }});
            incrementMapCount(hostUriCount, hostname);
        }
    }
    
    protected void saveHostStats(String hostname, long size) {
        incrementCacheCount(hostsDistribution, hostname);

        incrementCacheCount(hostsBytes, hostname, size);
        
        long time = new Long(System.currentTimeMillis());
        getHostLastFinished(hostname).set(time); 
    }

    public void crawledURINeedRetry(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_RETRY);
    }

    public void crawledURIDisregard(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_DISREGARD);
    }

    public void crawledURIFailure(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_FAILURE);
    }

    /**
     * Get a seed iterator for the job being monitored. 
     * 
     * <b>Note:</b> This iterator will iterate over a list of <i>strings</i> not
     * UURIs like the Scope seed iterator. The strings are equal to the URIs'
     * getURIString() values.
     * @return the seed iterator
     * FIXME: Consider using TransformingIterator here
     */
    public Iterator<String> getSeeds() {
        List<String> seedsCopy = new Vector<String>();
        Iterator<UURI> i = controller.getScope().seedsIterator();
        while (i.hasNext()) {
            seedsCopy.add(i.next().toString());
        }
        return seedsCopy.iterator();
    }

    public Iterator<SeedRecord> getSeedRecordsSortedByStatusCode() {
        return getSeedRecordsSortedByStatusCode(getSeeds());
    }
    
    protected Iterator<SeedRecord> getSeedRecordsSortedByStatusCode(
            Iterator<String> i) {
        TreeSet<SeedRecord> sortedSet = 
          new TreeSet<SeedRecord>(new Comparator<SeedRecord>() {
            public int compare(SeedRecord sr1, SeedRecord sr2) {
                int code1 = sr1.getStatusCode();
                int code2 = sr2.getStatusCode();
                if (code1 == code2) {
                    // If the values are equal, sort by URIs.
                    return sr1.getUri().compareTo(sr2.getUri());
                }
                // mirror and shift the nubmer line so as to
                // place zero at the beginning, then all negatives 
                // in order of ascending absolute value, then all 
                // positives descending
                code1 = -code1 - Integer.MAX_VALUE;
                code2 = -code2 - Integer.MAX_VALUE;
                
                return new Integer(code1).compareTo(new Integer(code2));
            }
        });
        while (i.hasNext()) {
            String seed = i.next();
            SeedRecord sr = (SeedRecord) processedSeedsRecords.get(seed);
            if(sr==null) {
                sr = new SeedRecord(seed,SEED_DISPOSITION_NOT_PROCESSED);
            }
            sortedSet.add(sr);
        }
        return sortedSet.iterator();
    }

    public void crawlEnded(String message) {
        logger.info("Entered crawlEnded");
        this.sExitMessage = message; // held for reference by reports
        super.crawlEnded(message);
        logger.info("Leaving crawlEnded");
    }
    
    /**
     * @param writer Where to write.
     */
    protected void writeSeedsReportTo(PrintWriter writer) {
        // Build header.
        writer.print("[code] [status] [seed] [redirect]\n");

        seedsCrawled = 0;
        seedsNotCrawled = 0;
        for (Iterator<SeedRecord> i = getSeedRecordsSortedByStatusCode(getSeeds());
                i.hasNext();) {
            SeedRecord sr = i.next();
            writer.print(sr.getStatusCode());
            writer.print(" ");
            if((sr.getStatusCode() > 0)) {
                seedsCrawled++;
                writer.print("CRAWLED");
            } else {
                seedsNotCrawled++;
                writer.print("NOTCRAWLED");
            }
            writer.print(" ");
            writer.print(sr.getUri());
            if(sr.getRedirectUri()!=null) {
                writer.print(" ");
                writer.print(sr.getRedirectUri());
            }
            writer.print("\n");
        }
    }
    
    protected void writeSourceReportTo(PrintWriter writer) {
        
        writer.print("[source] [host] [#urls]\n");
        // for each source
        for (String sourceKey: sourceHostDistribution.keySet()) {
            Map<String,AtomicLong> hostCounts = sourceHostDistribution.get(sourceKey);
            // sort hosts by #urls
            SortedMap<String,AtomicLong> sortedHostCounts = getReverseSortedHostCounts(hostCounts);
            // for each host
            for (String hostKey: sortedHostCounts.keySet()) {
                AtomicLong hostCount = hostCounts.get(hostKey);
                writer.print(sourceKey.toString());
                writer.print(" ");
                writer.print(hostKey.toString());
                writer.print(" ");
                writer.print(hostCount.get());
                writer.print("\n");
            }
        }
    }
  
    /**
     * Return a copy of the hosts distribution in reverse-sorted (largest first)
     * order.
     * 
     * @return SortedMap of hosts distribution
     */
    public SortedMap<String,AtomicLong> getReverseSortedHostCounts(
            Map<String,AtomicLong> hostCounts) {
        return getReverseSortedCopy(hostCounts);
    }

    
    protected void writeHostsReportTo(final PrintWriter writer) {
        // TODO: use CrawlHosts for all stats; only perform sorting on 
        // manageable number of hosts
        SortedMap<String,AtomicLong> hd = getReverseSortedHostsDistribution();
        // header
        writer.print("[#urls] [#bytes] [host] [#robots] [#remaining] [#novel-urls] [#novel-bytes] [#dup-by-hash-urls] [#dup-by-hash-bytes] [#not-modified-urls] [#not-modified-bytes]\n");
        for (String key: hd.keySet()) {
            // Key is 'host'.
            CrawlHost host = controller.getServerCache().getHostFor(key);
            AtomicLong val = hd.get(key);
            writeReportLine(writer,
                    val == null ? "-" : val.get(),
                    getBytesPerHost(key),
                    key,
                    host.getSubstats().getRobotsDenials(),
                    host.getSubstats().getRemaining(),
                    host.getSubstats().getNovelUrls(),
                    host.getSubstats().getNovelBytes(),
                    host.getSubstats().getDupByHashUrls(),
                    host.getSubstats().getDupByHashBytes(),
                    host.getSubstats().getNotModifiedUrls(),
                    host.getSubstats().getNotModifiedBytes());
        }
        // StatisticsTracker doesn't know of zero-completion hosts; 
        // so supplement report with those entries from host cache
        Closure logZeros = new Closure() {
            public void execute(Object obj) {
                CrawlHost host = (CrawlHost)obj;
                if(host.getSubstats().getRecordedFinishes()==0) {
                    writeReportLine(writer,
                            host.getSubstats().getRecordedFinishes(),
                            host.getSubstats().getTotalBytes(),
                            host.getHostName(),
                            host.getSubstats().getRobotsDenials(),
                            host.getSubstats().getRemaining(),
                            host.getSubstats().getNovelUrls(),
                            host.getSubstats().getNovelBytes(),
                            host.getSubstats().getDupByHashUrls(),
                            host.getSubstats().getDupByHashBytes(),
                            host.getSubstats().getNotModifiedUrls(),
                            host.getSubstats().getNotModifiedBytes());
                }
            }};
        controller.getServerCache().forAllHostsDo(logZeros);
    }
    
    protected void writeReportLine(PrintWriter writer, Object  ... fields) {
       for(Object field : fields) {
           writer.print(field);
           writer.print(" ");
       }
       writer.print("\n");
    }

    /**
     * Return a copy of the hosts distribution in reverse-sorted
     * (largest first) order. 
     * @return SortedMap of hosts distribution
     */
    public SortedMap<String,AtomicLong> getReverseSortedHostsDistribution() {
        return getReverseSortedCopy(hostsDistribution);
    }

    protected void writeMimetypesReportTo(PrintWriter writer) {
        // header
        writer.print("[#urls] [#bytes] [mime-types]\n");
        TreeMap<String,AtomicLong> fd = getReverseSortedCopy(getFileDistribution());
        for (String key: fd.keySet()) { 
            // Key is mime type.
            writer.print(Long.toString(fd.get(key).get()));
            writer.print(" ");
            writer.print(Long.toString(getBytesPerFileType(key)));
            writer.print(" ");
            writer.print(key);
            writer.print("\n");
        }
    }
    
    protected void writeResponseCodeReportTo(PrintWriter writer) {
        // Build header.
        writer.print("[rescode] [#urls]\n");
        TreeMap<String,AtomicLong> scd = getReverseSortedCopy(getStatusCodeDistribution());
        for (String key: scd.keySet()) { 
            writer.print(key);
            writer.print(" ");
            writer.print(Long.toString(scd.get(key).get()));
            writer.print("\n");
        }
    }
    
    protected void writeCrawlReportTo(PrintWriter writer) {
        writer.print("Crawl Name: " + controller.getOrder().getCrawlOrderName());
        writer.print("\nCrawl Status: " + sExitMessage);
        writer.print("\nDuration Time: " +
                ArchiveUtils.formatMillisecondsToConventional(crawlDuration()));
        writer.print("\nTotal Seeds Crawled: " + seedsCrawled);
        writer.print("\nTotal Seeds not Crawled: " + seedsNotCrawled);
        // hostsDistribution contains all hosts crawled plus an entry for dns.
        writer.print("\nTotal Hosts Crawled: " + (hostsDistribution.size()-1));
        writer.print("\nTotal Documents Crawled: " + finishedUriCount);
        writer.print("\nDocuments Crawled Successfully: " + downloadedUriCount);
        writer.print("\nNovel Documents Crawled: " + novelUriCount);
        if (dupByHashUriCount > 0)
            writer.print("\nDuplicate-by-hash Documents Crawled: " + dupByHashUriCount);
        if (notModifiedUriCount > 0)
            writer.print("\nNot-modified Documents Crawled: " + notModifiedUriCount);
        writer.print("\nProcessed docs/sec: " +
                ArchiveUtils.doubleToString(docsPerSecond,2));
        writer.print("\nBandwidth in Kbytes/sec: " + totalKBPerSec);
        writer.print("\nTotal Raw Data Size in Bytes: " + totalProcessedBytes +
                " (" + ArchiveUtils.formatBytesForDisplay(totalProcessedBytes) +
                ") \n");
        writer.print("Novel Bytes: " 
                + crawledBytes.get(CrawledBytesHistotable.NOVEL)
                + " (" + ArchiveUtils.formatBytesForDisplay(
                        crawledBytes.get(CrawledBytesHistotable.NOVEL))
                +  ") \n");
        if(crawledBytes.containsKey(CrawledBytesHistotable.DUPLICATE)) {
            writer.print("Duplicate-by-hash Bytes: " 
                    + crawledBytes.get(CrawledBytesHistotable.DUPLICATE)
                    + " (" + ArchiveUtils.formatBytesForDisplay(
                            crawledBytes.get(CrawledBytesHistotable.DUPLICATE))
                    +  ") \n");
        }
        if(crawledBytes.containsKey(CrawledBytesHistotable.NOTMODIFIED)) {
            writer.print("Not-modified Bytes: " 
                    + crawledBytes.get(CrawledBytesHistotable.NOTMODIFIED)
                    + " (" + ArchiveUtils.formatBytesForDisplay(
                            crawledBytes.get(CrawledBytesHistotable.NOTMODIFIED))
                    +  ") \n");
        }
    }
    
    protected void writeProcessorsReportTo(PrintWriter writer) {
        controller.reportTo(CrawlController.PROCESSORS_REPORT,writer);
    }
    
    protected void writeReportFile(String reportName, String filename) {
        File f = new File(controller.getDisk().getPath(), filename);
        try {
            PrintWriter bw = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(f, false),
                    "UTF-8"));
            writeReportTo(reportName, bw);
            bw.close();
            controller.addToManifest(f.getAbsolutePath(),
                CrawlController.MANIFEST_REPORT_FILE, true);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to write " + f.getAbsolutePath() +
                " at the end of crawl.", e);
        }
        logger.info("wrote report: " + f.getAbsolutePath());
    }
    
    /**
     * @param writer Where to write.
     */
    protected void writeManifestReportTo(PrintWriter writer) {
        controller.reportTo(CrawlController.MANIFEST_REPORT, writer);
    }
    
    /**
     * @param reportName Name of report.
     * @param w Where to write.
     */
    private void writeReportTo(String reportName, PrintWriter w) {
        if("hosts".equals(reportName)) {
            writeHostsReportTo(w);
        } else if ("mime types".equals(reportName)) {
            writeMimetypesReportTo(w);
        } else if ("response codes".equals(reportName)) {
            writeResponseCodeReportTo(w);
        } else if ("seeds".equals(reportName)) {
            writeSeedsReportTo(w);
        } else if ("crawl".equals(reportName)) {
            writeCrawlReportTo(w);
        } else if ("processors".equals(reportName)) {
            writeProcessorsReportTo(w);
        } else if ("manifest".equals(reportName)) {
            writeManifestReportTo(w);
        } else if ("frontier".equals(reportName)) {
            writeFrontierReportTo(w);
        } else if ("source".equals(reportName)) {
            writeSourceReportTo(w);
        }// / TODO else default/error
    }

    /**
     * Write the Frontier's 'nonempty' report (if available)
     * @param writer to report to
     */
    protected void writeFrontierReportTo(PrintWriter writer) {
        if(controller.getFrontier().isEmpty()) {
            writer.println("frontier empty");
        } else {
            controller.getFrontier().reportTo("nonempty", writer);
        }
    }

    /**
     * Run the reports.
     */
    public void dumpReports() {
        // Add all files mentioned in the crawl order to the
        // manifest set.
        controller.addOrderToManifest();
        controller.installThreadContextSettingsHandler();
        writeReportFile("hosts","hosts-report.txt");
        writeReportFile("mime types","mimetype-report.txt");
        writeReportFile("response codes","responsecode-report.txt");
        writeReportFile("seeds","seeds-report.txt");
        writeReportFile("crawl","crawl-report.txt");
        writeReportFile("processors","processors-report.txt");
        writeReportFile("manifest","crawl-manifest.txt");
        writeReportFile("frontier","frontier-report.txt");
        if (sourceHostDistribution.size()>0) {
            writeReportFile("source","source-report.txt");
        }
        // TODO: Save object to disk?
    }

    public void crawlCheckpoint(File cpDir) throws Exception {
        // CrawlController is managing the checkpointing of this object.
        logNote("CRAWL CHECKPOINTING TO " + cpDir.toString());
    }
}
