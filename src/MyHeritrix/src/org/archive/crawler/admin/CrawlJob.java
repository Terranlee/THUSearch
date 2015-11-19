/* CrawlJob
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.SimpleType;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.IOUtils;
import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.Checkpoint;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.StatisticsTracking;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.frontier.AbstractFrontier;
import org.archive.crawler.settings.ComplexType;
import org.archive.crawler.settings.ModuleAttributeInfo;
import org.archive.crawler.settings.TextField;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.crawler.util.CheckpointUtils;
import org.archive.crawler.util.IoUtils;
import org.archive.util.ArchiveUtils;
import org.archive.util.FileUtils;
import org.archive.util.JEMBeanHelper;
import org.archive.util.JmxUtils;
import org.archive.util.iterator.LineReadingIterator;
import org.archive.util.iterator.RegexpLineIterator;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;

/**
 * A CrawlJob encapsulates a 'crawl order' with any and all information and
 * methods needed by a CrawlJobHandler to accept and execute them.
 *
 * <p>A given crawl job may also be a 'profile' for a crawl. In that case it
 * should not be executed as a crawl but can be edited and used as a template
 * for creating new CrawlJobs.
 *
 * <p>All of it's constructors are protected since only a CrawlJobHander
 * should construct new CrawlJobs.
 *
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.admin.CrawlJobHandler#newJob(CrawlJob, String,
 * String, String, String, int)
 * @see org.archive.crawler.admin.CrawlJobHandler#newProfile(CrawlJob,
 *  String, String, String)
 */

