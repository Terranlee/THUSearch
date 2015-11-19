/* ARHostQueueList.java
*
* Created on Sep 13, 2004
*
* Copyright (C) 2004 Kristinn Sigur?sson.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

/**
 * Maintains an ordered list of {@link AdaptiveRevisitHostQueue}s used by a
 * Frontier.
 * <p> 
 * The list is ordered by the 
 * {@link AdaptiveRevisitHostQueue#getNextReadyTime() AdaptiveRevisitHostQueue.getNextReadyTime()}, 
 * smallest value at the top of the list and then on in descending order.
 * <p>
 * The list will maintain a list of hostnames in a seperate DB. On creation a
 * list will try to open the DB at a specified location. If it already exists
 * the list will create HQs for all the hostnames in the list, discarding 
 * those that turn out to be empty.
 * <p>
 * Any BDB DatabaseException will be converted to an IOException by public 
 * methods. This includes preserving the original stacktrace, in favor of the
 * one created for the IOException, so that the true source of the exception
 * is not lost. 
 *
 * @author Kristinn Sigurdsson
 */
public class AdaptiveRevisitQueueList implements Reporter {

    // TODO: Handle HQs becoming empty.
    
    /** The Environment for the BerkleyDB databases in the HQs */
    private final Environment env;
    private final StoredClassCatalog catalog;
    /** Contains host names for all HQs. Name is key, valence is value */
    private Database hostNamesDB;
    private EntryBinding keyBinding;
    private EntryBinding valueBinding;
    
    /** A hash table of all the HQs (wrapped), keyed by hostName */
    private HashMap<String,AdaptiveRevisitHostQueueWrapper> hostQueues; 
    /** Contains the hostQueues (wrapped) sorted by their time of next fetch */
    private TreeSet<AdaptiveRevisitHostQueueWrapper> sortedHostQueues;
    /** Logger */
    private static final Logger logger =
        Logger.getLogger(AdaptiveRevisitQueueList.class.getName());

    
    public AdaptiveRevisitQueueList(Environment env,
            StoredClassCatalog catalog)
    throws IOException {
        Cursor cursor = null;
        try {
            this.env = env;
            this.catalog = catalog;
            keyBinding = new StringBinding();
            valueBinding = new IntegerBinding();

            // Then initialize other data
            hostQueues = new HashMap<String,AdaptiveRevisitHostQueueWrapper>();
            sortedHostQueues = new TreeSet<AdaptiveRevisitHostQueueWrapper>();

            // Open the hostNamesDB
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(false);
            dbConfig.setAllowCreate(true);
            hostNamesDB = env.openDatabase(null, "hostNames", dbConfig);

            // Read any existing hostNames and create relevant HQs
            cursor = hostNamesDB.openCursor(null, null);
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry dataEntry = new DatabaseEntry();
            OperationStatus opStatus = cursor.getFirst(keyEntry, dataEntry,
                    LockMode.DEFAULT);
            while (opStatus == OperationStatus.SUCCESS) {
                // Got one!
                String hostName = (String)keyBinding.entryToObject(keyEntry);
                int valence = ((Integer)valueBinding.entryToObject(dataEntry)).
                    intValue();
                opStatus = cursor.getNext(keyEntry, dataEntry, LockMode.DEFAULT);
                // Create HQ
                createHQ(hostName, valence);
                // TODO: If the hq is empty, then it can be discarded
                // immediately
            }
        } catch (DatabaseException e) {
            throw convertDbException(e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (DatabaseException e) {
                    throw convertDbException(e);
                }
            }
        }
    }
        
    private IOException convertDbException(Exception e) {
        IOException e2 = new IOException(e.getMessage());
        e2.setStackTrace(e.getStackTrace());
        return e2;
    }
    
    /**
     * Get an AdaptiveRevisitHostQueue for the specified host.
     * <p>
     * If one does not already exist, null is returned 
     * 
     * @param hostName The host's name 
     * @return an AdaptiveRevisitHostQueue for the specified host
     */
    public AdaptiveRevisitHostQueue getHQ(String hostName) {
        AdaptiveRevisitHostQueueWrapper wrapper =
            ((AdaptiveRevisitHostQueueWrapper)hostQueues.get(hostName));
        if (wrapper != null) {
            return wrapper.hq;
        }
        return null;
    }
    
