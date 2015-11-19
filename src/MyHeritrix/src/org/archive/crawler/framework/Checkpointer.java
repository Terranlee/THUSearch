/* Checkpointer
*
* $Id: Checkpointer.java 4550 2006-08-29 00:19:31Z stack-sf $
*
* Created on Apr 19, 2004
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
package org.archive.crawler.framework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.Checkpoint;
import org.archive.util.ArchiveUtils;

/**
 * Runs checkpointing.
 * Also keeps history of crawl checkpoints  Generally used by CrawlController
 * only but also has static utility methods classes that need to participate in
 * a checkpoint can use.
 *
 * @author gojomo
 * @author stack
 */
public class Checkpointer implements Serializable {
    private static final long serialVersionUID = 7610078446694353173L;

    private final static Logger LOGGER =
        Logger.getLogger(Checkpointer.class.getName());

    private static final String DEFAULT_PREFIX = "";
    
    /**
     * String to prefix any new checkpoint names.
     */
    private  String checkpointPrefix = DEFAULT_PREFIX;
    
    /**
     * Next  overall series checkpoint number.
     */
    private int nextCheckpoint = 1;

    /**
     * All checkpoint names in chain prior to now. May not all still
     * exist on disk.
     */
    private List predecessorCheckpoints = new LinkedList();

    /**
     * If a checkpoint has begun, its directory under
     * <code>checkpointDirectory</code>.
     */
    private transient File checkpointInProgressDir = null;

    /**
     * If the checkpoint in progress has encountered fatal errors.
     */
    private transient boolean checkpointErrors = false;
    
    /**
     * checkpointThread is set if a checkpoint is currently running.
     */
    private transient Thread checkpointThread = null;
    
    private transient CrawlController controller;
    
    /**
     * Setup in constructor or on a call to revovery.
     */
    private transient Timer timerThread = null;
    
    public static final DecimalFormat INDEX_FORMAT = new DecimalFormat("00000");

    /**
     * Create a new CheckpointContext with the given store directory
     * @param cc CrawlController instance thats hosting this Checkpointer.
     * @param checkpointDir Where to store checkpoint.
     */
    public Checkpointer(final CrawlController cc, final File checkpointDir) {
        this(cc, DEFAULT_PREFIX);
    }
    
    /**
     * Create a new CheckpointContext with the given store directory
     *
     * @param cc CrawlController instance thats hosting this Checkpointer.
     * @param prefix Prefix for checkpoint label.
     */
    public Checkpointer(final CrawlController cc, final String prefix) {
        super();
        initialize(cc, prefix);
        
    }
    
    protected void initialize(final CrawlController cc, final String prefix) {
        this.controller = cc;
        this.checkpointPrefix = prefix;
        // Period is in hours.
        int period = Integer.parseInt(System.getProperties().getProperty(
            this.getClass().getName() + ".period", "-1"));
        if (period <= 0) {
            return;
        }
        // Convert period from hours to milliseconds.
        long periodMs = period * (1000 * 60 * 60);
        TimerTask tt = new TimerTask() {
            private CrawlController cController = cc;
            public void run() {
                if (isCheckpointing()) {
                    LOGGER.info("CheckpointTimerThread skipping checkpoint, " +
                        "already checkpointing: State: " +
                        this.cController.getState());
                    return;
                }
                LOGGER.info("TimerThread request checkpoint");
                this.cController.requestCrawlCheckpoint();
            }
        };
        this.timerThread = new Timer(true);
        this.timerThread.schedule(tt, periodMs, periodMs);
        LOGGER.info("Installed Checkpoint TimerThread to checkpoint every " +
            period + " hour(s).");
    }
    
    void cleanup() {
        if (this.timerThread != null) {
            LOGGER.info("Cleanedup Checkpoint TimerThread.");
            this.timerThread.cancel();
        }
    }
    
    /**
     * @return Returns the nextCheckpoint index.
     */
    public int getNextCheckpoint() {
        return this.nextCheckpoint;
    }

    /**
     * Run a checkpoint of the crawler.
     */
    public void checkpoint() {
        String name = "Checkpoint-" + getNextCheckpointName();
        this.checkpointThread = new CheckpointingThread(name);
        this.checkpointThread.setDaemon(true);
        this.checkpointThread.start();
    }

    /**
     * Thread to run the checkpointing.
     * @author stack
     */
    public class CheckpointingThread extends Thread {
        public CheckpointingThread(final String name) {
            super(name);
        }

        public CrawlController getController() {
        	return Checkpointer.this.controller;
        }
        
