/* BdbFrontier
 * 
 * $Id: BdbFrontier.java 6815 2010-04-12 21:32:49Z gojomo $
* 
 * Created on Sep 24, 2004
 *
 *  Copyright (C) 2004 Internet Archive.
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
  */
package org.archive.crawler.frontier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.collections.Closure;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.crawler.util.BloomUriUniqFilter;
import org.archive.crawler.util.CheckpointUtils;
import org.archive.crawler.util.DiskFPMergeUriUniqFilter;
import org.archive.crawler.util.MemFPMergeUriUniqFilter;
import org.archive.queue.StoredQueue;
import org.archive.util.ArchiveUtils;
import org.archive.util.Supplier;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;

/**
 * A Frontier using several BerkeleyDB JE Databases to hold its record of
 * known hosts (queues), and pending URIs. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends WorkQueueFrontier implements Serializable {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
        .classnameBasedUID(BdbFrontier.class, 1);

    private static final Logger logger =
        Logger.getLogger(BdbFrontier.class.getName());

    /** all URIs scheduled to be crawled */
    protected transient BdbMultipleWorkQueues pendingUris;

    /** all URI-already-included options available to be chosen */
    private String[] AVAILABLE_INCLUDED_OPTIONS = new String[] {
            BdbUriUniqFilter.class.getName(),
            BloomUriUniqFilter.class.getName(),
            MemFPMergeUriUniqFilter.class.getName(),
            DiskFPMergeUriUniqFilter.class.getName()};
    
    /** URI-already-included to use (by class name) */
    public final static String ATTR_INCLUDED = "uri-included-structure";
    
    private final static String DEFAULT_INCLUDED =
        BdbUriUniqFilter.class.getName();
    
    /** URI-already-included to use (by class name) */
    public final static String ATTR_DUMP_PENDING_AT_CLOSE = 
        "dump-pending-at-close";
    private final static Boolean DEFAULT_DUMP_PENDING_AT_CLOSE = 
        Boolean.FALSE;

    
    /**
     * Constructor.
     * @param name Name for of this Frontier.
     */
    public BdbFrontier(String name) {
        this(name, "BdbFrontier. "
                + "A Frontier using BerkeleyDB Java Edition databases for "
                + "persistence to disk.");
    }

    /**
     * Create the BdbFrontier
     * 
     * @param name
     * @param description
     */
    public BdbFrontier(String name, String description) {
        super(name, description);
        Type t = addElementToDefinition(new SimpleType(ATTR_INCLUDED,
                "Structure to use for tracking already-seen URIs. Non-default " +
                "options may require additional configuration via system " +
                "properties.", DEFAULT_INCLUDED, AVAILABLE_INCLUDED_OPTIONS));
        t.setExpertSetting(true);
        t = addElementToDefinition(new SimpleType(ATTR_DUMP_PENDING_AT_CLOSE,
                "Whether to dump all URIs waiting in queues to crawl.log " +
                "when a crawl ends. May add a significant delay to " +
                "crawl termination. Dumped lines will have a zero (0) " +
                "status.", DEFAULT_DUMP_PENDING_AT_CLOSE));
        t.setExpertSetting(true);
    }

    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultipleWorkQueues
     * @throws DatabaseException
     */
    private BdbMultipleWorkQueues createMultipleWorkQueues()
    throws DatabaseException {
        return new BdbMultipleWorkQueues(this.controller.getBdbEnvironment(),
            this.controller.getBdbEnvironment().getClassCatalog(),
            this.controller.isCheckpointRecover());
    }

    
    @Override
    protected void initQueuesOfQueues() {
        if(this.controller.isCheckpointRecover()) {
            // do not setup here; take/init from deserialized frontier
            return; 
        }
        // small risk of OutOfMemoryError: if 'hold-queues' is false,
        // readyClassQueues may grow in size without bound
        readyClassQueues = new LinkedBlockingQueue<String>();

        try {
            Database inactiveQueuesDb = this.controller.getBdbEnvironment()
                    .openDatabase(null, "inactiveQueues",
                            StoredQueue.databaseConfig());
            inactiveQueues = new StoredQueue<String>(inactiveQueuesDb,
                    String.class, null);
            Database retiredQueuesDb = this.controller.getBdbEnvironment()
                    .openDatabase(null, "retiredQueues",
                            StoredQueue.databaseConfig());
            retiredQueues = new StoredQueue<String>(retiredQueuesDb,
                    String.class, null);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
        
        // small risk of OutOfMemoryError: in large crawls with many 
        // unresponsive queues, an unbounded number of snoozed queues 
        // may exist
        snoozedClassQueues = Collections.synchronizedSortedSet(new TreeSet<WorkQueue>());
    }

    protected Queue<String> reinit(Queue<String> q, String name) {
        try {
            // restore the innner Database/StoredSortedMap of the queue
            Database db = this.controller.getBdbEnvironment()
                .openDatabase(null, name, StoredQueue.databaseConfig());
            
            StoredQueue<String> queue;
            if(q instanceof StoredQueue) {
                queue = (StoredQueue<String>) q;
                queue.hookupDatabase(db, String.class, null);
            } else {
                // recovery of older checkpoint; copy to StoredQueue
                queue = new StoredQueue<String>(db,String.class,
                        this.controller.getBdbEnvironment().getClassCatalog()); 
                queue.addAll(q);
            }
            return queue;
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Create a UriUniqFilter that will serve as record 
     * of already seen URIs.
     *
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws IOException
     */
    protected UriUniqFilter createAlreadyIncluded() throws IOException {
        UriUniqFilter uuf;
        String c = null;
        try {
            c = (String)getAttribute(null, ATTR_INCLUDED);
        } catch (AttributeNotFoundException e) {
            // Do default action if attribute not in order.
        }
        // TODO: avoid all this special-casing; enable some common
        // constructor interface usable for all alt implemenations
        if (c != null && c.equals(BloomUriUniqFilter.class.getName())) {
            uuf = this.controller.isCheckpointRecover()?
                    deserializeAlreadySeen(BloomUriUniqFilter.class,
                        this.controller.getCheckpointRecover().getDirectory()):
                    new BloomUriUniqFilter();
        } else if (c!=null && c.equals(MemFPMergeUriUniqFilter.class.getName())) {
            // TODO: add checkpointing for MemFPMergeUriUniqFilter
            uuf = new MemFPMergeUriUniqFilter();
        } else if (c!=null && c.equals(DiskFPMergeUriUniqFilter.class.getName())) {
            // TODO: add checkpointing for DiskFPMergeUriUniqFilter
            uuf = new DiskFPMergeUriUniqFilter(controller.getScratchDisk());
        } else {
            // Assume its BdbUriUniqFilter.
            uuf = this.controller.isCheckpointRecover()?
                deserializeAlreadySeen(BdbUriUniqFilter.class,
                    this.controller.getCheckpointRecover().getDirectory()):
                new BdbUriUniqFilter(this.controller.getBdbEnvironment());
            if (this.controller.isCheckpointRecover()) {
                // If recover, need to call reopen of the db.
                try {
                    ((BdbUriUniqFilter)uuf).
                        reopen(this.controller.getBdbEnvironment());
                } catch (DatabaseException e) {
                    throw new IOException(e.getMessage());
                }
            }   
        }
        uuf.setDestination(this);
        return uuf;
    }
    
    protected UriUniqFilter deserializeAlreadySeen(
            final Class<? extends UriUniqFilter> cls,
            final File dir)
    throws FileNotFoundException, IOException {
        UriUniqFilter uuf = null;
        try {
            logger.fine("Started deserializing " + cls.getName() +
                " of checkpoint recover.");
            uuf = CheckpointUtils.readObjectFromFile(cls, dir);
            logger.fine("Finished deserializing bdbje as part " +
                "of checkpoint recover.");
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize "  +
                cls.getName() + ": " + e.getMessage());
        }
        return uuf;
    }

    /**
     * Return the work queue for the given CrawlURI's classKey. URIs
     * are ordered and politeness-delayed within their 'class'.
     * 
     * @param curi CrawlURI to base queue on
     * @return the found or created BdbWorkQueue
     */
    protected WorkQueue getQueueFor(final CrawlURI curi) {
        final String classKey = curi.getClassKey();
        synchronized (allQueues) {
            WorkQueue wq = allQueues.getOrUse(
                classKey,
                new Supplier<WorkQueue>() {
                    public WorkQueue get() {
                        String qKey = new String(classKey); // ensure private minimal key
                        WorkQueue q = new BdbWorkQueue(qKey, BdbFrontier.this);
                        q.setTotalBudget(((Long)getUncheckedAttribute(
                                curi,ATTR_QUEUE_TOTAL_BUDGET)).longValue()); 
                        return q;
                    }});
            return wq;
        }
    }
    
    /**
     * Return the work queue for the given classKey, or null
     * if no such queue exists.
     * 
     * @param classKey key to look for
     * @return the found WorkQueue
     */
    protected WorkQueue getQueueFor(String classKey) {
        WorkQueue wq; 
        synchronized (allQueues) {
            wq = (WorkQueue)allQueues.get(classKey);
        }
        return wq;
    }

    public FrontierMarker getInitialMarker(String regexpr,
            boolean inCacheOnly) {
        return pendingUris.getInitialMarker(regexpr);
    }

    /**
     * Return list of urls.
     * @param marker
     * @param numberOfMatches
     * @param verbose 
     * @return List of URIs (strings).
     */
    public ArrayList<String> getURIsList(FrontierMarker marker, 
            int numberOfMatches, final boolean verbose) {
        List curis;
        try {
            curis = pendingUris.getFrom(marker, numberOfMatches);
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        ArrayList<String> results = new ArrayList<String>(curis.size());
        Iterator iter = curis.iterator();
        while(iter.hasNext()) {
            CrawlURI curi = (CrawlURI) iter.next();
            results.add("["+curi.getClassKey()+"] "+curi.singleLineReport());
        }
        return results;
    }
    
    protected void initQueue() throws IOException {
        try {
            this.pendingUris = createMultipleWorkQueues();
        } catch(DatabaseException e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }
    }
    
    public void finalTasks() {
    	if((Boolean)getUncheckedAttribute(null,ATTR_DUMP_PENDING_AT_CLOSE)) {
            try {
                dumpAllPendingToLog();
            } catch (DatabaseException e) {
                logger.log(Level.WARNING,"dump pending problem",e);
            }
        }
    }
    
    protected void closeQueue() {
        if (this.pendingUris != null) {
            this.pendingUris.close();
            this.pendingUris = null;
        }
    }
        
    protected BdbMultipleWorkQueues getWorkQueues() {
        return pendingUris;
    }

    protected boolean workQueueDataOnDisk() {
        return true;
    }
    
    public void initialize(CrawlController c)
    throws FatalConfigurationException, IOException {
        this.controller = c;
        // fill in anything from a checkpoint recovery first (because
        // usual initialization will skip initQueueOfQueues in checkpoint)
        if (c.isCheckpointRecover()) {
            // If a checkpoint recover, copy old values from serialized
            // instance into this Frontier instance. Do it this way because 
            // though its possible to serialize BdbFrontier, its currently not
            // possible to set/remove frontier attribute plugging the
            // deserialized object back into the settings system.
            // The below copying over is error-prone because its easy
            // to miss a value.  Perhaps there's a better way?  Introspection?
            BdbFrontier f = null;
            try {
                f = (BdbFrontier)CheckpointUtils.
                    readObjectFromFile(this.getClass(),
                        c.getCheckpointRecover().getDirectory());
            } catch (FileNotFoundException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            } catch (IOException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            }

            this.nextOrdinal = f.nextOrdinal;
            this.totalProcessedBytes = f.totalProcessedBytes;
            this.liveDisregardedUriCount = f.liveDisregardedUriCount;
            this.liveFailedFetchCount = f.liveFailedFetchCount;
            this.processedBytesAfterLastEmittedURI =
                f.processedBytesAfterLastEmittedURI;
            this.liveQueuedUriCount = f.liveQueuedUriCount;
            this.liveSucceededFetchCount = f.liveSucceededFetchCount;
            this.lastMaxBandwidthKB = f.lastMaxBandwidthKB;
            this.readyClassQueues = f.readyClassQueues;
            this.inactiveQueues = reinit(f.inactiveQueues,"inactiveQueues");
            this.retiredQueues = reinit(f.retiredQueues,"retiredQueues");
            this.snoozedClassQueues = f.snoozedClassQueues;
            this.inProcessQueues = f.inProcessQueues;
            super.initialize(c);
            wakeQueues();
        } else {
            // perform usual initialization 
            super.initialize(c);
        }
    }

    
    
    @Override
    public void crawlEnded(String sExitMessage) {
        ((StoredQueue)inactiveQueues).close();
        ((StoredQueue)retiredQueues).close();
        super.crawlEnded(sExitMessage);
    }

    public void crawlCheckpoint(File checkpointDir) throws Exception {
        super.crawlCheckpoint(checkpointDir);
        logger.fine("Started serializing already seen as part "
            + "of checkpoint. Can take some time.");
        // An explicit sync on the any deferred write dbs is needed to make the
        // db recoverable. Sync'ing the environment doesn't work.
        if (this.pendingUris != null) {
        	this.pendingUris.sync();
        }
        CheckpointUtils.writeObjectToFile(this.alreadyIncluded, checkpointDir);
        logger.fine("Finished serializing already seen as part "
            + "of checkpoint.");
        // Serialize ourselves.
        CheckpointUtils.writeObjectToFile(this, checkpointDir);
    }
    
    /**
     * Dump all still-enqueued URIs to the crawl.log -- without actually
     * dequeuing. Useful for understanding what was remaining in a
     * crawl that was ended early, for example at a time limit. 
     * 
     * @throws DatabaseException
     */
    public void dumpAllPendingToLog() throws DatabaseException {
        Closure tolog = new Closure() {
            public void execute(Object curi) {
                log((CrawlURI)curi);
            }};
        pendingUris.forAllPendingDo(tolog);
    }
}