    /**
     * Creates a new AdaptiveRevisitHostQueue.
     * <p>
     * If a HQ already existed for the specified hostName, the existing HQ
     * is returned as it is. It's existing valence will <i>not</i> be updated
     * to reflect a different valence.
     * 
     * @param hostName
     * @param valence number of simultaneous connections allowed to this host
     * @return the newly created HQ
     * @throws IOException
     */
    public AdaptiveRevisitHostQueue createHQ(String hostName, int valence)
            throws IOException{
        AdaptiveRevisitHostQueueWrapper hqw = hostQueues.get(hostName);
        if (hqw != null) {
            return hqw.hq;
        }
        AdaptiveRevisitHostQueue hq;
        // Ok, the HQ does not already exist. (Had to make sure) 
        // Create it, save it and return it.
        hq = new AdaptiveRevisitHostQueue(hostName, env, catalog, valence);
        hq.setOwner(this);
        
        try{
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry dataEntry = new DatabaseEntry();
            keyBinding.objectToEntry(hostName,keyEntry);
            valueBinding.objectToEntry(new Integer(valence),dataEntry);
            hostNamesDB.put(null,keyEntry,dataEntry);
            AdaptiveRevisitHostQueueWrapper tmp =
                new AdaptiveRevisitHostQueueWrapper(hq);
            hostQueues.put(hostName,tmp);
            sortedHostQueues.add(tmp);
            return hq;
        } catch (DatabaseException e) {
            throw convertDbException(e);
        }
    }
    
    public AdaptiveRevisitHostQueue getTopHQ(){
        AdaptiveRevisitHostQueueWrapper wrapper = 
            (AdaptiveRevisitHostQueueWrapper)sortedHostQueues.first(); 
        return wrapper.hq;
    }

    /**
     * Returns the number of URIs in all the HQs in this list
     * @return the number of URIs in all the HQs in this list
     */
    public long getSize() {
    	long size = 0;
        for (Iterator it = sortedHostQueues.iterator(); it.hasNext();) {
            AdaptiveRevisitHostQueue hq = ((AdaptiveRevisitHostQueueWrapper)it
                    .next()).hq;
            size += hq.getSize();
        }
        return size;
	}
    
    /**
     * Returns the average depth of all the HQs in this list
     * @return the average depth of all the HQs in this list (rounded down)
     */
    public long getAverageDepth() {
    	long size = getSize();
    	return size/hostQueues.size();
    }
    
    /**
     * Returns the size of the largest (deepest) queue.
     * @return the size of the largest (deepest) queue.
     */
    public long getDeepestQueueSize(){
    	long size = 0;
        for (Iterator it = sortedHostQueues.iterator(); it.hasNext();) {
            AdaptiveRevisitHostQueue hq = ((AdaptiveRevisitHostQueueWrapper)it
                    .next()).hq;
            if(hq.getSize() > size){
            	size = hq.getSize();
            }
        }
        return size;
    }
    
    /**
     * Returns the congestion ratio.
     * <p>
     * The congestion ratio is equal to the total number of queues divided
     * by the number of queues currently being processed or are snozzed (i.e. 
     * not ready). A congestion ratio of 1 indicates no congestion.
     * @return the congestion ratio
     */
    public float getCongestionRatio(){
    	int readyQueues = 0;
        for (Iterator it = sortedHostQueues.iterator(); it.hasNext();) {
            AdaptiveRevisitHostQueue hq = ((AdaptiveRevisitHostQueueWrapper)it
                    .next()).hq;
            if(hq.getState() == AdaptiveRevisitHostQueue.HQSTATE_READY){
            	readyQueues++;
            }
        }
        int totalQueues = hostQueues.size();
        
        return (float)(totalQueues) / (totalQueues-readyQueues);
    }
    
