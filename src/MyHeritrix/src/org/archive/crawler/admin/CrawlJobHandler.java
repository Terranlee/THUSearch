/* CrawlJobHandler
 *
 * $Id: CrawlJobHandler.java 6704 2009-11-25 01:38:55Z gojomo $
 *
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler.admin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.frontier.FrontierJournal;
import org.archive.crawler.settings.ComplexType;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.util.ArchiveUtils;
import org.archive.util.FileUtils;


/**
 * This class manages CrawlJobs. Submitted crawl jobs are queued up and run
 * in order when the crawler is running.
 * <p>Basically this provides a layer between any potential user interface and
 * the CrawlJobs.  It keeps the lists of completed jobs, pending jobs, etc.
 * <p>
 * The jobs managed by the handler can be divided into the following:
 * <ul>
 *  <li> <code>Pending</code> - Jobs that are ready to run and are waiting their
 *                              turn. These can be edited, viewed, deleted etc.
 *  <li> <code>Running</code> - Only one job can be running at a time. There may
 *                              be no job running. The running job can be viewed
 *                              and edited to some extent. It can also be
 *                              terminated. This job should have a
 *                              StatisticsTracking module attached to it for more
 *                              details on the crawl.
 * <li><code>Completed</code> - Jobs that have finished crawling or have been
 *                              deleted from the pending queue or terminated
 *                              while running. They can not be edited but can be
 *                              viewed. They retain the StatisticsTracking
 *                              module from their run.
 *  <li> <code>New job</code> - At any given time their can be one 'new job' the
 *                              new job is not considered ready to run. It can
 *                              be edited or discarded (in which case it will be
 *                              totally destroyed, including any files on disk).
 *                              Once an operator deems the job ready to run it
 *                              can be moved to the pending queue.
 * <li> <code>Profiles</code> - Jobs under profiles are not actual jobs. They can
 *                              be edited normally but can not be submitted to
 *                              the pending queue. New jobs can be created
 *                              using a profile as it's template.
 *
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.admin.CrawlJob
 */

public class CrawlJobHandler implements CrawlStatusListener {
    private static final Logger logger =
        Logger.getLogger(CrawlJobHandler.class.getName());

    /**
     * Name of system property whose specification overrides default profile
     * used.
     *
     */
    public static final String DEFAULT_PROFILE_NAME
        = "heritrix.default.profile";

    /**
     * Default profile name.
     */
    public static final String DEFAULT_PROFILE = "default";
    
    /**
     * Name of the profiles directory.
     */
    public static final String PROFILES_DIR_NAME = "profiles";
    
    public static final String ORDER_FILE_NAME = "order.xml";

    /**
     * Job currently being crawled.
     */
    private CrawlJob currentJob = null;
    
    /**
     * A new job that is being created/configured. Not yet ready for crawling.
     */
    private CrawlJob newJob = null;

	/**
	 * Thread to start the next job in background
     */    
    private Thread startingNextJob = null;

    /**
     * A list of pending CrawlJobs.
     */
    private TreeSet<CrawlJob> pendingCrawlJobs;

    /**
     * A list of completed CrawlJobs.
     */
    //private Vector completedCrawlJobs = new Vector();
    private TreeSet<CrawlJob> completedCrawlJobs;

    /**
     * A list of profile CrawlJobs.
     */
    private TreeSet<CrawlJob> profileJobs;
    
    // The UIDs of profiles should be NOT be timestamps. A descriptive name is
    // ideal.
    private String defaultProfile = null;

    /**
     * If true the crawler is 'running'. That is the next pending job will start
     * crawling as soon as the current job (if any) is completed.
     */
    private boolean running = false;
    
    /**
     * String to indicate recovery should be based on the recovery log, not
     * based on checkpointing.
     */
    public static final String RECOVER_LOG = "recover";
    
    /**
     * Jobs directory.
     */
    private final File jobsDir;
    
    /**
     * Constructor.
     * @param jobsDir Jobs directory.
     */
    public CrawlJobHandler(final File jobsDir) {
        this(jobsDir, true, true);
    }

    /**
     * Constructor allowing for optional loading of profiles and jobs.
     * @param jobsDir Jobs directory.
     * @param loadJobs If true then any applicable jobs will be loaded.
     * @param loadProfiles If true then any applicable profiles will be loaded.
     */
    public CrawlJobHandler(final File jobsDir,
            final boolean loadJobs, final boolean loadProfiles) {
        this.jobsDir = jobsDir;
        // Make a comparator for CrawlJobs.
        Comparator<CrawlJob> comp = new Comparator<CrawlJob>(){
            public int compare(CrawlJob job1, CrawlJob job2) {
                if( job1.getJobPriority() < job2.getJobPriority() ){
                    return -1;
                } else if( job1.getJobPriority() > job2.getJobPriority() ){
                    return 1;
                } else {
                    // Same priority, use UID (which should be a timestamp).
                    // Lower UID (string compare) means earlier time.
                    return job1.getUID().compareTo(job2.getUID());
                }
            }
        };
        this.pendingCrawlJobs = new TreeSet<CrawlJob>(comp);
        this.completedCrawlJobs = new TreeSet<CrawlJob>(comp);
        // Profiles always have the same priority so it will be sorted by name
        this.profileJobs = new TreeSet<CrawlJob>(comp);
        if (loadProfiles){
            loadProfiles();
        }
        if (loadJobs){
            loadJobs();
        }
    }
    
