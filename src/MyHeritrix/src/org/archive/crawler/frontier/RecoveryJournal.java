/* RecoveryJournal
 *
 * $Id: RecoveryJournal.java 5870 2008-07-11 22:14:18Z gojomo $
 *
 * Created on Jul 20, 2004
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
package org.archive.crawler.frontier;

import it.unimi.dsi.mg4j.util.MutableString;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.io.CrawlerJournal;
import org.archive.net.UURI;

/**
 * Helper class for managing a simple Frontier change-events journal which is
 * useful for recovering from crawl problems.
 * 
 * By replaying the journal into a new Frontier, its state (at least with
 * respect to URIs alreadyIncluded and in pending queues) will match that of the
 * original Frontier, allowing a pseudo-resume of a previous crawl, at least as
 * far as URI visitation/coverage is concerned.
 * 
 * @author gojomo
 */
public class RecoveryJournal extends CrawlerJournal 
implements FrontierJournal {
    private static final Logger LOGGER = Logger.getLogger(
            RecoveryJournal.class.getName());
    
    public final static String F_ADD = "F+ ";
    public final static String F_EMIT = "Fe ";
    public final static String F_DISREGARD = "Fd ";
    public final static String F_RESCHEDULE = "Fr ";
    public final static String F_SUCCESS = "Fs ";
    public final static String F_FAILURE = "Ff ";
    
    //  show recovery progress every this many lines
    private static final int PROGRESS_INTERVAL = 1000000;

    // once this many URIs are queued during recovery, allow 
    // crawl to begin, while enqueuing of other URIs from log
    // continues in background
    private static final long ENOUGH_TO_START_CRAWLING = 100000;
    
    /**
     * Create a new recovery journal at the given location
     * 
     * @param path Directory to make the recovery  journal in.
     * @param filename Name to use for recovery journal file.
     * @throws IOException
     */
    public RecoveryJournal(String path, String filename)
    throws IOException {
        super(path,filename);
        timestamp_interval = 10000; // write timestamp lines occasionally
    }
    
    public  void added(CandidateURI curi) {
        writeLongUriLine(F_ADD, curi);
    }
    
    public synchronized void writeLongUriLine(String tag, CandidateURI curi) {
        accumulatingBuffer.length(0);
        this.accumulatingBuffer.append(tag).
            append(curi.toString()).
            append(" "). 
            append(curi.getPathFromSeed()).
            append(" ").
            append(curi.flattenVia());
        writeLine(accumulatingBuffer);
    }

    public void finishedSuccess(CandidateURI curi) {
        writeLongUriLine(F_SUCCESS, curi);
    }

    public void emitted(CandidateURI curi) {
        writeLine(F_EMIT, curi.toString());

    }
    public void finishedDisregard(CandidateURI curi) {
        writeLine(F_DISREGARD, curi.toString());
    }
    
    public void finishedFailure(CandidateURI curi) {
        writeLongUriLine(F_FAILURE,curi);
    }

    public void rescheduled(CandidateURI curi) {
        writeLine(F_RESCHEDULE, curi.toString());
    }

    /**
     * Utility method for scanning a recovery journal and applying it to
     * a Frontier.
     * 
     * @param source Recover log path.
     * @param frontier Frontier reference.
     * @param retainFailures
     * @throws IOException
     * 
     * @see org.archive.crawler.framework.Frontier#importRecoverLog(String, boolean)
     */
    public static void importRecoverLog(final File source,
        final CrawlController controller, final boolean retainFailures)
    throws IOException {
        if (source == null) {
            throw new IllegalArgumentException("Passed source file is null.");
        }
        LOGGER.info("recovering frontier completion state from "+source);
        
        // first, fill alreadyIncluded with successes (and possibly failures),
        // and count the total lines
        final int lines =
            importCompletionInfoFromLog(source, controller, retainFailures);
        
        LOGGER.info("finished completion state; recovering queues from " +
            source);

        // now, re-add anything that was in old frontier and not already
        // registered as finished. Do this in a separate thread that signals
        // this thread once ENOUGH_TO_START_CRAWLING URIs have been queued. 
        final CountDownLatch recoveredEnough = new CountDownLatch(1);
        new Thread(new Runnable() {
            public void run() {
                importQueuesFromLog(source, controller, lines, recoveredEnough);
            }
        }, "queuesRecoveryThread").start();
        
        try {
            // wait until at least ENOUGH_TO_START_CRAWLING URIs queued
            recoveredEnough.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Import just the SUCCESS (and possibly FAILURE) URIs from the given
     * recovery log into the frontier as considered included. 
     * 
     * @param source recovery log file to use
     * @param frontier frontier to update
     * @param retainFailures whether failure ('Ff') URIs should count as done
     * @return number of lines in recovery log (for reference)
     * @throws IOException
     */
    private static int importCompletionInfoFromLog(File source, 
            CrawlController controller, boolean retainFailures) throws IOException {
        Frontier frontier = controller.getFrontier();
        boolean checkScope = (Boolean) controller.getOrder()
                .getUncheckedAttribute(null,
                        CrawlOrder.ATTR_RECOVER_SCOPE_INCLUDES);
        CrawlScope scope = checkScope ? controller.getScope() : null;
        // Scan log for all 'Fs' lines: add as 'alreadyIncluded'
        BufferedInputStream is = getBufferedInput(source);
        // create MutableString of good starting size (will grow if necessary)
        MutableString read = new MutableString(UURI.MAX_URL_LENGTH); 
        int lines = 0; 
        try {
            while (readLine(is,read)) {
                lines++;
                boolean wasSuccess = read.startsWith(F_SUCCESS);
                if (wasSuccess
						|| (retainFailures && read.startsWith(F_FAILURE))) {
                    try {
                        CandidateURI cauri = CandidateURI.fromString(
                                read.substring(F_SUCCESS.length()).toString());
                        if(checkScope) {
                            if(!scope.accepts(cauri)) {
                                // skip out-of-scope URIs.
                                continue;
                            }
                        }
                        frontier.considerIncluded(cauri.getUURI());
                        if(wasSuccess) {
                            if (frontier.getFrontierJournal() != null) {
                                frontier.getFrontierJournal().
                                    finishedSuccess(cauri);
                            }
                        } else {
                            // carryforward failure, in case future recovery
                            // wants to no retain them as finished 
                            if (frontier.getFrontierJournal() != null) {
                                frontier.getFrontierJournal().
                                    finishedFailure(cauri);
                            }
                        }
                    } catch (URIException e) {
                        e.printStackTrace();
                    }
                }
                if((lines%PROGRESS_INTERVAL)==0) {
                    // every 1 million lines, print progress
                    LOGGER.info(
                            "at line " + lines 
                            + " alreadyIncluded count = " +
                            frontier.discoveredUriCount());
                }
            }
        } catch (EOFException e) {
            // expected in some uncleanly-closed recovery logs; ignore
        } finally {
            is.close();
        }
        return lines;
    }

    /**
     * Read a line from the given bufferedinputstream into the MutableString.
     * Return true if a line was read; false if EOF. 
     * 
     * @param is
     * @param read
     * @return True if we read a line.
     * @throws IOException
     */
    private static boolean readLine(BufferedInputStream is, MutableString read)
    throws IOException {
        read.length(0);
        int c = is.read();
        while((c!=-1)&&c!='\n'&&c!='\r') {
            read.append((char)c);
            c = is.read();
        }
        if(c==-1 && read.length()==0) {
            // EOF and none read; return false
            return false;
        }
        if(c=='\n') {
            // consume LF following CR, if present
            is.mark(1);
            if(is.read()!='\r') {
                is.reset();
            }
        }
        // a line (possibly blank) was read
        return true;
    }

    /**
     * Import all ADDs from given recovery log into the frontier's queues
     * (excepting those the frontier drops as already having been included)
     * 
     * @param source recovery log file to use
     * @param frontier frontier to update
     * @param lines total lines noted in recovery log earlier
     * @param enough latch signalling 'enough' URIs queued to begin crawling
     */
    private static void importQueuesFromLog(File source, CrawlController controller,
            int lines, CountDownLatch enough) {
        BufferedInputStream is;
        // create MutableString of good starting size (will grow if necessary)
        MutableString read = new MutableString(UURI.MAX_URL_LENGTH);
        controller.installThreadContextSettingsHandler();
        Frontier frontier = controller.getFrontier();
        boolean checkScope = (Boolean) controller.getOrder()
        .getUncheckedAttribute(null,
                CrawlOrder.ATTR_RECOVER_SCOPE_ENQUEUES);
        CrawlScope scope = checkScope ? controller.getScope() : null;
        long queuedAtStart = frontier.queuedUriCount();
        long queuedDuringRecovery = 0;
        int qLines = 0;
        
        try {
            // Scan log for all 'F+' lines: if not alreadyIncluded, schedule for
            // visitation
            is = getBufferedInput(source);
            try {
                while (readLine(is,read)) {
                    qLines++;
                    if (read.startsWith(F_ADD)) {
                        try {
                            CandidateURI caUri = CandidateURI.fromString(
                                    read.substring(F_ADD.length()).toString());
                            if(checkScope) {
                                if(!scope.accepts(caUri)) {
                                    // skip out-of-scope URIs.
                                    continue;
                                }
                            }
                            frontier.schedule(caUri);
                            
                            queuedDuringRecovery =
                                frontier.queuedUriCount() - queuedAtStart;
                            if(((queuedDuringRecovery + 1) %
                                    ENOUGH_TO_START_CRAWLING) == 0) {
                                enough.countDown();
                            }
                        } catch (URIException e) {
                            LOGGER.log(Level.WARNING, "bad URI during " +
                                "log-recovery of queue contents: "+read,e);
                            // and continue...
                        } catch (RuntimeException e) {
                            LOGGER.log(Level.SEVERE, "exception during " +
                                    "log-recovery of queue contents: "+read,e);
                            // and continue, though this may be risky
                            // if the exception wasn't a trivial NPE 
                            // or wrapped interrupted-exception.
                        }
                    }
                    if((qLines%PROGRESS_INTERVAL)==0) {
                        // every 1 million lines, print progress
                        LOGGER.info(
                                "through line " 
                                + qLines + "/" + lines 
                                + " queued count = " +
                                frontier.queuedUriCount());
                    }
                }
            } catch (EOFException e) {
                // no problem: untidy end of recovery journal
            } finally {
            	    is.close(); 
            }
        } catch (IOException e) {
            // serious -- but perhaps ignorable? -- error
            LOGGER.log(Level.SEVERE, "problem importing queues", e);
        }
        LOGGER.info("finished recovering frontier from "+source+" "
                +qLines+" lines processed");
        enough.countDown();
    }
}