        public void run() {
            LOGGER.info("Started");
            // If crawler already paused, don't resume crawling after
            // finishing checkpointing.
            final boolean alreadyPaused = getController().isPaused() ||
                getController().isPausing();
            try {
                getController().requestCrawlPause();
                // Clear any checkpoint errors.
                setCheckpointErrors(false);
                if (!waitOnPaused()) {
                    checkpointFailed("Failed wait for complete pause.");
                } else {
                    createCheckpointInProgressDirectory();
                    this.getController().checkpoint();
                }
            } catch (Exception e) {
                checkpointFailed(e);
            } finally {
                if (!isCheckpointErrors()) {
                    writeValidity();
                }
                Checkpointer.this.nextCheckpoint++;
                clearCheckpointInProgressDirectory();
                LOGGER.info("Finished");
                getController().completePause();
                if (!alreadyPaused) {
                    getController().requestCrawlResume();
                }
            }
        }
        
        private synchronized boolean waitOnPaused() {
            // If we're paused we can exit but also exit if the crawl has been
            // resumed by the operator.
            while(!getController().isPaused() && !getController().isRunning()) {
                try {
                    wait(1000 * 3);
                } catch (InterruptedException e) {
                    // May be for us.
                }
            }
            return getController().isPaused();
        }
    }
    
    protected File createCheckpointInProgressDirectory() {
        this.checkpointInProgressDir =
            new File(Checkpointer.this.controller.getCheckpointsDisk(),
                getNextCheckpointName());
        this.checkpointInProgressDir.mkdirs();
        return this.checkpointInProgressDir;
    }
    
    protected void clearCheckpointInProgressDirectory() {
        this.checkpointInProgressDir = null;
    }
    
    protected CrawlController getController() {
        return this.controller;
    }
    
    /**
     * @return next checkpoint name (zero-padding string).
     */
    public String getNextCheckpointName() {
        return formatCheckpointName(this.checkpointPrefix, this.nextCheckpoint);
    }
    
    public static String formatCheckpointName(final String prefix,
    		final int index) {
    	return prefix + INDEX_FORMAT.format(index);
    }

    protected void writeValidity() {
        File valid = new File(this.checkpointInProgressDir,
            Checkpoint.VALIDITY_STAMP_FILENAME);
        try {
            FileOutputStream fos = new FileOutputStream(valid);
            fos.write(ArchiveUtils.get14DigitDate().getBytes());
            fos.close();
        } catch (IOException e) {
            valid.delete();
        }
    }

    /**
     * @return Checkpoint directory. Name of the directory is the name of this
     * current checkpoint.  Null if no checkpoint in progress.
     */
    public File getCheckpointInProgressDirectory() {
        return this.checkpointInProgressDir;
    }
    
    /**
     * @return True if a checkpoint is in progress.
     */
    public boolean isCheckpointing() {
        return this.checkpointThread != null && this.checkpointThread.isAlive();
    }

    /**
     * Note that a checkpoint failed
     *
     * @param e Exception checkpoint failed on.
     */
    protected void checkpointFailed(Exception e) {
        LOGGER.log(Level.WARNING, " Checkpoint failed", e);
        checkpointFailed();
    }
    
    protected void checkpointFailed(final String message) {
        LOGGER.warning(message);
        checkpointFailed();
    }
    
    protected void checkpointFailed() {
        this.checkpointErrors = true;
    }
    
    /**
     * @return True if current/last checkpoint failed.
     */
    public boolean isCheckpointFailed() {
        return this.checkpointErrors;
    }

    /**
     * @return Return whether this context is at a new crawl, never-
     * checkpointed state.
     */
    public boolean isAtBeginning() {
        return nextCheckpoint == 1;
    }

    /**
     * Call when recovering from a checkpoint.
     * Call this after instance has been revivifyied post-serialization to
     * amend counters and directories that effect where checkpoints get stored
     * from here on out.
     * @param cc CrawlController instance.
     */
    public void recover(final CrawlController cc) {
        // Prepend the checkpoint name with a little 'r' so we tell apart
        // checkpoints made from a recovery.  Allow for there being
        // multiple 'r' prefixes.
        initialize(cc, 'r' + this.checkpointPrefix);
    }
    
    /**
     * @return Returns the predecessorCheckpoints.
     */
    public List getPredecessorCheckpoints() {
        return this.predecessorCheckpoints;
    }

    protected boolean isCheckpointErrors() {
        return this.checkpointErrors;
    }

    protected void setCheckpointErrors(boolean checkpointErrors) {
        this.checkpointErrors = checkpointErrors;
    }
}