    /**
     * This method reorders the host queues. Method is only called by the
     * AdaptiveRevisitHostQueue that it 'owns' when their reported time of next
     * ready is being updated.
     * 
     * @param hq The calling HQ
     */
    protected void reorder(AdaptiveRevisitHostQueue hq){
        // Find the wrapper
        AdaptiveRevisitHostQueueWrapper wrapper = 
            (AdaptiveRevisitHostQueueWrapper)hostQueues.get(hq.getHostName());
        
        long newTime = hq.getNextReadyTime();
        
        if(newTime != wrapper.nextReadyTime){
            // Ok, the time has changed, move the queue around.
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("reorder(" + hq.getHostName() + ") was "
                        + wrapper.nextReadyTime);
            }
            // Remove it from the sorted list
            sortedHostQueues.remove(wrapper);
            // Update the time on the ref.
            wrapper.nextReadyTime = newTime;
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("reorder(" + hq.getHostName() + ") is "
                        + wrapper.nextReadyTime);
            }
            // Readd to the list
            sortedHostQueues.add(wrapper);
        }
    }
    
    /**
     * The total number of URIs queued in all the HQs belonging to this list.
     * 
     * @return total number of URIs queued in all the HQs belonging to this list.
     */
    public long getUriCount(){
        Iterator it = hostQueues.keySet().iterator();
        long count = 0;
        while(it.hasNext()){
            AdaptiveRevisitHostQueueWrapper wrapper =
                (AdaptiveRevisitHostQueueWrapper)it.next();
            count += wrapper.hq.getSize();
        }
        return count;
    }
        
    /**
     * This class wraps an AdaptiveRevisitHostQueue with a fixed value for next
     * ready time.
     * This is done to facilitate sorting by making sure that the value does 
     * not change while the HQ is in the sorted list. With this wrapper, it
     * is possible to remove it from the sorted list, then update the time of
     * next ready, and then readd (resort) it. 
     *
     * @author Kristinn Sigurdsson
     */
    private class AdaptiveRevisitHostQueueWrapper implements Comparable{
        long nextReadyTime;
        AdaptiveRevisitHostQueue hq;
        
        public AdaptiveRevisitHostQueueWrapper(AdaptiveRevisitHostQueue hq){
            nextReadyTime = hq.getNextReadyTime();
            this.hq = hq;
        }
        
        /**
         * Compares the this ARHQWrapper to the supplied one.
         * 
         * @param obj the HQ to compare to. If this object is not an instance
         *             of ARHostQueueWrapper then the method will throw a 
         *             ClassCastException.
         * @return a value less than 0 if this HQWrappers time of next ready
         *         value is less than the argument HQWrappers's; and a value 
         *         greater than 0 if this value is greater. If the time of
         *         next ready is equal, the hostName strings will be compared
         *         and that result returned.
         */
        public int compareTo(Object obj){
            AdaptiveRevisitHostQueueWrapper comp =
                (AdaptiveRevisitHostQueueWrapper)obj;

            long compTime = comp.nextReadyTime;
            
            if(nextReadyTime>compTime){
                return 1;
            } else if(nextReadyTime<compTime){
                return -1;
            } else {
                // Equal time. Use hostnames
                return hq.getHostName().compareTo(comp.hq.getHostName());
            }
        }
    }
    
    /**
     * Closes all HQs and the Environment. 
     */
    public void close(){
        Iterator it = sortedHostQueues.iterator();
        while(it.hasNext()){
            AdaptiveRevisitHostQueue hq =
                ((AdaptiveRevisitHostQueueWrapper)it.next()).hq;
            try {
                hq.close();
            } catch (IOException e) {
                logger.severe("IOException while closing " + hq.getHostName() +
                        "\n" + e.getMessage());
            }
        }
        try {
            hostNamesDB.close();
        } catch (DatabaseException e) {
            logger.severe("IOException while closing hostNamesDB" +
                    "\n" + e.getMessage());
        }
    }
    
    //
    // Reporter implementation
    //
    
    public String[] getReports() {
        // none but default for now
        return new String[] {};
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineReport()
     */
    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void reportTo(PrintWriter writer) {
        Iterator it = sortedHostQueues.iterator();
        while(it.hasNext()){
            AdaptiveRevisitHostQueueWrapper wrapper =
                (AdaptiveRevisitHostQueueWrapper)it.next();
          
            writer.print(wrapper.hq.report(10));
            writer.print("\n\n");
        }
    }
    
    public void reportTo(String name, PrintWriter writer) {
        if(name==null || hostQueues.containsKey(name)==false){
        	reportTo(writer);
        } else {
	    	AdaptiveRevisitHostQueueWrapper wrapper =
	                (AdaptiveRevisitHostQueueWrapper)hostQueues.get(name);
	          
	        writer.print(wrapper.hq.report(0));
	        writer.print("\n\n");
        }
    }
    
    public void singleLineReportTo(PrintWriter writer) {
        Iterator it = sortedHostQueues.iterator();
        int total = 0;
        int ready = 0;
        int snoozed = 0;
        int empty = 0;
        int busy = 0;
        while(it.hasNext()){
            AdaptiveRevisitHostQueueWrapper wrapper =
                (AdaptiveRevisitHostQueueWrapper)it.next();
            total++;
            switch(wrapper.hq.getState()){
                case AdaptiveRevisitHostQueue.HQSTATE_BUSY : busy++; break;
                case AdaptiveRevisitHostQueue.HQSTATE_EMPTY : empty++; break;
                case AdaptiveRevisitHostQueue.HQSTATE_READY : ready++; break;
                case AdaptiveRevisitHostQueue.HQSTATE_SNOOZED : snoozed++; break;
            }
        }
        writer.print(total + " queues: " + ready + " ready, " + snoozed + 
            " snoozed, " + busy + " busy, and " + empty + " empty");
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return "total ready snoozed busy empty";
    }

}