    /**
     * Find the state.job file in the job directory.
     * @param jobDir Directory to look in.
     * @return Full path to 'state.job' file or null if none found.
     */
    protected File getStateJobFile(final File jobDir) {
        // Need to find job file ('state.job').
        File[] jobFiles = jobDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".job") &&
                    (new File(dir, name)).canRead();
            }
            
        });
        return (jobFiles.length == 1)? jobFiles[0]: null;
    }

    /**
     * Loads any availible jobs in the jobs directory.
     * <p>
     * Availible jobs are any directory containing a file called
     * <code>state.job</code>. The file must contain valid job information.
     */
    private void loadJobs() {
        this.jobsDir.mkdirs();
        File[] jobs = this.jobsDir.listFiles();
        for (int i = 0; i < jobs.length; i++) {
            if (jobs[i].isDirectory()) {
                File jobFile = getStateJobFile(jobs[i]);
                if (jobFile != null) {
                    loadJob(jobFile);
                }
            }
        }
    }

    /**
     * Loads a job given a specific job file. The loaded job will be placed in
     * the list of completed jobs or pending queue depending on its status.
     * Running jobs will have their status set to 'finished abnormally' and put
     * into the completed list.
     * @param job The job file of the job to load.
     */
    protected void loadJob(final File job) {
        CrawlJob cjob = null;
        try {
            // Load the CrawlJob
            cjob = new CrawlJob(job, new CrawlJobErrorHandler());
        } catch (InvalidJobFileException e) {
            logger.log(Level.INFO,
                    "Invalid job file for " + job.getAbsolutePath(), e);
            return;
        } catch (IOException e) {
            logger.log(Level.INFO, "IOException for " + job.getName() +
                    ", " + job.getAbsolutePath(), e);
            return;
        }
        
        // TODO: Move test into CrawlJob.
        // Check job status and place it accordingly.
        if (cjob.getStatus().equals(CrawlJob.STATUS_RUNNING)
                || cjob.getStatus().equals(CrawlJob.STATUS_PAUSED)
                || cjob.getStatus().equals(CrawlJob.STATUS_CHECKPOINTING)
                || cjob.getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE) ){
            // Was a running job.
            cjob.setStatus(CrawlJob.STATUS_FINISHED_ABNORMAL);
            this.completedCrawlJobs.add(cjob);
        } else if( cjob.getStatus().equals(CrawlJob.STATUS_PENDING) ) {
            // Was a pending job.
            this.pendingCrawlJobs.add(cjob);
        } else if( cjob.getStatus().equals(CrawlJob.STATUS_CREATED)
                || cjob.getStatus().equals(CrawlJob.STATUS_DELETED) ) {
            // Ignore for now. TODO: Add to 'recycle bin'
        } else {
            // Must have been completed.
            this.completedCrawlJobs.add(cjob);
        }
    }

    /**
     * Looks in conf dir for a profiles dir.
     * @return the directory where profiles are stored else null if none
     * available
     * @throws IOException
     */
    private File getProfilesDirectory() throws IOException {
        URL webappProfilePath = Heritrix.class.getResource("/" + 
            PROFILES_DIR_NAME);
        if (webappProfilePath != null) {
            try {
                return new File(new URI(webappProfilePath.toString()));
            } catch (java.lang.IllegalArgumentException e) {
                // e.g. "profiles" within a jar file
                // try Heritrix.getConfdir() in this case
            } catch (java.net.URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return (Heritrix.getConfdir(false) == null)? null:
            new File(Heritrix.getConfdir().getAbsolutePath(), 
                PROFILES_DIR_NAME);        
    }

    /**
     * Loads the default profile and all other profiles found on disk.
     */
    private void loadProfiles() {
        boolean loadedDefault = false;
        File profileDir = null;
		try {
			profileDir = getProfilesDirectory();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (profileDir != null) {
            File[] ps = profileDir.listFiles();
            if (ps != null && ps.length > 0) {
                for (int i = 0; i < ps.length; i++) {
                    File f = ps[i];
                    if (f.isDirectory()) {
                        // Each directory in the profiles directory should
                        // contain the file order.xml.
                        File profile = new File(f, ORDER_FILE_NAME);
                        if (profile.canRead()) {
                            boolean b = loadProfile(profile);
                            if (b) {
                                loadedDefault = b;
                            }
                        }
                    }
                }
            }
        }
        // Now add in the default profile.  Its on the CLASSPATH and needs
        // special handling.  Don't add if already a default present.
        String parent = File.separator + PROFILES_DIR_NAME + File.separator;
        if (!loadedDefault) {
            loadProfile(new File(parent + DEFAULT_PROFILE, ORDER_FILE_NAME));
        }
        // Look to see if a default profile system property has been
        // supplied. If so, use it instead.
        // TODO: Try and read default profile from some permanent storage.
        defaultProfile = DEFAULT_PROFILE;
    }
    
    /**
     * Load one profile.
     * @param profile Profile to load.
     * @return True if loaded profile was the default profile.
     */
    protected boolean loadProfile(File profile) {
        boolean loadedDefault = false;
        // Ok, got the order file for this profile.
        try {
            // The directory name denotes the profiles UID and name.
            XMLSettingsHandler newSettingsHandler =
                new XMLSettingsHandler(profile);
            CrawlJobErrorHandler cjseh =
                new CrawlJobErrorHandler(Level.SEVERE);
            newSettingsHandler.
                setErrorReportingLevel(cjseh.getLevel());
            newSettingsHandler.initialize();
            addProfile(new CrawlJob(profile.getParentFile().getName(),
                newSettingsHandler, cjseh));
            loadedDefault = profile.getParentFile().getName().
                equals(DEFAULT_PROFILE);
        } catch (InvalidAttributeValueException e) {
            System.err.println("Failed to load profile '" +
                    profile.getParentFile().getName() +
                    "'. InvalidAttributeValueException.");
        }
        return loadedDefault;
    }

    /**
     * Add a new profile
     * @param profile The new profile
     */
    public synchronized void addProfile(CrawlJob profile){
        profileJobs.add(profile);
    }
    
    public synchronized void deleteProfile(CrawlJob cj) throws IOException {
        File d = getProfilesDirectory();
        File p = new File(d, cj.getJobName());
        if (!p.exists()) {
            throw new IOException("No profile named " + cj.getJobName() +
                " at " + d.getAbsolutePath());
        }
        FileUtils.deleteDir(p);
        this.profileJobs.remove(cj);
    }

    /**
     * Returns a List of all known profiles.
     * @return a List of all known profiles.
     */
    public synchronized List<CrawlJob> getProfiles(){
        ArrayList<CrawlJob> tmp = new ArrayList<CrawlJob>(profileJobs.size());
        tmp.addAll(profileJobs);
        return tmp;
    }

    /**
     * Submit a job to the handler. Job will be scheduled for crawling. At
     * present it will not take the job's priority into consideration.
     *
     * @param job A new job for the handler
     * @return CrawlJob that was added or null.
     */
    public CrawlJob addJob(CrawlJob job) {
        if(job.isProfile()){
            return null;     // Can't crawl profiles.
        }
        job.setStatus(CrawlJob.STATUS_PENDING);
        if(job.isNew()){
            // Are adding the new job to the pending queue.
            this.newJob = null;
            job.setNew(false);
        }
        this.pendingCrawlJobs.add(job);
        if(isCrawling() == false && isRunning()) {
            // Start crawling
            startNextJob();
        }
        return job;
    }

    /**
     * Returns the default profile. If no default profile has been set it will
     * return the first profile that was set/loaded and still exists. If no
     * profiles exist it will return null
     * @return the default profile.
     */
    public synchronized CrawlJob getDefaultProfile() {
        if(defaultProfile != null){
            for(Iterator it = profileJobs.iterator(); it.hasNext();) {
                CrawlJob item = (CrawlJob)it.next();
                if(item.getJobName().equals(defaultProfile)){
                    // Found it.
                    return item;
                }
            }
        }
        if(profileJobs.size() > 0){
            return (CrawlJob)profileJobs.first();
        }
        return null;
    }

    /**
     * Set the default profile.
     * @param profile The new default profile. The following must apply to it.
     *                profile.isProfile() should return true and
     *                this.getProfiles() should contain it.
     */
    public void setDefaultProfile(CrawlJob profile) {
        defaultProfile = profile.getJobName();
        // TODO: Make changes to default profile durable across restarts.
    }

    /**
     * A List of all pending jobs
     *
     * @return A List of all pending jobs.
     * No promises are made about the order of the list
     */
    public List<CrawlJob> getPendingJobs() {
        ArrayList<CrawlJob> tmp
         = new ArrayList<CrawlJob>(pendingCrawlJobs.size());
        tmp.addAll(pendingCrawlJobs);
        return tmp;
    }

    /**
     * @return The job currently being crawled.
     */
    public CrawlJob getCurrentJob() {
        return currentJob;
    }

    /**
     * @return A List of all finished jobs.
     */
    public List<CrawlJob> getCompletedJobs() {
        ArrayList<CrawlJob> tmp
         = new ArrayList<CrawlJob>(completedCrawlJobs.size());
        tmp.addAll(completedCrawlJobs);
        return tmp;
    }

    /**
     * Return a job with the given UID.
     * Doesn't matter if it's pending, currently running, has finished running
     * is new or a profile.
     *
     * @param jobUID The unique ID of the job.
     * @return The job with the UID or null if no such job is found
     */
    public CrawlJob getJob(String jobUID) {
        if (jobUID == null){
            return null; // UID can't be null
        }
        // First check currently running job
        if (currentJob != null && currentJob.getUID().equals(jobUID)) {
            return currentJob;
        } else if (newJob != null && newJob.getUID().equals(jobUID)) {
            // Then check the 'new job'
            return newJob;
        } else {
            // Then check pending jobs.
            Iterator itPend = pendingCrawlJobs.iterator();
            while (itPend.hasNext()) {
                CrawlJob cj = (CrawlJob) itPend.next();
                if (cj.getUID().equals(jobUID)) {
                    return cj;
                }
            }

            // Next check completed jobs.
            Iterator itComp = completedCrawlJobs.iterator();
            while (itComp.hasNext()) {
                CrawlJob cj = (CrawlJob) itComp.next();
                if (cj.getUID().equals(jobUID)) {
                    return cj;
                }
            }

            // And finally check the profiles.
            for (Iterator i = getProfiles().iterator(); i.hasNext();) {
                CrawlJob cj = (CrawlJob) i.next();
                if (cj.getUID().equals(jobUID)) {
                    return cj;
                }
            }
        }
        return null; // Nothing found, return null
    }
    
    /**
     * @return True if we terminated a current job (False if no job to
     * terminate)
     */
    public boolean terminateCurrentJob() {
        if (this.currentJob == null) {
            return false;
        }
        // requestCrawlStop will cause crawlEnding to be invoked.
        // It will handle the clean up.
        this.currentJob.stopCrawling();
        synchronized (this) {
            try {
                // Take a few moments so that the controller can change
                // states before the UI updates. The CrawlEnding event
                // will wake us if it occurs sooner than this.
                wait(3000);
            } catch (InterruptedException e) {
                // Ignore.
            }
        }
        return true;
    }

    /**
     * The specified job will be removed from the pending queue or aborted if
     * currently running.  It will be placed in the list of completed jobs with
     * appropriate status info. If the job is already in the completed list or
     * no job with the given UID is found, no action will be taken.
     *
     * @param jobUID The UID (unique ID) of the job that is to be deleted.
     *
     */
    public void deleteJob(String jobUID) {
        // First check to see if we are deleting the current job.
        if (currentJob != null && jobUID.equals(currentJob.getUID())) {
            terminateCurrentJob();
            return; // We're not going to find another job with the same UID
        }
        
        // Ok, it isn't the current job, let's check the pending jobs.
        for(Iterator it = pendingCrawlJobs.iterator(); it.hasNext();) {
            CrawlJob cj = (CrawlJob) it.next();
            if (cj.getUID().equals(jobUID)) {
                // Found the one to delete.
                cj.setStatus(CrawlJob.STATUS_DELETED);
                it.remove();
                return; // We're not going to find another job with the same UID
            }
        }
        
        // And finally the completed jobs.
        for (Iterator it = completedCrawlJobs.iterator(); it.hasNext();) {
            CrawlJob cj = (CrawlJob) it.next();
            if (cj.getUID().equals(jobUID)) {
                // Found the one to delete.
                cj.setStatus(CrawlJob.STATUS_DELETED);
                it.remove();
                return; // No other job will have the same UID
            }
        }
    }

    /**
     * Cause the current job to pause. If no current job is crawling this
     * method will have no effect. 
     */
    public void pauseJob() {
        if (this.currentJob != null) {
            this.currentJob.pause();
        }
    }

    /**
     * Cause the current job to resume crawling if it was paused. Will have no
     * effect if the current job was not paused or if there is no current job.
     * If the current job is still waiting to pause, this will not take effect
     * until the job has actually paused. At which time it will immeditatly
     * resume crawling.
     */
    public void resumeJob() {
        if (this.currentJob != null) {
            this.currentJob.resume();
        }
    }

    /**
     * Cause the current job to write a checkpoint to disk. Currently
     * requires job to already be paused.
     * @throws IllegalStateException Thrown if crawl is not paused.
     */
    public void checkpointJob() throws IllegalStateException {
        if (this.currentJob != null) {
            this.currentJob.checkpoint();
        }
    }

    /**
     * Returns a unique job ID.
     * <p>
     * No two calls to this method (on the same instance of this class) can ever
     * return the same value. <br>
     * Currently implemented to return a time stamp. That is subject to change
     * though.
     *
     * @return A unique job ID.
     *
     * @see ArchiveUtils#TIMESTAMP17
     */
    public String getNextJobUID() {
        return ArchiveUtils.get17DigitDate();
    }

    /**
     * Creates a new job. The new job will be returned and also registered as
     * the handler's 'new job'. The new job will be based on the settings
     * provided but created in a new location on disk.
     *
     * @param baseOn
     *            A CrawlJob (with a valid settingshandler) to use as the
     *            template for the new job.
     * @param recovery Whether to preinitialize new job as recovery of
     * <code>baseOn</code> job.  String holds RECOVER_LOG if we are to
     * do the recovery based off the recover.gz log -- See RecoveryJournal in
     * the frontier package -- or it holds the name of
     * the checkpoint we're to use recoverying.
     * @param name
     *            The name of the new job.
     * @param description
     *            Descriptions of the job.
     * @param seeds
     *            The contents of the new settings' seed file.
     * @param priority
     *            The priority of the new job.
     *
     * @return The new crawl job.
     * @throws FatalConfigurationException If a problem occurs creating the
     *             settings.
     */
    public CrawlJob newJob(CrawlJob baseOn, String recovery, String name,
            String description, String seeds, int priority)
    throws FatalConfigurationException {
        // See what the recover story is.
        File recover = null;
        try {
            if (recovery != null && recovery.length() > 0
                    && recovery.equals(RECOVER_LOG)) {
                // Then we're to do a recovery based off the RecoveryJournal
                // recover.gz log.
                File dir = baseOn.getSettingsHandler().getOrder()
                    .getSettingsDir(CrawlOrder.ATTR_LOGS_PATH);
                // Add name of recover file.  We're hardcoding it as
                // 'recover.gz'.
                recover = new File(dir, FrontierJournal.LOGNAME_RECOVER);
            } else if (recovery != null && recovery.length() > 0) {
                // Must be name of a checkpoint to use.
                recover = new File(baseOn.getSettingsHandler().
                    getOrder().getSettingsDir(CrawlOrder.ATTR_CHECKPOINTS_PATH),
                        recovery);
            }
        } catch (AttributeNotFoundException e1) {
            throw new FatalConfigurationException(
                "AttributeNotFoundException occured while setting up" +
                    "new job/profile " + name + " \n" + e1.getMessage());
        }

        CrawlJob cj = createNewJob(baseOn.getSettingsHandler().getOrderFile(),
            name, description, seeds, priority);
    
        updateRecoveryPaths(recover, cj.getSettingsHandler(), name);
        
        return cj;
    }
    
    /**
     * Creates a new job. The new job will be returned and also registered as
     * the handler's 'new job'. The new job will be based on the settings
     * provided but created in a new location on disk.
     * @param orderFile Order file to use as the template for the new job.
     * @param name The name of the new job.
     * @param description Descriptions of the job.
     * @param seeds The contents of the new settings' seed file.
     *
     * @return The new crawl job.
     * @throws FatalConfigurationException If a problem occurs creating the
     *             settings.
     */
    public CrawlJob newJob(final File orderFile, final String name,
        final String description, final String seeds)
    throws FatalConfigurationException {
        return createNewJob(orderFile, name, description, seeds,
            CrawlJob.PRIORITY_AVERAGE);
    }
    
    protected void checkDirectory(File dir)
    throws FatalConfigurationException {
        if (dir == null) {
            return;
        }
        if (!dir.exists() && !dir.canRead()) {
            throw new FatalConfigurationException(dir.getAbsolutePath() +
                " does not exist or is unreadable");
        }
    }
    
    protected CrawlJob createNewJob(final File orderFile, final String name,
            final String description, final String seeds, final int priority)
    throws FatalConfigurationException {
        if (newJob != null) {
            //There already is a new job. Discard it.
            discardNewJob();
        }
        String UID = getNextJobUID();
        File jobDir;
        jobDir = new File(this.jobsDir, name + "-" + UID);
        CrawlJobErrorHandler errorHandler = new CrawlJobErrorHandler();
        XMLSettingsHandler handler =
            createSettingsHandler(orderFile, name, description,
                seeds, jobDir, errorHandler, "order.xml", "seeds.txt");
        this.newJob = new CrawlJob(UID, name, handler, errorHandler, priority,
                jobDir);
        return this.newJob;
    }

    /**
     * Creates a new profile. The new profile will be returned and also
     * registered as the handler's 'new job'. The new profile will be based on
     * the settings provided but created in a new location on disk.
     *
     * @param baseOn
     *            A CrawlJob (with a valid settingshandler) to use as the
     *            template for the new profile.
     * @param name
     *            The name of the new profile.
     * @param description
     *            Description of the new profile
     * @param seeds
     *            The contents of the new profiles' seed file
     * @return The new profile.
     * @throws FatalConfigurationException
     * @throws IOException
     */
    public CrawlJob newProfile(CrawlJob baseOn, String name, String description,
            String seeds)
    throws FatalConfigurationException, IOException {
        File profileDir = new File(getProfilesDirectory().getAbsoluteFile()
            + File.separator + name);
        CrawlJobErrorHandler cjseh = new CrawlJobErrorHandler(Level.SEVERE);
        CrawlJob newProfile = new CrawlJob(name,
            createSettingsHandler(baseOn.getSettingsHandler().getOrderFile(),
                name, description, seeds, profileDir, cjseh, "order.xml",
                "seeds.txt"), cjseh);
        addProfile(newProfile);
        return newProfile;
    }
    
    /**
     * Creates a new settings handler based on an existing job. Basically all
     * the settings file for the 'based on' will be copied to the specified
     * directory.
     *
     * @param orderFile Order file to base new order file on.  Cannot be null.
     * @param name Name for the new settings
     * @param description Description of the new settings.
     * @param seeds The contents of the new settings' seed file.
     * @param newSettingsDir
     * @param errorHandler
     * @param filename Name of new order file.
     * @param seedfile Name of new seeds file.
     *
     * @return The new settings handler.
     * @throws FatalConfigurationException
     *             If there are problems with reading the 'base on'
     *             configuration, with writing the new configuration or it's
     *             seed file.
     */
    protected XMLSettingsHandler createSettingsHandler(
        final File orderFile, final String name, final String description,
        final String seeds, final File newSettingsDir,
        final CrawlJobErrorHandler errorHandler,
        final String filename, final String seedfile)
    throws FatalConfigurationException {
        XMLSettingsHandler newHandler = null;
        try {
            newHandler = new XMLSettingsHandler(orderFile);
            if(errorHandler != null){
                newHandler.registerValueErrorHandler(errorHandler);
            }
            newHandler.setErrorReportingLevel(errorHandler.getLevel());
            newHandler.initialize();
        } catch (InvalidAttributeValueException e2) {
            throw new FatalConfigurationException(
                "InvalidAttributeValueException occured while creating" +
                " new settings handler for new job/profile\n" +
                e2.getMessage());
        }

        // Make sure the directory exists.
        newSettingsDir.mkdirs();

        try {
            // Set the seed file
            ((ComplexType)newHandler.getOrder().getAttribute("scope"))
                .setAttribute(new Attribute("seedsfile", seedfile));
        } catch (AttributeNotFoundException e1) {
            throw new FatalConfigurationException(
                    "AttributeNotFoundException occured while setting up" +
                    "new job/profile\n" + e1.getMessage());
        } catch (InvalidAttributeValueException e1) {
            throw new FatalConfigurationException(
                    "InvalidAttributeValueException occured while setting" +
                    "up new job/profile\n"  + e1.getMessage());
        } catch (MBeanException e1) {
            throw new FatalConfigurationException(
                    "MBeanException occured while setting up new" +
                    " job/profile\n" + e1.getMessage());
        } catch (ReflectionException e1) {
            throw new FatalConfigurationException(
                    "ReflectionException occured while setting up" +
                    " new job/profile\n" + e1.getMessage());
        }

        File newFile = new File(newSettingsDir.getAbsolutePath(), filename);
        
        try {
            newHandler.copySettings(newFile, (String)newHandler.getOrder()
                .getAttribute(CrawlOrder.ATTR_SETTINGS_DIRECTORY));
        } catch (IOException e3) {
            // Print stack trace to help debug issue where cannot create
            // new job from an old that has overrides.
            e3.printStackTrace();
            throw new FatalConfigurationException(
                    "IOException occured while writing new settings files" +
                    " for new job/profile\n" + e3.getMessage());
        } catch (AttributeNotFoundException e) {
            throw new FatalConfigurationException(
                    "AttributeNotFoundException occured while writing new" +
                    " settings files for new job/profile\n" + e.getMessage());
        } catch (MBeanException e) {
            throw new FatalConfigurationException(
                    "MBeanException occured while writing new settings files" +
                    " for new job/profile\n" + e.getMessage());
        } catch (ReflectionException e) {
            throw new FatalConfigurationException(
                    "ReflectionException occured while writing new settings" +
                    " files for new job/profile\n" + e.getMessage());
        }
        CrawlerSettings orderfile = newHandler.getSettingsObject(null);

        orderfile.setName(name);
        orderfile.setDescription(description);

        if (seeds != null) {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(
                            newHandler.getPathRelativeToWorkingDirectory(seedfile)),
                        "UTF-8"));
                try {
                    writer.write(seeds);
                } finally {
                    writer.close();
                }
            } catch (IOException e) {
                throw new FatalConfigurationException(
                    "IOException occured while writing seed file for new"
                        + " job/profile\n" + e.getMessage());
            }
        }
        return newHandler;
    }
    
    /**
     * @param recover
     *            Source to use recovering. Can be full path to a recovery log
     *            or full path to a checkpoint src dir.
     * @param sh
     *            Settings Handler to update.
     * @param jobName
     *            Name of this job.
     * @throws FatalConfigurationException 
     */
    protected void updateRecoveryPaths(final File recover,
            final SettingsHandler sh, final String jobName)
    throws FatalConfigurationException {
        if (recover == null) {
            return;
        }
        checkDirectory(recover);
        try {
            // Set 'recover-path' to be old job's recovery log path
            updateRecoveryPaths(recover, sh);
        } catch (AttributeNotFoundException e1) {
            throw new FatalConfigurationException(
                    "AttributeNotFoundException occured while setting up"
                            + "new job/profile " + jobName + " \n"
                            + e1.getMessage());
        } catch (InvalidAttributeValueException e1) {
            throw new FatalConfigurationException(
                    "InvalidAttributeValueException occured while setting"
                            + "new job/profile " + jobName + " \n"
                            + e1.getMessage());
        } catch (MBeanException e1) {
            throw new FatalConfigurationException(
                    "MBeanException occured while setting up new"
                            + "new job/profile " + jobName + " \n"
                            + e1.getMessage());
        } catch (ReflectionException e1) {
            throw new FatalConfigurationException(
                    "ReflectionException occured while setting up"
                            + "new job/profile " + jobName + " \n"
                            + e1.getMessage());
        } catch (IOException e) {
            throw new FatalConfigurationException(
                    "IOException occured while setting up" + "new job/profile "
                            + jobName + " \n" + e.getMessage());
        }
    }

    /**
     * @param recover
     *            Source to use recovering. Can be full path to a recovery log
     *            or full path to a checkpoint src dir.
     * @param newHandler
     * @throws ReflectionException
     * @throws MBeanException
     * @throws InvalidAttributeValueException
     * @throws AttributeNotFoundException
     * @throws IOException
     */
    private void updateRecoveryPaths(final File recover,
        SettingsHandler newHandler)
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException, IOException {
        if (recover == null || !recover.exists()) {
            throw new IOException("Recovery src does not exist: " + recover);
        }
        newHandler.getOrder().setAttribute(
            new Attribute(CrawlOrder.ATTR_RECOVER_PATH,
                recover.getAbsolutePath()));
            
        // Now, ensure that 'logs' and 'state' don't overlap with
        // previous job's files (ok for 'arcs' and 'scratch' to overlap)
        File newLogsDisk = null;
        final String RECOVERY_SUFFIX = "-R";
        while(true) {
            try {
                newLogsDisk = newHandler.getOrder().
                    getSettingsDir(CrawlOrder.ATTR_LOGS_PATH);
            } catch (AttributeNotFoundException e) {
                logger.log(Level.SEVERE, "Failed to get logs directory", e);
            }
            if (newLogsDisk.list().length > 0) {
                // 'new' directory is nonempty; rename with trailing '-R'
                String logsPath =  (String) newHandler.getOrder().
                    getAttribute(CrawlOrder.ATTR_LOGS_PATH);
                if(logsPath.endsWith("/")) {
                    logsPath = logsPath.substring(0,logsPath.length()-1);
                }
                newHandler.getOrder().setAttribute(
                    new Attribute(CrawlOrder.ATTR_LOGS_PATH,
                        logsPath + RECOVERY_SUFFIX));
            } else {
                // directory is suitably empty; exit loop
                break;
            }
        }
        File newStateDisk = null;
        while (true) {
            try {
                newStateDisk = newHandler.getOrder().getSettingsDir(
                        CrawlOrder.ATTR_STATE_PATH);
            } catch (AttributeNotFoundException e) {
                logger.log(Level.SEVERE, "Failed to get state directory", e);
            }
            if (newStateDisk.list().length>0) {
                // 'new' directory is nonempty; rename with trailing '-R'
                String statePath =  (String) newHandler.getOrder().
                    getAttribute(CrawlOrder.ATTR_STATE_PATH);
                if(statePath.endsWith("/")) {
                    statePath = statePath.substring(0,statePath.length()-1);
                }
                newHandler.getOrder().setAttribute(
                    new Attribute(CrawlOrder.ATTR_STATE_PATH,
                        statePath + RECOVERY_SUFFIX));
            } else {
                // directory is suitably empty; exit loop
                break;
            }
        }
    }

    /**
     * Discard the handler's 'new job'. This will remove any files/directories
     * written to disk.
     */
    public void discardNewJob(){
        FileUtils.deleteDir(new File(newJob.getSettingsDirectory()));
    }

    /**
     * Get the handler's 'new job'
     * @return the handler's 'new job'
     */
    public CrawlJob getNewJob(){
        return newJob;
    }

    /**
     * Is the crawler accepting crawl jobs to run?
     * @return True if the next availible CrawlJob will be crawled. False otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Is a crawl job being crawled?
     * @return True if a job is actually being crawled (even if it is paused).
     *         False if no job is being crawled.
     */
    public boolean isCrawling() {
        return this.currentJob != null;
    }

    /**
     * Allow jobs to be crawled.
     */
    public void startCrawler() {
        running = true;
        if (pendingCrawlJobs.size() > 0 && isCrawling() == false) {
            // Ok, can just start the next job
            startNextJob();
        }
    }

    /**
     * Stop future jobs from being crawled.
     *
     * This action will not affect the current job.
     */
    public void stopCrawler() {
        running = false;
    }

    /**
     * Start next crawl job.
     *
     * If a is job already running this method will do nothing.
     */
    protected final void startNextJob() {
        synchronized (this) {
            if(startingNextJob != null) {
                try {
                    startingNextJob.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
            startingNextJob = new Thread(new Runnable() {
                public void run() {
                    startNextJobInternal();
                }
            }, "StartNextJob");
            startingNextJob.start();
        }
    }
    
    protected void startNextJobInternal() {
        if (pendingCrawlJobs.size() == 0 || isCrawling()) {
            // No job ready or already crawling.
            return;
        }
        this.currentJob = (CrawlJob)pendingCrawlJobs.first();
        assert pendingCrawlJobs.contains(currentJob) :
            "pendingCrawlJobs is in an illegal state";
        pendingCrawlJobs.remove(currentJob);
        try {
            this.currentJob.setupForCrawlStart();
            // This is ugly but needed so I can clear the currentJob
            // reference in the crawlEnding and update the list of completed
            // jobs.  Also, crawlEnded can startup next job.
            this.currentJob.getController().addCrawlStatusListener(this);
            // now, actually start
            this.currentJob.getController().requestCrawlStart();
        } catch (InitializationException e) {
            loadJob(getStateJobFile(this.currentJob.getDirectory()));
            this.currentJob = null;
            startNextJobInternal(); // Load the next job if there is one.
        }
    }

    /**
     * Forward a 'kick' update to current job if any.
     */
    public void kickUpdate() {
        if(this.currentJob != null) {
            this.currentJob.kickUpdate();
        }
    }

    /**
     * Loads options from a file. Typically these are a list of available
     * modules that can be plugged into some part of the configuration.
     * For examples Processors, Frontiers, Filters etc. Leading and trailing
     * spaces are trimmed from each line.
     * 
     * <p>Options are loaded from the CLASSPATH.
     * @param file the name of the option file (without path!)
     * @return The option file with each option line as a seperate entry in the
     *         ArrayList.
     * @throws IOException when there is trouble reading the file.
     */
    public static ArrayList<String> loadOptions(String file)
    throws IOException {
        ArrayList<String> ret = new ArrayList<String>();
        Enumeration resources = 
            CrawlJob.class.getClassLoader().getResources("modules/" + file);

        boolean noFileFound = true;
        while (resources.hasMoreElements()) {
            InputStream is = ((URL) resources.nextElement()).openStream();
            noFileFound = false;

            String line = null;
            BufferedReader bf =
                new BufferedReader(new InputStreamReader(is), 8192);
            try {
                while ((line = bf.readLine()) != null) {
                    line = line.trim();
                    if(line.indexOf('#')<0 && line.length()>0){
                        // Looks like a valid line.
                        ret.add(line);
                    }
                }
            } finally {
                bf.close();
            }
        }
        
        if (noFileFound) {
            throw new IOException("Failed to get " + file + " from the " +
                " CLASSPATH");
        }

        return ret;
    }

    /**
     * Returns a URIFrontierMarker for the current, paused, job. If there is no
     * current job or it is not paused null will be returned.
     *
     * @param regexpr
     *            A regular expression that each URI must match in order to be
     *            considered 'within' the marker.
     * @param inCacheOnly
     *            Limit marker scope to 'cached' URIs.
     * @return a URIFrontierMarker for the current job.
     * @see #getPendingURIsList(FrontierMarker, int, boolean)
     * @see org.archive.crawler.framework.Frontier#getInitialMarker(String,
     *      boolean)
     * @see org.archive.crawler.framework.FrontierMarker
     */
    public FrontierMarker getInitialMarker(String regexpr,
            boolean inCacheOnly) {
        return (this.currentJob != null)?
                this.currentJob.getInitialMarker(regexpr, inCacheOnly): null;
    }

    /**
     * Returns the frontiers URI list based on the provided marker. This method
     * will return null if there is not current job or if the current job is
     * not paused. Only when there is a paused current job will this method
     * return a URI list.
     *
     * @param marker
     *            URIFrontier marker
     * @param numberOfMatches
     *            maximum number of matches to return
     * @param verbose
     *            should detailed info be provided on each URI?
     * @return the frontiers URI list based on the provided marker
     * @throws InvalidFrontierMarkerException
     *             When marker is inconsistent with the current state of the
     *             frontier.
     * @see #getInitialMarker(String, boolean)
     * @see org.archive.crawler.framework.FrontierMarker
     */
    public ArrayList getPendingURIsList(FrontierMarker marker,
            int numberOfMatches, boolean verbose)
    throws InvalidFrontierMarkerException {
        return (this.currentJob != null)?
           this.currentJob.getPendingURIsList(marker, numberOfMatches, verbose):
           null;
    }

    /**
     * Delete any URI from the frontier of the current (paused) job that match
     * the specified regular expression. If the current job is not paused (or
     * there is no current job) nothing will be done.
     * @param regexpr Regular expression to delete URIs by.
     * @return the number of URIs deleted
     */
    public long deleteURIsFromPending(String regexpr) {
        return deleteURIsFromPending(regexpr, null);
    }
    
    /**
     * Delete any URI from the frontier of the current (paused) job that match
     * the specified regular expression. If the current job is not paused (or
     * there is no current job) nothing will be done.
     * @param uriPattern Regular expression to delete URIs by.
     * @param queuePattern Regular expression of target queues (or null for all)
     * @return the number of URIs deleted
     */
    public long deleteURIsFromPending(String uriPattern, String queuePattern) {
        return (this.currentJob != null)?
                this.currentJob.deleteURIsFromPending(uriPattern,queuePattern): 0;
    }
    
    public String importUris(String file, String style, String force) {
        return importUris(file, style, "true".equals(force));
    }

    /**
     * @param fileOrUrl Name of file w/ seeds.
     * @param style What style of seeds -- crawl log (<code>crawlLog</code>
     * style) or recovery journal (<code>recoveryJournal</code> style), or
     * seeds file style (Pass <code>default</code> style).
     * @param forceRevisit Should we revisit even if seen before?
     * @return A display string that has a count of all added.
     */
    public String importUris(final String fileOrUrl, final String style,
            final boolean forceRevisit) {
        return (this.currentJob != null)?
            this.currentJob.importUris(fileOrUrl, style, forceRevisit): null;
    }
    
    protected int importUris(InputStream is, String style,
            boolean forceRevisit) {
        return (this.currentJob != null)?
                this.currentJob.importUris(is, style, forceRevisit): 0;
    }
    
    /**
     * Schedule a uri.
     * @param uri Uri to schedule.
     * @param forceFetch Should it be forcefetched.
     * @param isSeed True if seed.
     * @throws URIException
     */
    public void importUri(final String uri, final boolean forceFetch,
            final boolean isSeed)
    throws URIException {
        importUri(uri, forceFetch, isSeed, true);
    }
    
    /**
     * Schedule a uri.
     * @param str String that can be: 1. a UURI, 2. a snippet of the
     * crawl.log line, or 3. a snippet from recover log.  See
     * {@link #importUris(InputStream, String, boolean)} for how it subparses
     * the lines from crawl.log and recover.log.
     * @param forceFetch Should it be forcefetched.
     * @param isSeed True if seed.
     * @param isFlush If true, flush the frontier IF it implements
     * flushing.
     * @throws URIException
     */
    public void importUri(final String str, final boolean forceFetch,
            final boolean isSeed, final boolean isFlush)
    throws URIException {
        if (this.currentJob != null) {
            this.currentJob.importUri(str, forceFetch, isSeed, isFlush);
        }
    }
    
    /**
     * If its a HostQueuesFrontier, needs to be flushed for the queued.
     */
    protected void doFlush() {
        if (this.currentJob != null) {
            this.currentJob.flush();
        }
    }
    
    public void stop() {
        if (isCrawling()) {
            deleteJob(getCurrentJob().getUID());
        }
    }
    
    public void requestCrawlStop() {
        if (this.currentJob != null) {
            this.currentJob.stopCrawling();
        }
    }
    
    /**
     * Ensure order file with new name/desc is written.
     * See '[ 1066573 ] sometimes job based-on other job uses older job name'.
     * @param newJob Newly created job.
     * @param metaname Metaname for new job.
     * @param description Description for new job.
     * @return <code>newJob</code>
     */
    public static CrawlJob ensureNewJobWritten(CrawlJob newJob, String metaname,
            String description) {
        XMLSettingsHandler settingsHandler = newJob.getSettingsHandler();
        CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);
        orderfile.setName(metaname);
        orderfile.setDescription(description);
        settingsHandler.writeSettingsObject(orderfile);
        return newJob;
    }

    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
        
    }

    public void crawlEnding(String sExitMessage) {
        loadJob(getStateJobFile(this.currentJob.getDirectory()));
        currentJob = null;
        synchronized (this) {
            // If the GUI terminated the job then it is waiting for this event.
            notifyAll();
        }
    }

    public void crawlEnded(String sExitMessage) {
        if (this.running) {
            startNextJob();
        }
    }

    public void crawlPausing(String statusMessage) {
        // TODO Auto-generated method stub
        
    }

    public void crawlPaused(String statusMessage) {
        // TODO Auto-generated method stub
        
    }

    public void crawlResuming(String statusMessage) {
        // TODO Auto-generated method stub
    }

    public void crawlCheckpoint(File checkpointDir) throws Exception {
        // TODO Auto-generated method stub
    }
}
