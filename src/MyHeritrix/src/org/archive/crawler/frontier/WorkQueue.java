package org.archive.crawler.frontier;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlSubstats;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Frontier;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;

/**
 * A single queue of related URIs to visit, grouped by a classKey
 * (typically "hostname:port" or similar) 
 * 
 * @author gojomo
 * @author Christian Kohlschuetter 
 */
public abstract class WorkQueue implements Frontier.FrontierGroup, Comparable,
        Serializable, Reporter {
    static final long serialVersionUID = -1939168792663316048L;
    
    private static final Logger logger =
        Logger.getLogger(WorkQueue.class.getName());
    
    /** The classKey */
    protected final String classKey;

    private boolean active = true;

    /** Total number of stored items */
    private long count = 0;

    /** Total number of items ever enqueued */
    private long enqueueCount = 0;
    
    /** Whether queue is already in lifecycle stage */
    private boolean isHeld = false;

    /** Time to wake, if snoozed */
    private long wakeTime = 0;

    /** Running 'budget' indicating whether queue should stay active */
    private int sessionBalance = 0;

    /** Cost of the last item to be charged against queue */
    private int lastCost = 0;

    /** Total number of items charged against queue; with totalExpenditure
     * can be used to calculate 'average cost'. */
    private long costCount = 0;

    /** Running tally of total expenditures out of this queue;
     * tallied only as a URI is finished or retried */
    private long totalExpenditure = 0;

    /** Net cost of all currently-enqueued URIs. */
    private long pendingExpenditure = 0;
    
    /** Total to spend on this queue over its lifetime */
    private long totalBudget = 0;

    /** The next item to be returned */
    private CrawlURI peekItem = null;

    /** Last URI enqueued */
    private String lastQueued;

    /** Last URI peeked */
    private String lastPeeked;

    /** time of last dequeue (disposition of some URI) **/ 
    private long lastDequeueTime;
    
    /** count of errors encountered */
    private long errorCount = 0;
    
    /** Substats for all CrawlURIs in this group */
    protected CrawlSubstats substats = new CrawlSubstats();

    private boolean retired;
    
    public WorkQueue(final String pClassKey) {
        this.classKey = pClassKey;
    }

    /**
     * Delete URIs matching the given pattern from this queue. 
     * @param frontier
     * @param match
     * @return count of deleted URIs
     */
    public long deleteMatching(final WorkQueueFrontier frontier, String match) {
        try {
            final long deleteCount = deleteMatchingFromQueue(frontier, match);
            this.count -= deleteCount;
            return deleteCount;
        } catch (IOException e) {
            //FIXME better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Add the given CrawlURI, noting its addition in running count. (It
     * should not already be present.)
     * 
     * @param frontier Work queues manager.
     * @param curi CrawlURI to insert.
     */
    public synchronized void enqueue(final WorkQueueFrontier frontier,
        CrawlURI curi) {
        try {
            insert(frontier, curi, false);
        } catch (IOException e) {
            //FIXME better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        count++;
        enqueueCount++;
        pendingExpenditure += curi.getHolderCost();
    }

    /**
     * Return the topmost queue item -- and remember it,
     * such that even later higher-priority inserts don't
     * change it. 
     * 
     * TODO: evaluate if this is really necessary
     * @param frontier Work queues manager
     * 
     * @return topmost queue item, or null
     */
    public CrawlURI peek(final WorkQueueFrontier frontier) {
        if(peekItem == null && count > 0) {
            try {
                peekItem = peekItem(frontier);
            } catch (IOException e) {
                //FIXME better exception handling
                logger.log(Level.SEVERE,"peek failure",e);
                e.printStackTrace();
                // throw new RuntimeException(e);
            }
            if(peekItem != null) {
                lastPeeked = peekItem.toString();
            }
        }
        return peekItem;
    }

    /**
     * Remove the peekItem from the queue and adjusts the count.
     * 
     * @param frontier  Work queues manager.
     */
    public synchronized void dequeue(final WorkQueueFrontier frontier) {
        try {
            deleteItem(frontier, peekItem);
        } catch (IOException e) {
            //FIXME better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        pendingExpenditure -= peekItem.getHolderCost();
        unpeek();
        count--;
        lastDequeueTime = System.currentTimeMillis();
    }

    /**
     * Set the session 'activity budget balance' to the given value
     * 
     * @param balance to use
     */
    public void setSessionBalance(int balance) {
        this.sessionBalance = balance;
    }

    /**
     * Return current session 'activity budget balance' 
     * 
     * @return session balance
     */
    public int getSessionBalance() {
        return this.sessionBalance;
    }

    /**
     * Set the total expenditure level allowable before queue is 
     * considered inherently 'over-budget'. 
     * 
     * @param budget
     */
    public void setTotalBudget(long budget) {
        this.totalBudget = budget;
    }

    /**
     * Retrieve the total expenditure level allowed by this queue.
     * 
     * @return the queues total budget
     */
    public long getTotalBudget() {
        return this.totalBudget;
    }
    
    /**
     * Check whether queue has temporarily or permanently exceeded
     * its budget. 
     * 
     * @return true if queue is over its set budget(s)
     */
    public boolean isOverBudget() {
        // check whether running balance is depleted 
        // or totalExpenditure exceeds totalBudget
        return this.sessionBalance <= 0
            || (this.totalBudget >= 0 && this.totalExpenditure >= this.totalBudget);
    }

    /**
     * Return the tally of all expenditures from this queue (dequeued 
     * items)
     * 
     * @return total amount expended on this queue
     */
    public long getTotalExpenditure() {
        return totalExpenditure;
    }
    
    /**
     * Return the tally of all URI costs currently inside this queue
     * 
     * @return total amount expended on this queue
     */
    public long getPendingExpenditure() {
        return pendingExpenditure;
    }

    /**
     * Increase the internal running budget to be used before 
     * deactivating the queue
     * 
     * @param amount amount to increment
     * @return updated budget value
     */
    public int incrementSessionBalance(int amount) {
        this.sessionBalance = this.sessionBalance + amount;
        return this.sessionBalance;
    }

    /**
     * Decrease the internal running budget by the given amount. 
     * @param amount tp decrement
     * @return updated budget value
     */
    public int expend(int amount) {
        this.sessionBalance = this.sessionBalance - amount;
        this.totalExpenditure = this.totalExpenditure + amount;
        this.lastCost = amount;
        this.costCount++;
        return this.sessionBalance;
    }

    /**
     * A URI should not have been charged against queue (eg
     * it was disregarded); return the amount expended 
     * @param amount to return
     * @return updated budget value
     */
    public int refund(int amount) {
        this.sessionBalance = this.sessionBalance + amount;
        this.totalExpenditure = this.totalExpenditure - amount;
        this.costCount--;
        return this.sessionBalance;
    }
    
    /**
     * Note an error and assess an extra penalty. 
     * @param penalty additional amount to deduct
     */
    public void noteError(int penalty) {
        this.sessionBalance = this.sessionBalance - penalty;
        this.totalExpenditure = this.totalExpenditure + penalty;
        errorCount++;
    }
    
    /**
     * @param l
     */
    public void setWakeTime(long l) {
        wakeTime = l;
    }

    /**
     * @return wakeTime
     */
    public long getWakeTime() {
        return wakeTime;
    }

    /**
     * @return classKey, the 'identifier', for this queue.
     */
    public String getClassKey() {
        return this.classKey;
    }

    /**
     * Clear isHeld to false
     */
    public void clearHeld() {
        isHeld = false;
    }

    /**
     * Whether the queue is already in a lifecycle stage --
     * such as ready, in-progress, snoozed -- and thus should
     * not be redundantly inserted to readyClassQueues
     * 
     * @return isHeld
     */
    public boolean isHeld() {
        return isHeld;
    }

    /**
     * Set isHeld to true
     */
    public void setHeld() {
        isHeld = true;
    }

    /**
     * Forgive the peek, allowing a subsequent peek to 
     * return a different item. 
     * 
     */
    public void unpeek() {
        peekItem = null;
    }

    public final int compareTo(Object obj) {
        if(this == obj) {
            return 0; // for exact identity only
        }
        WorkQueue other = (WorkQueue) obj;
        if(getWakeTime() > other.getWakeTime()) {
            return 1;
        }
        if(getWakeTime() < other.getWakeTime()) {
            return -1;
        }
        // at this point, the ordering is arbitrary, but still
        // must be consistent/stable over time
        return this.classKey.compareTo(other.getClassKey());
    }

    /**
     * Update the given CrawlURI, which should already be present. (This
     * is not checked.) Equivalent to an enqueue without affecting the count.
     * 
     * @param frontier Work queues manager.
     * @param curi CrawlURI to update.
     */
    public void update(final WorkQueueFrontier frontier, CrawlURI curi) {
        try {
            insert(frontier, curi, true);
        } catch (IOException e) {
            //FIXME better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Returns the count.
     */
    public synchronized long getCount() {
        return this.count;
    }

    /**
     * Insert the given curi, whether it is already present or not. 
     * @param frontier WorkQueueFrontier.
     * @param curi CrawlURI to insert.
     * @throws IOException
     */
    private void insert(final WorkQueueFrontier frontier, CrawlURI curi, 
            boolean overwriteIfPresent)
        throws IOException {
        insertItem(frontier, curi, overwriteIfPresent);
        lastQueued = curi.toString();
    }

    /**
     * Insert the given curi, whether it is already present or not.
     * Hook for subclasses. 
     * 
     * @param frontier WorkQueueFrontier.
     * @param curi CrawlURI to insert.
     * @throws IOException  if there was a problem while inserting the item
     */
    protected abstract void insertItem(final WorkQueueFrontier frontier,
        CrawlURI curi, boolean expectedPresent) throws IOException;

    /**
     * Delete URIs matching the given pattern from this queue. 
     * @param frontier WorkQueues manager.
     * @param match  the pattern to match
     * @return count of deleted URIs
     * @throws IOException  if there was a problem while deleting
     */
    protected abstract long deleteMatchingFromQueue(
        final WorkQueueFrontier frontier, final String match)
        throws IOException;

    /**
     * Removes the given item from the queue.
     * 
     * This is only used to remove the first item in the queue,
     * so it is not necessary to implement a random-access queue.
     * 
     * @param frontier  Work queues manager.
     * @throws IOException  if there was a problem while deleting the item
     */
    protected abstract void deleteItem(final WorkQueueFrontier frontier,
        final CrawlURI item) throws IOException;

    /**
     * Returns first item from queue (does not delete)
     * 
     * @return The peeked item, or null
     * @throws IOException  if there was a problem while peeking
     */
    protected abstract CrawlURI peekItem(final WorkQueueFrontier frontier)
        throws IOException;

    /**
     * Suspends this WorkQueue. Closes all connections to resources etc.
     * 
     * @param frontier
     * @throws IOException
     */
    protected void suspend(final WorkQueueFrontier frontier) throws IOException {
    }

    /**
     * Resumes this WorkQueue. Eventually opens connections to resources etc.
     * 
     * @param frontier
     * @throws IOException
     */
    protected void resume(final WorkQueueFrontier frontier) throws IOException {
    }

    public void setActive(final WorkQueueFrontier frontier, final boolean b) {
        if(active != b) {
            active = b;
            try {
                if(active) {
                    resume(frontier);
                } else {
                    suspend(frontier);
                }
            } catch (IOException e) {
                //FIXME better exception handling
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    
    // 
    // Reporter
    //

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#getReports()
     */
    public String[] getReports() {
        return new String[] {};
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void reportTo(PrintWriter writer) {
        reportTo(null,writer);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineReportTo(java.io.Writer)
     */
    public void singleLineReportTo(PrintWriter writer) {
        // queue name
        writer.print(classKey);
        writer.print(" ");
        // count of items
        writer.print(Long.toString(count));
        writer.print(" ");
        // enqueue count
        writer.print(Long.toString(enqueueCount));
        writer.print(" ");
        writer.print(sessionBalance);
        writer.print(" ");
        writer.print(lastCost);
        writer.print("(");
        writer.print(ArchiveUtils.doubleToString(
                    ((double) totalExpenditure / costCount), 1));
        writer.print(")");
        writer.print(" ");
        // last dequeue time, if any, or '-'
        if (lastDequeueTime != 0) {
            writer.print(ArchiveUtils.getLog17Date(lastDequeueTime));
        } else {
            writer.print("-");
        }
        writer.print(" ");
        // wake time if snoozed, or '-'
        if (wakeTime != 0) {
            writer.print(ArchiveUtils.formatMillisecondsToConventional(wakeTime - System.currentTimeMillis()));
        } else {
            writer.print("-");
        }
        writer.print(" ");
        writer.print(Long.toString(totalExpenditure));
        writer.print("/");
        writer.print(Long.toString(totalBudget));
        writer.print(" ");
        writer.print(Long.toString(errorCount));
        writer.print(" ");
        writer.print(lastPeeked);
        writer.print(" ");
        writer.print(lastQueued);
        writer.print("\n");
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return "queue currentSize totalEnqueues sessionBalance lastCost " +
                "(averageCost) lastDequeueTime wakeTime " +
                "totalSpend/totalBudget errorCount lastPeekUri lastQueuedUri";
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineReport()
     */
    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }
    
    /**
     * @param writer
     * @throws IOException
     */
    public void reportTo(String name, PrintWriter writer) {
        // name is ignored: only one kind of report for now
        writer.print("Queue ");
        writer.print(classKey);
        writer.print("\n");
        writer.print("  ");
        writer.print(Long.toString(count));
        writer.print(" items");
        if (wakeTime != 0) {
            writer.print("\n   wakes in: "+ArchiveUtils.formatMillisecondsToConventional(wakeTime - System.currentTimeMillis()));
        }
        writer.print("\n    last enqueued: ");
        writer.print(lastQueued);
        writer.print("\n      last peeked: ");
        writer.print(lastPeeked);
        writer.print("\n");
        writer.print("   total expended: ");
        writer.print(Long.toString(totalExpenditure));
        writer.print(" (total budget: ");
        writer.print(Long.toString(totalBudget));
        writer.print(")\n");
        writer.print("   active balance: ");
        writer.print(sessionBalance);
        writer.print("\n   last(avg) cost: ");
        writer.print(lastCost);
        writer.print("(");
        writer.print(ArchiveUtils.doubleToString(
                    ((double) totalExpenditure / costCount), 1));
        writer.print(")\n\n");
    }
    
    public CrawlSubstats getSubstats() {
        return substats;
    }

    /**
     * Set the retired status of this queue.
     * 
     * @param b new value for retired status
     */
    public void setRetired(boolean b) {
        this.retired = b;
    }
    
    public boolean isRetired() {
        return retired;
    }

    public UURI getContextUURI(WorkQueueFrontier wqf) {
        if(lastPeeked!=null) {
            try {
                return UURIFactory.getInstance(lastPeeked);
            } catch (URIException e) {
                // just move along to next try
            }
        }
        if(lastQueued!=null) {
            try {
                return UURIFactory.getInstance(lastQueued);
            } catch (URIException e) {
                // just move along to next try
            }
        }
        if(peekItem!=null) {
            return peekItem.getUURI();
        }
        // peek a CrawlURI temporarily just for context 
        UURI contextUri = peek(wqf).getUURI(); 
        unpeek(); // but don't insist on that URI being next released
        return contextUri;
    }
}