public class CrawlJob extends NotificationBroadcasterSupport
implements DynamicMBean, MBeanRegistration, CrawlStatusListener, Serializable {
    /**
     * Eclipse generated serial number.
     */
    private static final long serialVersionUID = 3411161000452525856L;
    
    private static final Logger logger =
        Logger.getLogger(CrawlJob.class.getName());
    /*
     * Possible values for Priority
     */
    /** lowest */
    public static final int PRIORITY_MINIMAL = 0;
    /** low */
    public static final int PRIORITY_LOW = 1;
    /** average */
    public static final int PRIORITY_AVERAGE = 2;
    /** high */
    public static final int PRIORITY_HIGH = 3;
    /** highest */
    public static final int PRIORITY_CRITICAL = 4;

    /*
     * Possible states for a Job.
     */
    /** Inital value. May not be ready to run/incomplete. */
    public static final String STATUS_CREATED = "Created";
    /** Job has been successfully submitted to a CrawlJobHandler */
    public static final String STATUS_PENDING = "Pending";
    /** Job is being crawled */
    public static final String STATUS_RUNNING = "Running";
    /** Job was deleted by user, will not be displayed in UI. */
    public static final String STATUS_DELETED = "Deleted";
    /** Job was terminted by user input while crawling */
    public static final String STATUS_ABORTED = "Finished - Ended by operator";
    /** Something went very wrong */
    public static final String STATUS_FINISHED_ABNORMAL =
        "Finished - Abnormal exit from crawling";
    /** Job finished normally having completed its crawl. */
    public static final String STATUS_FINISHED = "Finished";
    /** Job finished normally when the specified timelimit was hit. */
    public static final String STATUS_FINISHED_TIME_LIMIT =
        "Finished - Timelimit hit";
    /** Job finished normally when the specifed amount of 
     * data (MB) had been downloaded */
    public static final String STATUS_FINISHED_DATA_LIMIT =
        "Finished - Maximum amount of data limit hit";
    /** Job finished normally when the specified number of documents had been
     * fetched.
     */
    public static final String STATUS_FINISHED_DOCUMENT_LIMIT =
        "Finished - Maximum number of documents limit hit";
    /** Job is going to be temporarly stopped after active threads are finished. */
    public static final String STATUS_WAITING_FOR_PAUSE = "Pausing - " +
        "Waiting for threads to finish";
    /** Job was temporarly stopped. State is kept so it can be resumed */
    public static final String STATUS_PAUSED = "Paused";
    /**
     * Job is being checkpointed.  When finished checkpointing, job is set
     * back to STATUS_PAUSED (Job must be first paused before checkpointing
     * will run).
     */
    public static final String STATUS_CHECKPOINTING = "Checkpointing";
    /** Job could not be launced due to an InitializationException */
    public static final String STATUS_MISCONFIGURED = "Could not launch job " +
        "- Fatal InitializationException";
    /** Job is actually a profile */
    public static final String STATUS_PROFILE = "Profile";
    
    public static final String STATUS_PREPARING = "Preparing";

    // Class variables
    private String UID;       //A UID issued by the CrawlJobHandler.
    private String name;
    private String status;
    private boolean isReadOnly = false;
    private boolean isNew = true;
    private boolean isProfile = false;
    private boolean isRunning = false;
    private int priority;
    private int numberOfJournalEntries = 0;
    
    private String statisticsFileSave = "";

    private String errorMessage = null;

    private File jobDir = null;

    private transient CrawlJobErrorHandler errorHandler = null;

    protected transient XMLSettingsHandler settingsHandler;
    
    private transient CrawlController controller = null;
    
    public static final String RECOVERY_JOURNAL_STYLE = "recoveryJournal";
    public static final String CRAWL_LOG_STYLE = "crawlLog";
    
    // OpenMBean support.

    /**
     * Server we registered with. Maybe null.
     */
    private transient MBeanServer mbeanServer = null;
    private transient ObjectName mbeanName = null;
    public static final String CRAWLJOB_JMXMBEAN_TYPE =
        JmxUtils.SERVICE + ".Job";
    private transient JEMBeanHelper bdbjeMBeanHelper = null;
    private transient List<String> bdbjeAttributeNameList = null;
    private transient List<String> bdbjeOperationsNameList = null;
    
    
    /**
     * The MBean we've registered ourselves with (May be null
     * throughout life of Heritrix).
     */
    private transient OpenMBeanInfoSupport openMBeanInfo;
    
    public static final String NAME_ATTR = "Name";
    public static final String UID_ATTR = "UID";
    public static final String STATUS_ATTR = "Status";
    public static final String FRONTIER_SHORT_REPORT_ATTR =
        "FrontierShortReport";
    public static final String THREADS_SHORT_REPORT_ATTR =
        "ThreadsShortReport";
    public static final String TOTAL_DATA_ATTR = "TotalData";
    public static final String CRAWL_TIME_ATTR = "CrawlTime";
    public static final String DOC_RATE_ATTR = "DocRate";
    public static final String CURRENT_DOC_RATE_ATTR = "CurrentDocRate";
    public static final String KB_RATE_ATTR = "KbRate";
    public static final String CURRENT_KB_RATE_ATTR = "CurrentKbRate";
    public static final String THREAD_COUNT_ATTR = "ThreadCount";
    public static final String DOWNLOAD_COUNT_ATTR = "DownloadedCount";
    public static final String DISCOVERED_COUNT_ATTR = "DiscoveredCount";
    public static final String [] ATTRIBUTE_ARRAY = {NAME_ATTR, UID_ATTR,
        STATUS_ATTR, FRONTIER_SHORT_REPORT_ATTR, THREADS_SHORT_REPORT_ATTR,
        TOTAL_DATA_ATTR, CRAWL_TIME_ATTR, DOC_RATE_ATTR,
        CURRENT_DOC_RATE_ATTR, KB_RATE_ATTR, CURRENT_KB_RATE_ATTR,
        THREAD_COUNT_ATTR, DOWNLOAD_COUNT_ATTR, DISCOVERED_COUNT_ATTR};
    public static final List ATTRIBUTE_LIST = Arrays.asList(ATTRIBUTE_ARRAY);
    
    public static final String IMPORT_URI_OPER = "importUri";
    public static final String IMPORT_URIS_OPER = "importUris";
    public static final String DUMP_URIS_OPER = "dumpUris";
    public static final String PAUSE_OPER = "pause";
    public static final String RESUME_OPER = "resume";
    public static final String FRONTIER_REPORT_OPER = "frontierReport";
    public static final String THREADS_REPORT_OPER = "threadsReport";
    public static final String SEEDS_REPORT_OPER = "seedsReport";
    public static final String CHECKPOINT_OPER = "startCheckpoint";
    public static final String PROGRESS_STATISTICS_OPER =
        "progressStatistics";
    public static final String PROGRESS_STATISTICS_LEGEND_OPER =
        "progressStatisticsLegend";
    
    public static final String PROG_STATS = "progressStatistics";
    
    // Same as JEMBeanHelper.OP_DB_STAT
    public static final String OP_DB_STAT = "getDatabaseStats";
    
    /**
     * Don't add the following crawl-order items.
     */
    public static final List ORDER_EXCLUDE;
    static {
        ORDER_EXCLUDE = Arrays.asList(new String [] {"bdb-cache-percent",
            "extract-processors", "DNS", "uri-included-structure"});
    }
    
    /**
     * Sequence number for jmx notifications.
     */
    private static int notificationsSequenceNumber = 1;
    
    /**
     * A shutdown Constructor.
     */
    protected CrawlJob() {
        super();
    }

    /**
     * A constructor for jobs.
     *
     * <p> Create, ready to crawl, jobs.
     * @param UID A unique ID for this job. Typically emitted by the
     *            CrawlJobHandler.
     * @param name The name of the job
     * @param settingsHandler The associated settings
     * @param errorHandler The crawl jobs settings error handler.
     *           <tt>null</tt> means none is set
     * @param priority job priority.
     * @param dir The directory that is considered this jobs working directory.
     */
    public CrawlJob(final String UID,
            final String name, final XMLSettingsHandler settingsHandler,
            final CrawlJobErrorHandler errorHandler, final int priority,
            final File dir) {
        this(UID, name, settingsHandler, errorHandler,
                priority, dir, null, false, true);
    }

    /**
     * A constructor for profiles.
     *
     * <p> Any job created with this constructor will be
     * considered a profile. Profiles are not stored on disk (only their
     * settings files are stored on disk). This is because their data is
     * predictible given any settings files.
     * @param UIDandName A unique ID for this job. For profiles this is the same
     *           as name
     * @param settingsHandler The associated settings
     * @param errorHandler The crawl jobs settings error handler.
     *           <tt>null</tt> means none is set
     */
    protected CrawlJob(final String UIDandName,
            final XMLSettingsHandler settingsHandler,
            final CrawlJobErrorHandler errorHandler) {
        this(UIDandName, UIDandName, settingsHandler, errorHandler,
            PRIORITY_AVERAGE, null, STATUS_PROFILE, true, false);
    }
    
    public CrawlJob(final String UID,
            final String name, final XMLSettingsHandler settingsHandler,
            final CrawlJobErrorHandler errorHandler, final int priority,
            final File dir, final String status, final boolean isProfile,
            final boolean isNew) {
        super();
        this.UID = UID;
        this.name = name;
        this.settingsHandler = settingsHandler;
        this.errorHandler = errorHandler;
        this.status = status;
        this.isProfile = isProfile;
        this.isNew = isNew;
        this.jobDir = dir;
        this.priority = priority;
    }

    /**
     * A constructor for reloading jobs from disk. Jobs (not profiles) have
     * their data written to persistent storage in the file system. This method
     * is used to load the job from such storage. This is done by the
     * <code>CrawlJobHandler</code>.
     * <p>
     * Proper structure of a job file (TODO: Maybe one day make this an XML file)
     * Line 1. UID <br>
     * Line 2. Job name (string) <br>
     * Line 3. Job status (string) <br>
     * Line 4. is job read only (true/false) <br>
     * Line 5. is job running (true/false) <br>
     * Line 6. job priority (int) <br>
     * Line 7. number of journal entries <br>
     * Line 8. setting file (with path) <br>
     * Line 9. statistics tracker file (with path) <br>
     * Line 10-?. error message (String, empty for null), can be many lines <br>
     * @param jobFile
     *            a file containing information about the job to load.
     * @param errorHandler The crawl jobs settings error handler.
     *            null means none is set
     * @throws InvalidJobFileException
     *            if the specified file does not refer to a valid job file.
     * @throws IOException
     *            if io operations fail
     */
    protected CrawlJob(final File jobFile,
            final CrawlJobErrorHandler errorHandler)
            throws InvalidJobFileException, IOException {
        this(null, null, null, errorHandler,
                PRIORITY_AVERAGE, null, null, false, true);
        this.jobDir = jobFile.getParentFile();
        
        // Check for corrupt job.state files (can be corrupt if we crash).
        if (jobFile.length() == 0) {
            throw new InvalidJobFileException(jobFile.getCanonicalPath() +
                " is corrupt (length is zero)");
        }
        
        // Open file. Read data and set up class variables accordingly...
        BufferedReader jobReader =
            new BufferedReader(new FileReader(jobFile), 4096);
        // UID
        this.UID = jobReader.readLine();
        // name
        this.name = jobReader.readLine();
        // status
        this.status = jobReader.readLine();
        if(status.equals(STATUS_ABORTED)==false
                && status.equals(STATUS_CREATED)==false
                && status.equals(STATUS_DELETED)==false
                && status.equals(STATUS_FINISHED)==false
                && status.equals(STATUS_FINISHED_ABNORMAL)==false
                && status.equals(STATUS_FINISHED_DATA_LIMIT)==false
                && status.equals(STATUS_FINISHED_DOCUMENT_LIMIT)==false
                && status.equals(STATUS_FINISHED_TIME_LIMIT)==false
                && status.equals(STATUS_MISCONFIGURED)==false
                && status.equals(STATUS_PAUSED)==false
                && status.equals(STATUS_CHECKPOINTING)==false
                && status.equals(STATUS_PENDING)==false
                && status.equals(STATUS_RUNNING)==false
                && status.equals(STATUS_WAITING_FOR_PAUSE)==false
                && status.equals(STATUS_PREPARING)==false){
            // status is invalid. Must be one of the above
            throw new InvalidJobFileException("Status (line 3) in job file " +
                    "is not valid: '" + status + "'");
        }
        // isReadOnly
        String tmp = jobReader.readLine();
        if(tmp.equals("true")){
            isReadOnly = true;
        } else if(tmp.equals("false")){
            isReadOnly = false;
        } else {
            throw new InvalidJobFileException("isReadOnly (line 4) in job" +
                    " file '" + jobFile.getAbsolutePath() + "' is not " +
                    "valid: '" + tmp + "'");
        }
        // isRunning
        tmp = jobReader.readLine();
        if(tmp.equals("true")){
            this.isRunning = true;
        } else if(tmp.equals("false")){
            this.isRunning = false;
        } else {
            throw new InvalidJobFileException("isRunning (line 5) in job " +
                    "file '" + jobFile.getAbsolutePath() + "' is not valid: " +
                    "'" + tmp + "'");
        }
        // priority
        tmp = jobReader.readLine();
        try{
            this.priority = Integer.parseInt(tmp);
        } catch(NumberFormatException e){
            throw new InvalidJobFileException("priority (line 5) in job " +
                    "file '" + jobFile.getAbsolutePath() + "' is not valid: " +
                    "'" + tmp + "'");
        }
        // numberOfJournalEntries
        tmp = jobReader.readLine();
        try{
            this.numberOfJournalEntries = Integer.parseInt(tmp);
        } catch(NumberFormatException e){
            throw new InvalidJobFileException("numberOfJournalEntries " +
                    "(line 5) in job file '" + jobFile.getAbsolutePath() +
                    "' is not valid: " + "'" + tmp + "'");
        }
        // settingsHandler
        tmp = jobReader.readLine();
        try {
            File f = new File(tmp);
            this.settingsHandler = new XMLSettingsHandler((f.isAbsolute())?
                f: new File(jobDir, f.getName()));
            if(this.errorHandler != null){
                this.settingsHandler.registerValueErrorHandler(errorHandler);
            }
            this.settingsHandler.initialize();
        } catch (InvalidAttributeValueException e1) {
            throw new InvalidJobFileException("Problem reading from settings " +
                    "file (" + tmp + ") specified in job file '" +
                    jobFile.getAbsolutePath() + "'\n" + e1.getMessage());
        }
        // Statistics tracker.
        jobReader.readLine();
        // errorMessage
        // TODO: Multilines
        tmp = jobReader.readLine();
        errorMessage = "";
        while(tmp!=null){
            errorMessage+=tmp+'\n';
            tmp = jobReader.readLine();
        }
        if(errorMessage.length()==0){
            // Empty error message should be null
            errorMessage = null;
        }
        // TODO: Load stattrack if needed.

        // TODO: This should be inside a finally block.
        jobReader.close();
    }

    /**
     * Cause the job to be written to persistent storage.
     * This will also save the statistics tracker if it is not null and the
     * job status is finished (regardless of how it's finished)
     */
    private void writeJobFile() {
        if (isProfile) {
            return;
        }
        
        final String jobDirAbsolute = jobDir.getAbsolutePath();
        if (!jobDir.exists() || !jobDir.canWrite()) {
            logger.warning("Can't update status on " +
                jobDirAbsolute + " because file does not" +
                " exist (or is unwriteable)");
            return;
        }
        File f = new File(jobDirAbsolute, "state.job");

        String settingsFile = getSettingsDirectory();
        // Make settingsFile's path relative if order.xml is somewhere in the
        // job's directory tree
        if(settingsFile.startsWith(jobDirAbsolute.concat(File.separator))) {
            settingsFile = settingsFile.substring(jobDirAbsolute.length()+1);
        }
        try {
            OutputStreamWriter jobWriter = 
                new OutputStreamWriter(
                    new FileOutputStream(f, false),
                    "UTF-8");
            try {
                jobWriter.write(UID + "\n");
                jobWriter.write(name + "\n");
                jobWriter.write(status + "\n");
                jobWriter.write(isReadOnly + "\n");
                jobWriter.write(isRunning + "\n");
                jobWriter.write(priority + "\n");
                jobWriter.write(numberOfJournalEntries + "\n");
                jobWriter.write(settingsFile + "\n");
                jobWriter.write(statisticsFileSave + "\n");// TODO: Is this
                                                            // right?
                // Can be multiple lines so we keep it last
                if (errorMessage != null) {
                    jobWriter.write(errorMessage + "\n");
                }
            } finally {
                if (jobWriter != null) {
                    jobWriter.close();
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "An IOException occured saving job " +
                    name + " (" + UID + ")", e);
        }
    }
  
    /**
     * Returns this jobs unique ID (UID) that was issued by the
     * CrawlJobHandler() when this job was first created.
     * 
     * @return Job This jobs UID.
     * @see CrawlJobHandler#getNextJobUID()
     */
    public String getUID(){
        return UID;
    }

    /**
     * Returns this job's 'name'. The name comes from the settings for this job,
     * need not be unique and may change. For a unique identifier use
     * {@link #getUID() getUID()}.
     * <p>
     * The name corrisponds to the value of the 'name' tag in the 'meta' section
     * of the settings file.
     *
     * @return This job's 'name'
     */
    public String getJobName(){
        return name;
    }

    /**
     * Return the combination of given name and UID most commonly
     * used in administrative interface.
     *
     * @return Job's name with UID notation
     */
    public String getDisplayName() {
        return getJobName()+" ["+getUID()+"]";
    }

    /**
     * Set this job's level of priority.
     *
     * @param priority The level of priority
     *
     * @see #getJobPriority()
     * @see #PRIORITY_MINIMAL
     * @see #PRIORITY_LOW
     * @see #PRIORITY_AVERAGE
     * @see #PRIORITY_HIGH
     * @see #PRIORITY_CRITICAL
     */
    public void setJobPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Get this job's level of priority.
     *
     * @return this job's priority
     * @see #setJobPriority(int)
     * @see #PRIORITY_MINIMAL
     * @see #PRIORITY_LOW
     * @see #PRIORITY_AVERAGE
     * @see #PRIORITY_HIGH
     * @see #PRIORITY_CRITICAL
     */
    public int getJobPriority() {
        return priority;
    }

    /**
     * Once called no changes can be made to the settings for this job.
     * Typically this is done once a crawl is completed and further changes
     * to the crawl order are therefor meaningless.
     */
    public void setReadOnly() {
        isReadOnly = true;
        writeJobFile(); //Save changes
    }

    /**
     * Is job read only?
     * @return false until setReadOnly has been invoked, after that it returns true.
     */
    public boolean isReadOnly(){
        return isReadOnly;
    }

    /**
     * Set the status of this CrawlJob.
     *
     * @param status Current status of CrawlJob
     *         (see constants defined here beginning with STATUS)
     */
    public void setStatus(String status) {
        this.status = status;
        writeJobFile(); //Save changes
        // TODO: If job finished, save StatisticsTracker!
    }

    /**
     * @return Status of the crawler (Used by JMX).
     */
    public String getCrawlStatus() {
        return this.controller != null?
            this.controller.getState().toString(): "Illegal State";
    }
    
    /**
     * Get the current status of this CrawlJob
     *
     * @return The current status of this CrawlJob
     *         (see constants defined here beginning with STATUS)
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Returns the settings handler for this job. It will have been initialized.
     * @return the settings handler for this job.
     */
    public XMLSettingsHandler getSettingsHandler() {
        return this.settingsHandler;
    }
    /**
     * Is this a new job?
     * @return True if is new.
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Set if the job is considered to be a profile
     * @return True if is a profile.
     */
    public boolean isProfile() {
        return isProfile;
    }

    /**
     * Set if the job is considered a new job or not.
     * @param b Is the job considered to be new.
     */
    public void setNew(boolean b) {
        isNew = b;
        writeJobFile(); //Save changes
    }

    /**
     * Returns true if the job is being crawled.
     * @return true if the job is being crawled
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Set if job is being crawled.
     * @param b Is job being crawled.
     */
    protected void setRunning(boolean b) {
        isRunning = b;
        writeJobFile(); // Save changes
        //TODO: Job ending -> Save statistics tracker.
        //TODO: This is likely to happen as the CrawlEnding event occurs,
        // need to ensure that the StatisticsTracker is saved to disk on
        // CrawlEnded. Maybe move responsibility for this into the
        // StatisticsTracker?
    }
    
    protected void unregisterMBean() {
        // Unregister current job from JMX agent, if there one.
        if (this.mbeanServer == null) {
            return;
        }
        try {
            this.mbeanServer.unregisterMBean(this.mbeanName);
            this.mbeanServer = null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed with " + this.mbeanName, e);
        }
    }
    
    /**
     * Subclass of crawlcontroller that unregisters beans when stopped.
     * Done as subclass so CrawlController doesn't get any JMX (or 'CrawlJob')
     * pollution, so for sure CrawlJob is unregistered with JMX and so any
     * listeners on the CrawlJob get a chance to get crawl ended message
     * (These latter notifications may not actually be getting through -- TBD).
     * <p>TODO: This override dirtys the data model since CC knows about CJs.
     * The facility provided by this class emitting events and statistics so
     * they can be read by JMX needs to go back into CC.  Probably best to
     * registering in JMX the CC, rather than CJ.  Lets do this in Heritrix 2.0
     * since means changing the JMX API some.
     */
    public class MBeanCrawlController extends CrawlController
    implements Serializable {
        private static final long serialVersionUID = -4608537998168407222L;
        private CrawlJob cj = null;
        private CompositeType ct =  null;
        
        public CrawlJob getCrawlJob() {
            return this.cj;
        }

        public void setCrawlJob(CrawlJob cj) {
            this.cj = cj;
        }
        
        @SuppressWarnings("unchecked")
        public void progressStatisticsEvent(final EventObject e) {
            super.progressStatisticsEvent(e);
            if (this.cj.getMbeanName() == null) {
                // Can be null around job startup.  Return w/o doing anything.
                return;
            }
                
            Map s = ((StatisticsTracking)e.getSource()).getProgressStatistics();
            // Convert the statistics to OpenType CompositeData and add as
            // user data to Notification.
            CompositeData cd = null;
            try {
                if (this.ct == null) {
                    this.ct = JmxUtils.createCompositeType(s, PROG_STATS,
                        PROG_STATS + " for " + this.cj.getMbeanName());
                }
                cd = new CompositeDataSupport(this.ct, s);
            } catch (OpenDataException ode) {
                ode.printStackTrace();
            }
            if (cd != null) {
                Notification n = new Notification(PROG_STATS,
                    this.cj.getMbeanName(), getNotificationsSequenceNumber(),
                    ((StatisticsTracking)e.getSource()).
                        getProgressStatisticsLine());
                n.setUserData(cd);
                this.cj.sendNotification(n);
            }
        }
        
        protected void completeStop() {
            try {
                super.completeStop();
            } finally {
                if (this.cj != null) {
                    this.cj.unregisterMBean();
                }
                this.cj = null;
            }
        }
    }
    
    protected CrawlController setupCrawlController()
    throws InitializationException {
        CrawlController controller = null;
        
        // Check if we're to do a checkpoint recover.  If so, deserialize
        // the checkpoint's CrawlController and use that in place of a new
        // CrawlController instance.
        Checkpoint cp = CrawlController.
            getCheckpointRecover(getSettingsHandler().getOrder());
        if (cp != null) {
            try {
            	controller = (MBeanCrawlController)CheckpointUtils.
                    readObjectFromFile(MBeanCrawlController.class,
                        cp.getDirectory());
            } catch (FileNotFoundException e) {
                throw new InitializationException(e);
            } catch (IOException e) {
                throw new InitializationException(e);
            } catch (ClassNotFoundException e) {
                throw new InitializationException(e);
            }
        } else {
        	controller = new MBeanCrawlController();
        }
        return controller;
    }
    
    protected CrawlController createCrawlController() {
    	return new MBeanCrawlController();
    }
    
    public void setupForCrawlStart()
    throws InitializationException {
        try {
        	this.controller = setupCrawlController();
            // Register as listener to get job finished notice.
            this.controller.addCrawlStatusListener(this);
            this.controller.initialize(getSettingsHandler());
            // Set the crawl job this MBeanCrawlController needs to worry about.
            ((MBeanCrawlController)this.controller).setCrawlJob(this);
            // Create our mbean description and register our crawljob.
            this.openMBeanInfo = buildMBeanInfo();
            try {
                Heritrix.registerMBean(this, getJmxJobName(),
                    CRAWLJOB_JMXMBEAN_TYPE);
            } catch (InstanceAlreadyExistsException e) {
                throw new InitializationException(e);
            } catch (MBeanRegistrationException e) {
                throw new InitializationException(e);
            } catch (NotCompliantMBeanException e) {
                throw new InitializationException(e);
            }
        } catch (InitializationException e) {
            // Can't load current job since it is misconfigured.
            setStatus(CrawlJob.STATUS_MISCONFIGURED);
            setErrorMessage("A fatal InitializationException occured when "
                    + "loading job:\n" + e.getMessage());
            // Log to stdout so its seen in logs as well as in UI.
            e.printStackTrace();
            this.controller = null;
            throw e;
        }
        setStatus(CrawlJob.STATUS_RUNNING);
        setRunning(true);
    }
    
    public void stopCrawling() {
        if(this.controller != null) {
            this.controller.requestCrawlStop();
        }
    }

    /**
     * @return One-line Frontier report.
     */
    public String getFrontierOneLine() {
        if (this.controller == null || this.controller.getFrontier() == null) {
            return "Crawler not running";
        }
        return this.controller.getFrontier().singleLineReport();
    }
    
    /**
     * @param reportName Name of report to write.
     * @return A report of the frontier's status.
     */
    public String getFrontierReport(final String reportName) {
        if (this.controller == null || this.controller.getFrontier() == null) {
            return "Crawler not running";
        }
        return ArchiveUtils.writeReportToString(this.controller.getFrontier(),
                reportName);
    }
    
    /**
     * Write the requested frontier report to the given PrintWriter
     * @param reportName Name of report to write.
     * @param writer Where to write to.
     */
    public void writeFrontierReport(String reportName, PrintWriter writer) {
        if (this.controller == null || this.controller.getFrontier() == null) {
            writer.println("Crawler not running.");
            return;
        }
        this.controller.getFrontier().reportTo(reportName,writer);
    }

    /**
     * @return One-line threads report.
     */
    public String getThreadOneLine() {
        if (this.controller == null) {
            return "Crawler not running";
        }
        return this.controller.oneLineReportThreads();
    }
    
    /**
     * Get the CrawlControllers ToeThreads report for the running crawl.
     * @return The CrawlControllers ToeThreads report
     */
    public String getThreadsReport() {
        if (this.controller == null) {
            return "Crawler not running";
        }
        return ArchiveUtils.writeReportToString(this.controller.getToePool(),
                null);
    }
    
    /**
     * Write the requested threads report to the given PrintWriter
     * @param reportName Name of report to write.
     * @param writer Where to write to.
     */
    public void writeThreadsReport(String reportName, PrintWriter writer) {
        if (this.controller == null || this.controller.getFrontier() == null) {
            writer.println("Crawler not running.");
            return;
        }
        this.controller.getToePool().reportTo(reportName, writer);
    }
    
    /**
     * Kills a thread. For details see
     * {@link org.archive.crawler.framework.ToePool#killThread(int, boolean)
     * ToePool.killThread(int, boolean)}.
     * @param threadNumber Thread to kill.
     * @param replace Should thread be replaced.
     * @see org.archive.crawler.framework.ToePool#killThread(int, boolean)
     */
    public void killThread(int threadNumber, boolean replace) {
        if (this.controller ==  null) {
            return;
        }
        this.controller.killThread(threadNumber, replace);
    }

    /**
     * Get the Processors report for the running crawl.
     * @return The Processors report for the running crawl.
     */
    public String getProcessorsReport() {
        if (this.controller == null) {
            return "Crawler not running";
        }
        return ArchiveUtils.writeReportToString(this.controller,
                CrawlController.PROCESSORS_REPORT);
    }
    
    /**
     * Returns the directory where the configuration files for this job are
     * located.
     *
     * @return the directory where the configuration files for this job are
     *         located
     */
    public String getSettingsDirectory() {
        return settingsHandler.getOrderFile().getPath();
    }

    /**
     * Returns the path of the job's base directory. For profiles this is always
     * equal to <code>new File(getSettingsDirectory())</code>.
     * @return the path of the job's base directory.
     */
    public File getDirectory(){
        return isProfile? new File(getSettingsDirectory()): jobDir;
    }

    /**
     * Get the error message associated with this job. Will return null if there
     * is no error message.
     * @return the error message associated with this job
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set an error message for this job. Generally this only occurs if the job
     * is misconfigured.
     * @param string the error message associated with this job
     */
    public void setErrorMessage(String string) {
        errorMessage = string;
        writeJobFile(); //Save changes
    }

    /**
     * @return Returns the number of journal entries.
     */
    public int getNumberOfJournalEntries() {
        return numberOfJournalEntries;
    }

    /**
     * @param numberOfJournalEntries The number of journal entries to set.
     */
    public void setNumberOfJournalEntries(int numberOfJournalEntries) {
        this.numberOfJournalEntries = numberOfJournalEntries;
        writeJobFile();
    }

    /**
     * @return Returns the error handler for this crawl job
     */
    public CrawlJobErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Read all the checkpoints found in the job's checkpoints
     * directory into Checkpoint instances
     * @return Collection containing list of all checkpoints.
     */
    public Collection scanCheckpoints() {
        File checkpointsDirectory =
            settingsHandler.getOrder().getCheckpointsDirectory();
        File[] perCheckpointDirs = checkpointsDirectory.listFiles();
        Collection<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
        if (perCheckpointDirs != null) {
            for (int i = 0; i < perCheckpointDirs.length; i++) {
                Checkpoint cp = new Checkpoint(perCheckpointDirs[i]);
                checkpoints.add(cp);
            }
        }
        return checkpoints;
    }

    /**
     * Returns the absolute path of the specified log.
     * Note: If crawl has not begun, this file may not exist.
     * @param log
     * @return the absolute path for the specified log.
     * @throws AttributeNotFoundException
     * @throws ReflectionException
     * @throws MBeanException
     */
    public String getLogPath(String log) 
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        String logsPath = (String)settingsHandler.getOrder().
            getAttribute(CrawlOrder.ATTR_LOGS_PATH);
        CrawlOrder order = settingsHandler.getOrder();
        String diskPath = (String) order.getAttribute(null,
            CrawlOrder.ATTR_DISK_PATH);
        File disk = settingsHandler.
            getPathRelativeToWorkingDirectory(diskPath);
        File f = new File(logsPath, log);
        if (!f.isAbsolute()) {
            f = new File(disk.getPath(), f.getPath());
        }
        return f.getAbsolutePath();
    }

    // OpenMBean implementation.
    
    protected void pause() {
        if (this.controller != null && this.controller.isPaused() == false) {
            this.controller.requestCrawlPause();
        }
    }
    
    protected void resume() {
        if (this.controller != null) {
            this.controller.requestCrawlResume();
        }
    }

    /**
     * @throws IllegalStateException Thrown if crawl is not paused.
     */
    protected void checkpoint() throws IllegalStateException {
        if (this.controller != null) {
            this.controller.requestCrawlCheckpoint();
        }
    }
    
    /**
     * @return True if checkpointing.
     */
    public boolean isCheckpointing() {
        return this.controller != null? this.controller.isCheckpointing(): false;
    }
    
    /**
     * If its a HostQueuesFrontier, needs to be flushed for the queued.
     */
    protected void flush() {
        // Nothing to do.
    }

    /**
     * Delete any URI from the frontier of the current (paused) job that match
     * the specified regular expression. If the current job is not paused (or
     * there is no current job) nothing will be done.
     * @param regexpr Regular expression to delete URIs by.
     * @return the number of URIs deleted
     */
    public long deleteURIsFromPending(String regexpr){
        return deleteURIsFromPending(regexpr,null);
    }
    
    /**
     * Delete any URI from the frontier of the current (paused) job that match
     * the specified regular expression. If the current job is not paused (or
     * there is no current job) nothing will be done.
     * @param regexpr Regular expression to delete URIs by.
     * @return the number of URIs deleted
     */
    public long deleteURIsFromPending(String uriPattern, String queuePattern){
        return (this.controller != null &&
                this.controller.getFrontier() != null &&
                this.controller.isPaused())?
            this.controller.getFrontier().deleteURIs(uriPattern,queuePattern): 0;
    }
    
    public String importUris(String file, String style, String force) {
        return importUris(file, style, "true".equals(force));
    }
    
    public String importUris(final String fileOrUrl, final String style,
            final boolean forceRevisit) {
        return importUris(fileOrUrl, style, forceRevisit, false);
    }

    /**
     * @param fileOrUrl Name of file w/ seeds.
     * @param style What style of seeds -- crawl log, recovery journal, or
     * seeds file.
     * @param forceRevisit Should we revisit even if seen before?
     * @param areSeeds Is the file exclusively seeds?
     * @return A display string that has a count of all added.
     */
    public String importUris(final String fileOrUrl, final String style,
            final boolean forceRevisit, final boolean areSeeds) {
        InputStream is =
            IoUtils.getInputStream(this.controller.getDisk(), fileOrUrl);
        String message = null;
        // Do we have an inputstream?
        if (is == null) {
            message = "Failed to get inputstream from " + fileOrUrl;
            logger.severe(message);
        } else {
            int addedCount = importUris(is, style, forceRevisit, areSeeds);
            message = Integer.toString(addedCount) + " URIs added from " +
                fileOrUrl;
        }
        return message;
    }
    
    protected int importUris(InputStream is, String style,
            boolean forceRevisit) {
        return importUris(is, style, forceRevisit, false);
    }
    
    /**
     * Import URIs.
     * @param is Stream to use as URI source.
     * @param style Style in which URIs are rendored.  Currently support for
     * <code>recoveryJournal</code>, <code>crawlLog</code>, and seeds file
     * format (i.e <code>default</code>) where <code>default</code> style is
     * a UURI per line (comments allowed).
     * @param forceRevisit Whether we should revisit this URI even if we've
     * visited it previously.
     * @param areSeeds Are the imported URIs seeds?
     * @return Count of added URIs.
     */
    protected int importUris(InputStream is, String style,
            boolean forceRevisit, final boolean areSeeds) {
        // Figure the regex to use parsing each line of input stream.
        String extractor;
        String output;
        if(CRAWL_LOG_STYLE.equals(style)) {
            // Skip first 3 fields
            extractor = "\\S+\\s+\\S+\\s+\\S+\\s+(\\S+\\s+\\S+\\s+\\S+\\s+).*";
            output = "$1";
        } else if (RECOVERY_JOURNAL_STYLE.equals(style)) {
            // Skip the begin-of-line directive
            extractor = "\\S+\\s+((\\S+)(?:\\s+\\S+\\s+\\S+)?)\\s*";
            output = "$1";
        } else {
            extractor =
                RegexpLineIterator.NONWHITESPACE_ENTRY_TRAILING_COMMENT;
            output = RegexpLineIterator.ENTRY;
        }
        
        controller.installThreadContextSettingsHandler();
        
        // Read the input stream.
        BufferedReader br = null;
        int addedCount = 0;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            Iterator iter = new RegexpLineIterator(new LineReadingIterator(br),
                RegexpLineIterator.COMMENT_LINE, extractor, output);
            while(iter.hasNext()) {
                try {
                    importUri((String)iter.next(), forceRevisit, areSeeds,
                        false);
                    addedCount++;
                } catch (URIException e) {
                    e.printStackTrace();
                }
            }
            br.close();
            flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addedCount;
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
        CandidateURI caUri = CandidateURI.fromString(str);
        caUri.setForceFetch(forceFetch);
        if (isSeed) {
            caUri.setIsSeed(isSeed);
            if (caUri.getVia() == null || caUri.getVia().length() <= 0) {
                // Danger of double-add of seeds because of this code here.
                // Only call addSeed if no via.  If a via, the schedule will
                // take care of updating scope.
                this.controller.getScope().addSeed(caUri);
            }
        }
        this.controller.getFrontier().schedule(caUri);
        if (isFlush) {
            flush();
        }
    }
    
    
    /**
     * @return Our mbean info (Needed for CrawlJob to qualify as a
     * DynamicMBean).
     */
    public MBeanInfo getMBeanInfo() {
        return this.openMBeanInfo;
    }
    
    /**
     * Build up the MBean info for Heritrix main.
     * @return Return created mbean info instance.
     * @throws InitializationException 
     */
    protected OpenMBeanInfoSupport buildMBeanInfo()
    throws InitializationException {
        // Start adding my attributes.
        List<OpenMBeanAttributeInfo> attributes
         = new ArrayList<OpenMBeanAttributeInfo>();

        // Attributes.
        attributes.add(new OpenMBeanAttributeInfoSupport(NAME_ATTR,
            "Crawl job name", SimpleType.STRING, true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(STATUS_ATTR,
            "Short basic status message", SimpleType.STRING, true, false,
            false));
        attributes.add(
                new OpenMBeanAttributeInfoSupport(FRONTIER_SHORT_REPORT_ATTR,
                "Short frontier report", SimpleType.STRING, true,
                false, false));
        attributes.add(
                new OpenMBeanAttributeInfoSupport(THREADS_SHORT_REPORT_ATTR,
                "Short threads report", SimpleType.STRING, true,
                false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(UID_ATTR,
            "Crawl job UID", SimpleType.STRING, true, false, false));  
        attributes.add(new OpenMBeanAttributeInfoSupport(TOTAL_DATA_ATTR,
            "Total data received", SimpleType.LONG, true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(CRAWL_TIME_ATTR,
            "Crawl time", SimpleType.LONG, true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(CURRENT_DOC_RATE_ATTR,
            "Current crawling rate (Docs/sec)", SimpleType.DOUBLE,
            true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(CURRENT_KB_RATE_ATTR,
            "Current crawling rate (Kb/sec)", SimpleType.LONG,
            true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(THREAD_COUNT_ATTR,
            "Active thread count", SimpleType.INTEGER, true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(DOC_RATE_ATTR,
            "Crawling rate (Docs/sec)", SimpleType.DOUBLE,
            true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(KB_RATE_ATTR,
            "Current crawling rate (Kb/sec)", SimpleType.LONG,
            true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(DOWNLOAD_COUNT_ATTR,
            "Count of downloaded documents", SimpleType.LONG,
            true, false, false));
        attributes.add(new OpenMBeanAttributeInfoSupport(DISCOVERED_COUNT_ATTR,
            "Count of discovered documents", SimpleType.LONG,
            true, false, false));
        
        // Add in the crawl order attributes.
        addCrawlOrderAttributes(this.getController().getOrder(), attributes);
        
        // Add the bdbje attributes.  Convert to open mbean attributes.
        // First do bdbeje setup.  Then add a subset of the bdbje attributes.
        // Keep around the list of names as a convenience for when it comes
        // time to test if attribute is supported.
        Environment env = this.controller.getBdbEnvironment();
        try {
            this.bdbjeMBeanHelper =
                new JEMBeanHelper(env.getConfig(), env.getHome(), true);
        } catch (DatabaseException e) {
            e.printStackTrace();
            InitializationException ie =
                new InitializationException(e.getMessage());
            ie.setStackTrace(e.getStackTrace());
            throw ie;
        }
        this.bdbjeAttributeNameList = Arrays.asList(new String [] {
                JEMBeanHelper.ATT_ENV_HOME,
                JEMBeanHelper.ATT_OPEN,
                JEMBeanHelper.ATT_IS_READ_ONLY,
                JEMBeanHelper.ATT_IS_TRANSACTIONAL,
                JEMBeanHelper.ATT_CACHE_SIZE,
                JEMBeanHelper.ATT_CACHE_PERCENT,
                JEMBeanHelper.ATT_LOCK_TIMEOUT,
                JEMBeanHelper.ATT_IS_SERIALIZABLE,
                JEMBeanHelper.ATT_SET_READ_ONLY,
        });
        addBdbjeAttributes(attributes,
                this.bdbjeMBeanHelper.getAttributeList(env),
                this.bdbjeAttributeNameList);

        // Operations.
        List<OpenMBeanOperationInfo> operations
         = new ArrayList<OpenMBeanOperationInfo>();
        OpenMBeanParameterInfo[] args = new OpenMBeanParameterInfoSupport[3];
        args[0] = new OpenMBeanParameterInfoSupport("url",
            "URL to add to the frontier", SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("forceFetch",
            "True if URL is to be force fetched", SimpleType.BOOLEAN);
        args[2] = new OpenMBeanParameterInfoSupport("seed",
            "True if URL is a seed", SimpleType.BOOLEAN);
        operations.add(new OpenMBeanOperationInfoSupport(IMPORT_URI_OPER,
            "Add passed URL to the frontier", args, SimpleType.VOID,
                MBeanOperationInfo.ACTION));
        
        args = new OpenMBeanParameterInfoSupport[4];
        args[0] = new OpenMBeanParameterInfoSupport("pathOrUrl",
            "Path or URL to file of URLs", SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("style",
            "Format format:default|crawlLog|recoveryJournal",
            SimpleType.STRING);
        args[2] = new OpenMBeanParameterInfoSupport("forceFetch",
            "True if URLs are to be force fetched", SimpleType.BOOLEAN);
        args[3] = new OpenMBeanParameterInfoSupport("seed",
            "True if all content are seeds.", SimpleType.BOOLEAN);
        operations.add(new OpenMBeanOperationInfoSupport(IMPORT_URIS_OPER,
            "Add file of passed URLs to the frontier", args, SimpleType.STRING,
                MBeanOperationInfo.ACTION));
        
        
        args = new OpenMBeanParameterInfoSupport[4];
        args[0] = new OpenMBeanParameterInfoSupport("filename",
                "File to print to", SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("regexp",
                "Regular expression URLs must match", SimpleType.STRING);
        args[2] = new OpenMBeanParameterInfoSupport("numberOfMatches",
                "Maximum number of matches to return", SimpleType.INTEGER);
        args[3] = new OpenMBeanParameterInfoSupport("verbose",
                "Should they be verbose descriptions", SimpleType.BOOLEAN);
        operations.add(new OpenMBeanOperationInfoSupport(DUMP_URIS_OPER,
                "Dump pending URIs from frontier to a file", args,
                SimpleType.VOID, MBeanOperationInfo.ACTION));
        
        operations.add(new OpenMBeanOperationInfoSupport(PAUSE_OPER,
            "Pause crawling (noop if already paused)", null, SimpleType.VOID,
            MBeanOperationInfo.ACTION));
        
        operations.add(new OpenMBeanOperationInfoSupport(RESUME_OPER,
            "Resume crawling (noop if already resumed)", null,
            SimpleType.VOID, MBeanOperationInfo.ACTION));
        
        args = new OpenMBeanParameterInfoSupport[1];
        args[0] = new OpenMBeanParameterInfoSupport("name",
            "Name of report ('all', 'standard', etc.).", SimpleType.STRING);
        operations.add(new OpenMBeanOperationInfoSupport(FRONTIER_REPORT_OPER,
             "Full frontier report", args, SimpleType.STRING,
             MBeanOperationInfo.INFO));
        
        operations.add(new OpenMBeanOperationInfoSupport(THREADS_REPORT_OPER,
             "Full thread report", null, SimpleType.STRING,
             MBeanOperationInfo.INFO));
        
        operations.add(new OpenMBeanOperationInfoSupport(SEEDS_REPORT_OPER,
             "Seeds report", null, SimpleType.STRING, MBeanOperationInfo.INFO));  
 
        operations.add(
                new OpenMBeanOperationInfoSupport(PROGRESS_STATISTICS_OPER,
                "Progress statistics at time of invocation", null,
                SimpleType.STRING, MBeanOperationInfo.INFO)); 
        
        operations.add(new OpenMBeanOperationInfoSupport(
            PROGRESS_STATISTICS_LEGEND_OPER,
                "Progress statistics legend", null,
                SimpleType.STRING, MBeanOperationInfo.INFO));  
        
        operations.add(new OpenMBeanOperationInfoSupport(CHECKPOINT_OPER,
                "Start a checkpoint", null, SimpleType.VOID,
                MBeanOperationInfo.ACTION));
                
        // Add bdbje operations. Add subset only. Keep around the list so have
        // it to hand when figuring what operations are supported. Usual actual
        // Strings because not accessible from JEMBeanHelper.
        this.bdbjeOperationsNameList = Arrays.asList(new String[] { "cleanLog",
                "evictMemory", "checkpoint", "sync",
                "getEnvironmentStatsToString", "getLockStatsToString",
                "getDatabaseNames", OP_DB_STAT
        });
        addBdbjeOperations(operations,
                this.bdbjeMBeanHelper.getOperationList(env),
                this.bdbjeOperationsNameList);
        
        // Register notifications
        List<MBeanNotificationInfo> notifications
         = new ArrayList<MBeanNotificationInfo>();
        notifications.add(
            new MBeanNotificationInfo(new String [] {"crawlStarted",
                    "crawlEnding", "crawlPaused", "crawlResuming", PROG_STATS},
                this.getClass().getName() + ".notifications",
                "CrawlStatusListener events and progress statistics as " +
                    "notifications"));
        MBeanNotificationInfo [] notificationsArray =
            new MBeanNotificationInfo[notifications.size()];
        notifications.toArray(notificationsArray);
        
        // Build the info object.
        OpenMBeanAttributeInfoSupport[] attributesArray =
            new OpenMBeanAttributeInfoSupport[attributes.size()];
        attributes.toArray(attributesArray);
        OpenMBeanOperationInfoSupport[] operationsArray =
            new OpenMBeanOperationInfoSupport[operations.size()];
        operations.toArray(operationsArray);
        return new OpenMBeanInfoSupport(this.getClass().getName(),
            "Current Crawl Job as OpenMBean",
            attributesArray,
            new OpenMBeanConstructorInfoSupport [] {},
            operationsArray,
            notificationsArray);
    }
    
    protected void addBdbjeAttributes(
            final List<OpenMBeanAttributeInfo> attributes,
            final List<MBeanAttributeInfo> bdbjeAttributes, 
            final List<String> bdbjeNamesToAdd) {
        for (MBeanAttributeInfo info: bdbjeAttributes) {
            if (bdbjeNamesToAdd.contains(info.getName())) {
                attributes.add(JmxUtils.convertToOpenMBeanAttribute(info));
            }
        }   
    }
    
    protected void addBdbjeOperations(
            final List<OpenMBeanOperationInfo> operations,
            final List<MBeanOperationInfo> bdbjeOperations, 
            final List<String> bdbjeNamesToAdd) {
        for (MBeanOperationInfo info: bdbjeOperations) {
            if (bdbjeNamesToAdd.contains(info.getName())) {
                OpenMBeanOperationInfo omboi = null;
                if (info.getName().equals(OP_DB_STAT)) {
                    // Db stats needs special handling. The published
                    // signature is wrong and its return type is awkward.
                    // Handle it.
                    omboi = JmxUtils.convertToOpenMBeanOperation(info, null,
                        SimpleType.STRING);
                    MBeanParameterInfo[] params = omboi.getSignature();
                    OpenMBeanParameterInfo[] args =
                        new OpenMBeanParameterInfoSupport[params.length + 1];
                    for (int ii = 0; ii < params.length; ii++) {
                        args[ii] = (OpenMBeanParameterInfo) params[ii];
                    }
                    args[params.length] = new OpenMBeanParameterInfoSupport(
                            "name", "Database name", SimpleType.STRING);
                    omboi = new OpenMBeanOperationInfoSupport(omboi.getName(),
                        omboi.getDescription(), args, omboi.getReturnOpenType(),
                        omboi.getImpact());
                } else {
                    omboi = JmxUtils.convertToOpenMBeanOperation(info);
                }
                operations.add(omboi);
            }
        }
    }
    
    protected void addCrawlOrderAttributes(final ComplexType type,
            final List<OpenMBeanAttributeInfo> attributes) {
        for (final Iterator i = type.getAttributeInfoIterator(null);
                i.hasNext();) {
            ModuleAttributeInfo info = (ModuleAttributeInfo)i.next();
            if (ORDER_EXCLUDE.contains(info.getName())) {
                // Skip.
                continue;
            }
            String absoluteName = type.getAbsoluteName() + "/" + info.getName();
            if (JmxUtils.isOpenType(info.getType())) {
                String description = info.getDescription();
                if (description == null || description.length() <= 0) {
                    // Description can't be empty.
                    description = info.getName();
                }
                attributes.add(new OpenMBeanAttributeInfoSupport(
                    absoluteName, description,
                    JmxUtils.getOpenType(info.getType()), true, true, false));
            } else if(info.isComplexType()) {
                try {
                    ComplexType c =
                        (ComplexType)type.getAttribute(info.getName());
                    addCrawlOrderAttributes(c, attributes);
                } catch (AttributeNotFoundException e) {
                    logger.log(Level.SEVERE, "Failed get of attribute", e);
                } catch (MBeanException e) {
                    logger.log(Level.SEVERE, "Failed get of attribute", e);
                } catch (ReflectionException e) {
                    logger.log(Level.SEVERE, "Failed get of attribute", e);
                }
            } else if (info.getType().equals(TextField.class.getName())) {
                // Special handling for TextField.  Use the STRING OpenType.
                attributes.add(new OpenMBeanAttributeInfoSupport(
                        absoluteName, info.getDescription(),
                        SimpleType.STRING, true, true, false));
            } else {
                // Looks like only type we don't currently handle is StringList.
                // Figure how to do it.  Add as AttributeList?
                logger.fine(info.getType());
            }
        }
    }
    
    public Object getAttribute(String attribute_name)
    throws AttributeNotFoundException {
        if (attribute_name == null) {
            throw new RuntimeOperationsException(
                 new IllegalArgumentException("Attribute name cannot be null"),
                 "Cannot call getAttribute with null attribute name");
        }
        
        // If no controller, we can't do any work in here.
        if (this.controller == null) {
            throw new RuntimeOperationsException(
                 new NullPointerException("Controller is null"),
                 "Controller is null");
        }
        
        // Is it a bdbje attribute?
        if (this.bdbjeAttributeNameList.contains(attribute_name)) {
            try {
                return this.bdbjeMBeanHelper.getAttribute(
                        this.controller.getBdbEnvironment(), attribute_name);
            } catch (MBeanException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        
        // Is it a crawl-order attribute?
        if (attribute_name.
                startsWith(this.controller.getOrder().getAbsoluteName())) {
            return getCrawlOrderAttribute(attribute_name);
        }
        
        if (!ATTRIBUTE_LIST.contains(attribute_name)) {
            throw new AttributeNotFoundException("Attribute " +
                    attribute_name + " is unimplemented.");
        }

        // The pattern in the below is to match an attribute and when found
        // do a return out of if clause.  Doing it this way, I can fall
        // on to the AttributeNotFoundException for case where we've an
        // attribute but no handler.
        if (attribute_name.equals(STATUS_ATTR)) {
            return getCrawlStatus();
        }
        if (attribute_name.equals(NAME_ATTR)) {
            return getJobName();
        }
        if (attribute_name.equals(UID_ATTR)) {
            return getUID();
        }
        if (attribute_name.equals(TOTAL_DATA_ATTR)) {
            return new Long(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().totalBytesCrawled());
        }
        if (attribute_name.equals(CRAWL_TIME_ATTR)) {
            return new Long(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().getCrawlerTotalElapsedTime() /
                    1000);
        }
        if (attribute_name.equals(CURRENT_DOC_RATE_ATTR)) {
            return new Double(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().currentProcessedDocsPerSec());
        }
        if (attribute_name.equals(DOC_RATE_ATTR)) {
            return new Double(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().processedDocsPerSec());
        }
        if (attribute_name.equals(KB_RATE_ATTR)) {
            return new Long(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().currentProcessedKBPerSec());
        }
        if (attribute_name.equals(CURRENT_KB_RATE_ATTR)) {
            return new Long(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().processedKBPerSec());
        }
        if (attribute_name.equals(THREAD_COUNT_ATTR)) {
            return new Integer(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().activeThreadCount());
        }       
        if (attribute_name.equals(FRONTIER_SHORT_REPORT_ATTR)) {
            return getFrontierOneLine();
        }
        if (attribute_name.equals(THREADS_SHORT_REPORT_ATTR)) {
            return getThreadOneLine();
        }
        if (attribute_name.equals(DISCOVERED_COUNT_ATTR)) {
            return new Long(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().totalCount());
        }
        if (attribute_name.equals(DOWNLOAD_COUNT_ATTR)) {
            return new Long(this.controller == null &&
                    this.controller.getStatistics() != null? 0:
                this.controller.getStatistics().successfullyFetchedCount());
        }
        
        throw new AttributeNotFoundException("Attribute " +
            attribute_name + " not found.");
    }
    
    protected Object getCrawlOrderAttribute(final String attribute_name) {
        CrawlOrder order = this.getController().getOrder();
        Object result = null;
        try {
            result = getCrawlOrderAttribute(attribute_name.substring(order
                    .getAbsoluteName().length()), order);
        } catch (NullPointerException e) {
            logger.log(Level.SEVERE, "Failed get of " + attribute_name, e);
        } catch (AttributeNotFoundException e) {
            logger.log(Level.SEVERE, "Failed get of " + attribute_name, e);
        } catch (MBeanException e) {
            logger.log(Level.SEVERE, "Failed get of " + attribute_name, e);
        } catch (ReflectionException e) {
            logger.log(Level.SEVERE, "Failed get of " + attribute_name, e);
        }
        return result;
    }

    protected Object getCrawlOrderAttribute(final String attribute_name,
            final ComplexType ct)
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        String subName = attribute_name.startsWith("/") ? attribute_name
                .substring(1) : attribute_name;
        int index = subName.indexOf("/");
        if (index <= 0) {
            MBeanAttributeInfo info = ct.getAttributeInfo(subName);
            // Special handling for TextField.
            return info.getType().equals(TextField.class.getName()) ? ct
                    .getAttribute(subName).toString() : ct
                    .getAttribute(subName);
        }
        return getCrawlOrderAttribute(subName.substring(index + 1),
                (ComplexType) ct.getAttribute(subName.substring(0, index)));
    }
    
    public AttributeList getAttributes(String [] attributeNames) {
        if (attributeNames == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("attributeNames[] cannot be " +
                "null"), "Cannot call getAttributes with null attribute " +
                "names");
        }
        
        // If no controller, we can't do any work in here.
        if (this.controller == null) {
            throw new RuntimeOperationsException(
                 new NullPointerException("Controller is null"),
                 "Controller is null");
        }
        
        AttributeList resultList = new AttributeList();
        if (attributeNames.length == 0) {
            return resultList;
        }
        for (int i = 0; i < attributeNames.length; i++) {
            try {
                Object value = getAttribute(attributeNames[i]);
                resultList.add(new Attribute(attributeNames[i], value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return(resultList);
    }

    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException {
        setAttributeInternal(attribute);
        // prompt updating of settings-sensitive components
        kickUpdate();
    }

	protected void setAttributeInternal(Attribute attribute)
			throws AttributeNotFoundException {
		// Is it a crawl order attribute?
        CrawlOrder order = this.getController().getOrder();
        String attName = attribute.getName();
        if (attName.startsWith(order.getAbsoluteName())) {
            try {
                setCrawlOrderAttribute(attribute.getName().substring(
                        order.getAbsoluteName().length()), order, attribute);
            } catch (NullPointerException e) {
                logger.log(Level.SEVERE, "Failed set of " + attName, e);
            } catch (AttributeNotFoundException e) {
                logger.log(Level.SEVERE, "Failed set of " + attName, e);
            } catch (MBeanException e) {
                logger.log(Level.SEVERE, "Failed set of " + attName, e);
            } catch (ReflectionException e) {
                logger.log(Level.SEVERE, "Failed set of " + attName, e);
            } catch (InvalidAttributeValueException e) {
                logger.log(Level.SEVERE, "Failed set of " + attName, e);
            }
            return;
        }
        
        // Is it a bdbje attribute?
        if (this.bdbjeAttributeNameList.contains(attName)) {
            try {
                this.bdbjeMBeanHelper.setAttribute(this.controller
                        .getBdbEnvironment(), attribute);
            } catch (AttributeNotFoundException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            } catch (InvalidAttributeValueException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
            return;
        }
        
        // Else, we don't know how to handle this attribute.
        throw new AttributeNotFoundException("Attribute " + attName +
            " can not be set.");
	}
    
    protected void setCrawlOrderAttribute(final String attribute_name,
            final ComplexType ct, final Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        String subName = attribute_name.startsWith("/") ? attribute_name
                .substring(1) : attribute_name;
        int index = subName.indexOf("/");
        if (index <= 0) {
            ct.setAttribute(new Attribute(subName, attribute.getValue()));
            return;
        }
        setCrawlOrderAttribute(subName.substring(index + 1), (ComplexType) ct
                .getAttribute(subName.substring(0, index)), attribute);
    }

    public AttributeList setAttributes(AttributeList attributes) {
        if (attributes == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("attributeNames[] cannot be " +
                "null"), "Cannot call getAttributes with null attribute " +
                "names");
        }
        
        AttributeList resultList = new AttributeList();
        if (attributes.size() == 0) {
            return resultList;
        }
        for (int i = 0; i < attributes.size(); i++) {
            try {
                Attribute attr = (Attribute)attributes.get(i);
                setAttributeInternal(attr);
                String an = attr.getName();
                Object newValue = getAttribute(an);
                resultList.add(new Attribute(an, newValue));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // prompt updating of settings-sensitive components
        kickUpdate();
        return resultList;
    }

    public Object invoke(String operationName, Object[] params,
        String[] signature)
    throws ReflectionException {
        if (operationName == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Operation name cannot be null"),
                "Cannot call invoke with null operation name");
        }
        
        controller.installThreadContextSettingsHandler();
        
        if (this.bdbjeOperationsNameList.contains(operationName)) {
            try {
                Object o = this.bdbjeMBeanHelper.invoke(
                        this.controller.getBdbEnvironment(),
                        operationName, params, signature);
                // If OP_DB_ST, return String version of result.
                if (operationName.equals(OP_DB_STAT)) {
                    return o.toString();
                }
                return o;
            } catch (MBeanException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        
        // TODO: Exploit passed signature.
        
        // The pattern in the below is to match an operation and when found
        // do a return out of if clause.  Doing it this way, I can fall
        // on to the MethodNotFoundException for case where we've an
        // attribute but no handler.
        if (operationName.equals(IMPORT_URI_OPER)) {
            JmxUtils.checkParamsCount(IMPORT_URI_OPER, params, 3);
            mustBeCrawling();
            try {
                importUri((String)params[0],
                    ((Boolean)params[1]).booleanValue(),
                    ((Boolean)params[2]).booleanValue());
            } catch (URIException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
            return null;
        }
        
        if (operationName.equals(IMPORT_URIS_OPER)) {
            JmxUtils.checkParamsCount(IMPORT_URIS_OPER, params, 4);
            mustBeCrawling();
            return importUris((String)params[0],
                ((String)params[1]).toString(),
                ((Boolean)params[2]).booleanValue(),
                ((Boolean)params[3]).booleanValue());
        }
        
        if (operationName.equals(DUMP_URIS_OPER)) {
            JmxUtils.checkParamsCount(DUMP_URIS_OPER, params, 4);
            mustBeCrawling();
            if (!this.controller.isPaused()) {
                throw new RuntimeOperationsException(
                        new IllegalArgumentException("Must " + "be paused"),
                        "Cannot dump URI's from running job.");
            }
            dumpUris((String) params[0], (String) params[1],
                    ((Integer) params[2]).intValue(), ((Boolean) params[3])
                            .booleanValue());
        }
        
        if (operationName.equals(PAUSE_OPER)) {
            JmxUtils.checkParamsCount(PAUSE_OPER, params, 0);
            mustBeCrawling();
            pause();
            return null;
        }
        
        if (operationName.equals(RESUME_OPER)) {
            JmxUtils.checkParamsCount(RESUME_OPER, params, 0);
            mustBeCrawling();
            resume();
            return null;
        }
        
        if (operationName.equals(FRONTIER_REPORT_OPER)) {
            JmxUtils.checkParamsCount(FRONTIER_REPORT_OPER, params, 1);
            mustBeCrawling();
            return getFrontierReport((String)params[0]);
        }
        
        if (operationName.equals(THREADS_REPORT_OPER)) {
            JmxUtils.checkParamsCount(THREADS_REPORT_OPER, params, 0);
            mustBeCrawling();
            return getThreadsReport();
        }
        
        if (operationName.equals(SEEDS_REPORT_OPER)) {
            JmxUtils.checkParamsCount(SEEDS_REPORT_OPER, params, 0);
            mustBeCrawling();
            StringWriter sw = new StringWriter();
            if (getStatisticsTracking() != null &&
                    getStatisticsTracking() instanceof StatisticsTracker) {
                ((StatisticsTracker)getStatisticsTracking()).
                    writeSeedsReportTo(new PrintWriter(sw));
            } else {
                sw.write("Unsupported");
            }
            return sw.toString();
        }       
        
        if (operationName.equals(CHECKPOINT_OPER)) {
            JmxUtils.checkParamsCount(CHECKPOINT_OPER, params, 0);
            mustBeCrawling();
            try {
                checkpoint();
            } catch (IllegalStateException e) {
                throw new RuntimeOperationsException(e);
            }
            return null;
        }
        
        if (operationName.equals(PROGRESS_STATISTICS_OPER)) {
            JmxUtils.checkParamsCount(PROGRESS_STATISTICS_OPER, params, 0);
            mustBeCrawling();
            return getStatisticsTracking().getProgressStatisticsLine();
        }
        
        if (operationName.equals(PROGRESS_STATISTICS_LEGEND_OPER)) {
            JmxUtils.checkParamsCount(PROGRESS_STATISTICS_LEGEND_OPER,
                    params, 0);
            return getStatisticsTracking().progressStatisticsLegend();
        }
        
        throw new ReflectionException(
            new NoSuchMethodException(operationName),
                "Cannot find the operation " + operationName);
    }
    
    public void mustBeCrawling() {
        if (!isCrawling()) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Not " +
                "crawling (Shouldn't ever be the case)"),
                "Not current crawling job?");
        }
    }
    
    public boolean isCrawling() {
        return this.controller != null;
    }
    
    /**
     * Utility method to get the stored list of ignored seed items (if any),
     * from the last time the seeds were imported to the frontier.
     * 
     * @return String of all ignored seed items, or null if none
     */
    public String getIgnoredSeeds() {
        File ignoredFile = new File(getDirectory(),
                AbstractFrontier.IGNORED_SEEDS_FILENAME);
        if(!ignoredFile.exists()) {
            return null;
        }
        try {
            return FileUtils.readFileAsString(ignoredFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Forward a 'kick' update to current controller if any.
     * @see CrawlController#kickUpdate()
     */
    public void kickUpdate(){
        if (this.controller != null){
            this.controller.kickUpdate();
        }
    }
    
    /**
     * Returns a URIFrontierMarker for the current, paused, job. If there is no
     * current job or it is not paused null will be returned.
     *
     * @param regexpr A regular expression that each URI must match in order to
     * be considered 'within' the marker.
     * @param inCacheOnly Limit marker scope to 'cached' URIs.
     * @return a URIFrontierMarker for the current job.
     * @see #getPendingURIsList(FrontierMarker, int, boolean)
     * @see org.archive.crawler.framework.Frontier#getInitialMarker(String,
     *      boolean)
     * @see org.archive.crawler.framework.FrontierMarker
     */
    public FrontierMarker getInitialMarker(String regexpr,
            boolean inCacheOnly) {
        return (this.controller != null && this.controller.isPaused())?
           this.controller.getFrontier().getInitialMarker(regexpr, inCacheOnly):
               null;
    }
    
    /**
     * Returns the frontiers URI list based on the provided marker. This method
     * will return null if there is not current job or if the current job is
     * not paused. Only when there is a paused current job will this method
     * return a URI list.
     *
     * @param marker URIFrontier marker
     * @param numberOfMatches Maximum number of matches to return
     * @param verbose Should detailed info be provided on each URI?
     * @return the frontiers URI list based on the provided marker
     * @throws InvalidFrontierMarkerException
     *             When marker is inconsistent with the current state of the
     *             frontier.
     * @see #getInitialMarker(String, boolean)
     * @see org.archive.crawler.framework.FrontierMarker
     */
    public ArrayList<String> getPendingURIsList(FrontierMarker marker,
            int numberOfMatches, boolean verbose)
    throws InvalidFrontierMarkerException {
        return  (this.controller != null && this.controller.isPaused())?
            this.controller.getFrontier().getURIsList(marker, numberOfMatches,
                    verbose):
            null;
    }

    public void dumpUris(String filename, String regexp, int numberOfMatches,
            boolean verbose) {
        try {
            PrintWriter out = new PrintWriter(filename); 
            FrontierMarker marker = 
                controller.getFrontier().getInitialMarker(regexp, false);
            int matchesDumped = 0;
            
            while(matchesDumped<numberOfMatches) {
                int batchMatches = Math.min(100, numberOfMatches-matchesDumped);
                
                ArrayList<String> batchOfUris = 
                    getPendingURIsList(marker,batchMatches,false);
                for(String uriLine : batchOfUris) {
                    out.write(uriLine);
                    out.write("\n");
                    matchesDumped++;
                }
                if (batchOfUris.size()<batchMatches) {
                    // must be exhausted; we're finished
                    break; 
                }
            }
            IOUtils.closeQuietly(out); 
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Failed dumpUris write", e);
        } catch (InvalidFrontierMarkerException e) {
            logger.log(Level.SEVERE, "Failed dumpUris", e);
        }
    }
    
    public void crawlStarted(String message) {
        if (this.mbeanName != null) {
            // Can be null around job startup.
            sendNotification(new Notification("crawlStarted",
                this.mbeanName,  getNotificationsSequenceNumber(), message)); 
        }
    }

    public void crawlEnding(String sExitMessage) {
        setRunning(false);
        setStatus(sExitMessage);
        setReadOnly();
        if (this.mbeanName != null) {
            sendNotification(new Notification("crawlEnding", this.mbeanName,
                getNotificationsSequenceNumber(), sExitMessage));
        }
    }

    public void crawlEnded(String sExitMessage) {
        // Let the settings handler be cleaned up by the crawl controller
        // completeStop. Just let go of our reference in here.
        // if (this.settingsHandler != null) {
        //    this.settingsHandler.cleanup();
        // }
        
        // We used to zero-out datamembers but no longer needed now CrawlJobs
        // no longer persist after completion (They used to be kept around in
        // a list so operator could view CrawlJob finish state and reports --
        // but we now dump actual job and create a new uninitialized CrawlJob
        // that points at old CrawlJob data. 
    }

    public void crawlPausing(String statusMessage) {
        setStatus(statusMessage);
    }

    public void crawlPaused(String statusMessage) {
        setStatus(statusMessage);
        if (this.mbeanName != null) {
            // Can be null around job startup.
            sendNotification(new Notification("crawlPaused", this.mbeanName,
                getNotificationsSequenceNumber(), statusMessage));
        }
    }

    public void crawlResuming(String statusMessage) {
        setStatus(statusMessage);
        if (this.mbeanName != null) {
            // Can be null around job startup.
            sendNotification(new Notification("crawlResuming", this.mbeanName,
                getNotificationsSequenceNumber(), statusMessage));
        }
    }

    public void crawlCheckpoint(File checkpointDir) throws Exception {
        setStatus(CrawlJob.STATUS_CHECKPOINTING);
    }

    public CrawlController getController() {
        return this.controller;
    }
    
    public ObjectName preRegister(final MBeanServer server, ObjectName on)
    throws Exception {
        this.mbeanServer = server;
        @SuppressWarnings("unchecked")
        Hashtable<String,String> ht = on.getKeyPropertyList();
        if (!ht.containsKey(JmxUtils.NAME)) {
            throw new IllegalArgumentException("Name property required" +
                on.getCanonicalName());
        }
        // Now append key/values from hosting heritrix JMX ObjectName so it can be
        // found just by examination of the CrawlJob JMX ObjectName.  Add heritrix
        // name attribute as 'mother' attribute.
        Heritrix h = getHostingHeritrix();
        if (h == null || h.getMBeanName() == null) {
            throw new IllegalArgumentException("Hosting heritrix not found " +
                "or not registered with JMX: " + on.getCanonicalName());
        }
        @SuppressWarnings("unchecked")
        Map<String,String> hht = h.getMBeanName().getKeyPropertyList();
        ht.put(JmxUtils.MOTHER, hht.get(JmxUtils.NAME));
        String port = hht.get(JmxUtils.JMX_PORT);
        if (port != null) {
        	ht.put(JmxUtils.JMX_PORT, port);
        }
        ht.put(JmxUtils.HOST, hht.get(JmxUtils.HOST));
        if (!ht.containsKey(JmxUtils.TYPE)) {
            ht.put(JmxUtils.TYPE, CRAWLJOB_JMXMBEAN_TYPE);
        }
        this.mbeanName = new ObjectName(on.getDomain(), ht);
        return this.mbeanName;
    }

    public void postRegister(Boolean registrationDone) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(
                JmxUtils.getLogRegistrationMsg(this.mbeanName.getCanonicalName(),
                this.mbeanServer, registrationDone.booleanValue()));
        }
    }

    public void preDeregister() throws Exception {
        // Nothing to do.
    }

    public void postDeregister() {
        if (mbeanName ==  null) {
            return;
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info(JmxUtils.getLogUnregistrationMsg(
                    this.mbeanName.getCanonicalName(), this.mbeanServer));
        }
        this.mbeanName = null;
    }
    
    /**
     * @return Heritrix that is hosting this job.
     */
    protected Heritrix getHostingHeritrix() {
        Heritrix hostingHeritrix = null;
        Map heritrice = Heritrix.getInstances();
        for (final Iterator i = heritrice.keySet().iterator(); i.hasNext();) {
            Heritrix h = (Heritrix)heritrice.get(i.next());
            if (h.getJobHandler().getCurrentJob() == this) {
                hostingHeritrix = h;
                break;
            }
        }
        return hostingHeritrix;
    }
    
    /**
     * @return Unique name for job that is safe to use in jmx (Like display
     * name but without spaces).
     */
    public String getJmxJobName() {
        return getJobName() + "-" + getUID();
    }

    /**
     * @return Notification sequence number (Does increment after each access).
     */
    protected static int getNotificationsSequenceNumber() {
        return notificationsSequenceNumber++;
    }

    protected ObjectName getMbeanName() {
        return this.mbeanName;
    }
    
    /**
     * @return the statistics tracking instance (of null if none yet available).
     */
    public StatisticsTracking getStatisticsTracking() {
        return this.controller == null ||
            this.controller.getStatistics() == null? null:
                this.controller.getStatistics();
    }
}
