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
 *
 * ToePool.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;

import org.archive.util.ArchiveUtils;
import org.archive.util.Histotable;
import org.archive.util.Reporter;

/**
 * A collection of ToeThreads. The class manages the ToeThreads currently
 * running. Including increasing and decreasing their number, keeping track
 * of their state and it can be used to kill hung threads.
 *
 * @author Gordon Mohr
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.ToeThread
 */
public class ToePool extends ThreadGroup implements Reporter {
    /** run worker thread slightly lower than usual */
    public static int DEFAULT_TOE_PRIORITY = Thread.NORM_PRIORITY - 1;
    
    protected CrawlController controller;
    protected int nextSerialNumber = 1;
    protected int targetSize = 0; 

    /**
     * Constructor. Creates a pool of ToeThreads. 
     *
     * @param c A reference to the CrawlController for the current crawl.
     */
    public ToePool(CrawlController c) {
        super("ToeThreads");
        this.controller = c;
        setDaemon(true);
    }
    
    public void cleanup() {
        this.controller = null;
    }

    /**
     * @return The number of ToeThreads that are not available (Approximation).
     */
    public int getActiveToeCount() {
        Thread[] toes = getToes();
        int count = 0;
        for (int i = 0; i < toes.length; i++) {
            if((toes[i] instanceof ToeThread) &&
                    ((ToeThread)toes[i]).isActive()) {
                count++;
            }
        }
        return count; 
    }

    /**
     * @return The number of ToeThreads. This may include killed ToeThreads
     *         that were not replaced.
     */
    public int getToeCount() {
        Thread[] toes = getToes();
        int count = 0;
        for (int i = 0; i<toes.length; i++) {
            if((toes[i] instanceof ToeThread)) {
                count++;
            }
        }
        return count; 
    }
    
    private Thread[] getToes() {
        Thread[] toes = new Thread[activeCount()+10];
        this.enumerate(toes);
        return toes;
    }

    /**
     * Change the number of ToeThreads.
     *
     * @param newsize The new number of ToeThreads.
     */
    public void setSize(int newsize)
    {
        targetSize = newsize;
        int difference = newsize - getToeCount(); 
        if (difference > 0) {
            // must create threads
            for(int i = 1; i <= difference; i++) {
                startNewThread();
            }
        } else {
            // must retire extra threads
            int retainedToes = targetSize; 
            Thread[] toes = this.getToes();
            for (int i = 0; i < toes.length ; i++) {
                if(!(toes[i] instanceof ToeThread)) {
                    continue;
                }
                retainedToes--;
                if (retainedToes>=0) {
                    continue; // this toe is spared
                }
                // otherwise:
                ToeThread tt = (ToeThread)toes[i];
                tt.retire();
            }
        }
    }

    /**
     * Kills specified thread. Killed thread can be optionally replaced with a
     * new thread.
     *
     * <p><b>WARNING:</b> This operation should be used with great care. It may
     * destabilize the crawler.
     *
     * @param threadNumber Thread to kill
     * @param replace If true then a new thread will be created to take the
     *           killed threads place. Otherwise the total number of threads
     *           will decrease by one.
     */
    public void killThread(int threadNumber, boolean replace){

        Thread[] toes = getToes();
        for (int i = 0; i< toes.length; i++) {
            if(! (toes[i] instanceof ToeThread)) {
                continue;
            }
            ToeThread toe = (ToeThread) toes[i];
            if(toe.getSerialNumber()==threadNumber) {
                toe.kill();
            }
        }

        if(replace){
            // Create a new toe thread to take its place. Replace toe
            startNewThread();
        }
    }

    private synchronized void startNewThread() {
        ToeThread newThread = new ToeThread(this, nextSerialNumber++);
        newThread.setPriority(DEFAULT_TOE_PRIORITY);
        newThread.start();
    }

    /**
     * @return Instance of CrawlController.
     */
    public CrawlController getController() {
        return controller;
    }
    
    //
    // Reporter implementation
    //
    
    public static String STANDARD_REPORT = "standard";
    public static String COMPACT_REPORT = "compact";
    protected static String[] REPORTS = {STANDARD_REPORT,COMPACT_REPORT};

    public String[] getReports() {
        return REPORTS;
    }

    public void reportTo(String name, PrintWriter writer) {
        if(COMPACT_REPORT.equals(name)) {
            compactReportTo(writer);
            return;
        }
        if(name!=null && !STANDARD_REPORT.equals(name)) {
            writer.print(name);
            writer.print(" not recognized: giving standard report/n");
        }
        standardReportTo(writer);
    }      
            
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    protected void standardReportTo(PrintWriter writer) {
        writer.print("Toe threads report - "
                + ArchiveUtils.get12DigitDate() + "\n");
        writer.print(" Job being crawled: "
                + this.controller.getOrder().getCrawlOrderName() + "\n");
        writer.print(" Number of toe threads in pool: " + getToeCount() + " ("
                + getActiveToeCount() + " active)\n");

        Thread[] toes = this.getToes();
        synchronized (toes) {
            for (int i = 0; i < toes.length; i++) {
                if (!(toes[i] instanceof ToeThread)) {
                    continue;
                }
                ToeThread tt = (ToeThread) toes[i];
                if (tt != null) {
                    writer
                            .print("   ToeThread #" + tt.getSerialNumber()
                                    + "\n");
                    tt.reportTo(writer);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    protected void compactReportTo(PrintWriter writer) {
        writer.print(getToeCount() + " threads (" + getActiveToeCount()
                + " active)\n");

        Thread[] toes = this.getToes();
        boolean legendWritten = false; 
        // TODO: sort by activity: those with curi the longest at front
        synchronized (toes) {
            for (int i = 0; i < toes.length; i++) {
                if (!(toes[i] instanceof ToeThread)) {
                    continue;
                }
                ToeThread tt = (ToeThread) toes[i];
                if (tt != null) {
                    if(!legendWritten) {
                        writer.println(tt.singleLineLegend());
                        legendWritten = true;
                    }
                    tt.singleLineReportTo(writer);
                }
            }
        }
    }

    public void singleLineReportTo(PrintWriter w) {
        Histotable<Object> ht = new Histotable<Object>();
        Thread[] toes = getToes();
        for (int i = 0; i < toes.length; i++) {

            if(!(toes[i] instanceof ToeThread)) {
                continue;
            }
            ToeThread tt = (ToeThread)toes[i];
            if(tt!=null) {
                ht.tally(tt.getStep());
            }
        }
        TreeSet sorted = ht.getSortedByCounts();
        w.print(getToeCount());
        w.print(" threads: ");        
        w.print(Histotable.entryString(sorted.first()));
        if(sorted.size()>1) {
            Iterator iter = sorted.iterator();
            iter.next();
            w.print("; ");
            w.print(Histotable.entryString(iter.next()));
        }
        if(sorted.size()>2) {
            w.print("; etc...");
        }
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return "total: mostCommonStateTotal secondMostCommonStateTotal";
    }
    
    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }

    public void reportTo(PrintWriter writer) {
        reportTo(null,writer);
    }
}
