/*
 * CrawlOrder
 *
 * $Header$
 *
 * Created on May 15, 2003
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
 *
 */

package org.archive.crawler.datamodel;

import java.io.File;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.url.canonicalize.BaseRule;

/**
 * Represents the 'root' of the settings hierarchy. Contains those settings that
 * do not belong to any specific module, but rather relate to the crawl as a
 * whole (much of this is used by the CrawlController directly or indirectly).
 *
 * @see org.archive.crawler.settings.ModuleType
 */
public class CrawlOrder extends ModuleType implements Serializable {

    private static final long serialVersionUID = -6715840285961511669L;

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.CrawlOrder");

    public static final String ATTR_NAME = "crawl-order";
    public static final String ATTR_SETTINGS_DIRECTORY = "settings-directory";
    public static final String ATTR_DISK_PATH = "disk-path";
    public static final String ATTR_LOGS_PATH = "logs-path";
    public static final String ATTR_CHECKPOINTS_PATH = "checkpoints-path";
    public static final String ATTR_STATE_PATH = "state-path";
    public static final String ATTR_SCRATCH_PATH = "scratch-path";
    public static final String ATTR_RECOVER_PATH = "recover-path";
    public static final String ATTR_RECOVER_RETAIN_FAILURES =
        "recover-retain-failures";
    public static final String ATTR_RECOVER_SCOPE_INCLUDES =
        "recover-scope-includes";
    public static final String ATTR_RECOVER_SCOPE_ENQUEUES =
        "recover-scope-enqueues";
    public static final String ATTR_MAX_BYTES_DOWNLOAD = "max-bytes-download";
    public static final String ATTR_MAX_DOCUMENT_DOWNLOAD =
        "max-document-download";
    public static final String ATTR_MAX_TIME_SEC = "max-time-sec";
    public static final String ATTR_MAX_TOE_THREADS = "max-toe-threads";
    public static final String ATTR_HTTP_HEADERS = "http-headers";
    public static final String ATTR_USER_AGENT = "user-agent";
    public static final String ATTR_FROM = "from";
    public static final String ATTR_PRE_FETCH_PROCESSORS =
        "pre-fetch-processors";
    public static final String ATTR_FETCH_PROCESSORS = "fetch-processors";
    public static final String ATTR_EXTRACT_PROCESSORS = "extract-processors";
    public static final String ATTR_WRITE_PROCESSORS = "write-processors";
    public static final String ATTR_POST_PROCESSORS = "post-processors";
    public static final String ATTR_LOGGERS = "loggers";
    public static final String ATTR_RULES = "uri-canonicalization-rules";
    public static final String ATTR_RECORDER_OUT_BUFFER =
        "recorder-out-buffer-bytes";
    public static final String ATTR_RECORDER_IN_BUFFER =
        "recorder-in-buffer-bytes";
    
    /** Percentage of heap to allocate to bdb cache */
    public static final String ATTR_BDB_CACHE_PERCENT =
        "bdb-cache-percent";
    
    /**
     * When checkpointing, copy the bdb logs.
     * Default is true.  If false, then we do not copy logs on checkpoint AND
     * we tell bdbje never to delete log files; instead it renames
     * files-to-delete with a '.del' extension.  Assumption is that when this
     * setting is false, an external process is managing the removing of
     * bdbje log files and that come time to recover from a checkpoint, the
     * files that comprise a checkpoint are manually assembled.
     */
    public static final String ATTR_CHECKPOINT_COPY_BDBJE_LOGS =
        "checkpoint-copy-bdbje-logs";
    public static final Boolean DEFAULT_CHECKPOINT_COPY_BDBJE_LOGS =
        Boolean.TRUE;
    
    /**
     * Default size of bdb cache.
     */
    private final static Integer DEFAULT_BDB_CACHE_PERCENT = new Integer(0);

    private transient MapType httpHeaders;
    private transient MapType loggers;

    private transient CrawlController controller;

    /**
     * Regex for acceptable user-agent format.
     */
    private static String ACCEPTABLE_USER_AGENT =
        "\\S+.*\\(.*\\+http(s)?://\\S+\\.\\S+.*\\).*";

    /**
     * Regex for acceptable from address.
     */
    private static String ACCEPTABLE_FROM = "\\S+@\\S+\\.\\S+";
    

    /** Construct a CrawlOrder.
     */
    public CrawlOrder() {
        super(ATTR_NAME, "Heritrix crawl order. This forms the root of " +
                "the settings framework.");
        Type e;

        e = addElementToDefinition(new SimpleType(ATTR_SETTINGS_DIRECTORY,
                "Directory where override settings are kept. The settings " +
                "for many modules can be overridden based on the domain or " +
                "subdomain of the URI being processed. This setting specifies" +
                " a file level directory to store those settings. The path" +
                " is relative to 'disk-path' unless" +
                " an absolute path is provided.", "settings"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_DISK_PATH,
                "Directory where logs, arcs and other run time files will " +
                "be kept. If this path is a relative path, it will be " +
                "relative to the crawl order.", ""));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_LOGS_PATH,
                "Directory where crawler log files will be kept. If this path " +
                "is a relative path, it will be relative to the 'disk-path'.",
                "logs"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_CHECKPOINTS_PATH,
                "Directory where crawler checkpoint files will be kept. " +
                "If this path " +
                "is a relative path, it will be relative to the 'disk-path'.",
                "checkpoints"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_STATE_PATH,
                "Directory where crawler-state files will be kept. If this path " +
                "is a relative path, it will be relative to the 'disk-path'.",
                "state"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_SCRATCH_PATH,
                "Directory where discardable temporary files will be kept. " +
                "If this path " +
                "is a relative path, it will be relative to the 'disk-path'.",
                "scratch"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_MAX_BYTES_DOWNLOAD,
                "Maximum number of bytes to download. Once this number is" +
                " exceeded the crawler will stop. " +
                "A value of zero means no upper limit.", new Long(0)));
        e.setOverrideable(false);

        e = addElementToDefinition(new SimpleType(ATTR_MAX_DOCUMENT_DOWNLOAD,
                "Maximum number of documents to download. Once this number" +
                " is exceeded the crawler will stop. " +
                "A value of zero means no upper limit.", new Long(0)));
        e.setOverrideable(false);

        e = addElementToDefinition(new SimpleType(ATTR_MAX_TIME_SEC,
                "Maximum amount of time to crawl (in seconds). Once this" +
                " much time has elapsed the crawler will stop. A value of" +
                " zero means no upper limit.",
                new Long(0)));
        e.setOverrideable(false);
        
        e = addElementToDefinition(new SimpleType(ATTR_MAX_TOE_THREADS,
                "Maximum number of threads processing URIs at the same time.",
                new Integer(100)));
        e.setOverrideable(false);

        e = addElementToDefinition(new SimpleType(ATTR_RECORDER_OUT_BUFFER,
                "Size in bytes of in-memory buffer to record outbound " +
                "traffic. One such buffer is reserved for every ToeThread.",
                new Integer(4096)));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        
        e = addElementToDefinition(new SimpleType(ATTR_RECORDER_IN_BUFFER,
                "Size in bytes of in-memory buffer to record inbound " +
                "traffic. One such buffer is reserved for every ToeThread.",
                new Integer(65536)));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        
        e = addElementToDefinition(new SimpleType(ATTR_BDB_CACHE_PERCENT,
                "Percentage of heap to allocate to BerkeleyDB JE cache. " +
                "Default of zero means no preference (accept BDB's default, " +
                "usually 60%, or the je.maxMemoryPercent property value).",
                DEFAULT_BDB_CACHE_PERCENT));
        e.setExpertSetting(true);
        e.setOverrideable(false);
        
        addElementToDefinition(new CrawlScope());

        httpHeaders = (MapType) addElementToDefinition(new MapType(
                ATTR_HTTP_HEADERS, "HTTP headers. Information that will " +
                        "be used when constructing the HTTP headers of " +
                        "the crawler's HTTP requests."));

        e = httpHeaders.addElementToDefinition(new SimpleType(ATTR_USER_AGENT,
                "User agent to act as. Field must contain valid URL " +
                "that links to website of person or organization " +
                "running the crawl. Replace 'PROJECT_URL_HERE' in " +
                "initial template. E.g. If organization " +
                "is Library of Congress, a valid user agent would be:" +
                "'Mozilla/5.0 (compatible; loc-crawler/0.11.0 " +
                "+http://loc.gov)'. " +
                "Note, you must preserve the '+' before the 'http'.",
          "Mozilla/5.0 (compatible; heritrix/@VERSION@ +PROJECT_URL_HERE)"));

        e = httpHeaders.addElementToDefinition(new SimpleType(ATTR_FROM,
                "Contact information. This field must contain a valid " +
                "e-mail address for the person or organization responsible" +
                "for this crawl: e.g. 'webmaster@loc.gov'",
                "CONTACT_EMAIL_ADDRESS_HERE"));

        addElementToDefinition(new RobotsHonoringPolicy());

        e = addElementToDefinition(new ModuleType(
                Frontier.ATTR_NAME, "Frontier"));
        e.setLegalValueType(Frontier.class);

        e = (MapType) addElementToDefinition(new MapType(ATTR_RULES,
            "Ordered list of url canonicalization rules. " +
            "Rules are applied in the order listed from top to bottom.",
            BaseRule.class));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        
        e = addElementToDefinition(new MapType(
                ATTR_PRE_FETCH_PROCESSORS, "Processors to run prior to" +
                        " fetching anything from the network.",
                        Processor.class));
        e.setOverrideable(false);

        e = addElementToDefinition(new MapType(
                ATTR_FETCH_PROCESSORS, "Processors that fetch documents."
                , Processor.class));
        e.setOverrideable(false);

        e = addElementToDefinition(new MapType(
                ATTR_EXTRACT_PROCESSORS, "Processors that extract new URIs" +
                        " from fetched documents.", Processor.class));
        e.setOverrideable(false);

        e = addElementToDefinition(new MapType(
                ATTR_WRITE_PROCESSORS, "Processors that write documents" +
                        " to archives.", Processor.class));
        e.setOverrideable(false);

        e = addElementToDefinition(new MapType(
                ATTR_POST_PROCESSORS, "Processors that do cleanup and feed" +
                        " the frontier with new URIs.", Processor.class));
        e.setOverrideable(false);

        loggers = (MapType) addElementToDefinition(new MapType(ATTR_LOGGERS,
                "Statistics tracking modules. Any number of specialized " +
                "statistics tracker that monitor a crawl and write logs, " +
                "reports and/or provide information to the user interface."));

        e = addElementToDefinition(new SimpleType(ATTR_RECOVER_PATH,
                "Optional. Points at recover log (or recover.gz log) OR " +
                "the checkpoint directory to use recovering a crawl.", ""));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        
        e = addElementToDefinition(new SimpleType(
            ATTR_CHECKPOINT_COPY_BDBJE_LOGS,
            "When true, on a checkpoint, we copy off the bdbje log files to " +
            "the checkpoint directory. To recover a checkpoint, just " +
            "set the " + ATTR_RECOVER_PATH + " to point at the checkpoint " +
            "directory to recover.  This is default setting. " +
            "But if crawl is large, " +
            "copying bdbje log files can take tens of minutes and even " +
            "upwards of an hour (Copying bdbje log files will consume bulk " +
            "of time checkpointing). If this setting is false, we do NOT copy " +
            "bdbje logs on checkpoint AND we set bdbje to NEVER delete log " +
            "files (instead we have it rename files-to-delete with a '.del'" +
            "extension). Assumption is that when this setting is false, " +
            "an external process is managing the removal of bdbje log files " +
            "and that come time to recover from a checkpoint, the files that " +
            "comprise a checkpoint are manually assembled. This is an expert " +
            "setting.",
            DEFAULT_CHECKPOINT_COPY_BDBJE_LOGS));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_RECOVER_RETAIN_FAILURES,
                "When recovering via the recover.log, should failures " +
                "in the log be retained in the recovered crawl, " +
                "preventing the corresponding URIs from being retried. " +
                "Default is false, meaning failures are forgotten, and " +
                "the corresponding URIs will be retried in the recovered " +
                "crawl.", Boolean.FALSE));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_RECOVER_SCOPE_INCLUDES,
                "When recovering via the recover.log, should URIs " +
                "be checked against scope before considered included " +
                "during the first phase which primes the already-seen " +
                "set. " +
                "Default is true, meaning scope changes in a recovered " +
                "crawl can slim the already-seen size. ", Boolean.TRUE));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_RECOVER_SCOPE_ENQUEUES,
                "When recovering via the recover.log, should URIs " +
                "be checked against scope before reenqueued during " +
                "the second phase which fills the to-be-fetched queues. " +
                "Default is true, meaning scope changes in a recovered " +
                "crawl can slim the pending queues. ", Boolean.TRUE));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        
        
        e = addElementToDefinition(
           new CredentialStore(CredentialStore.ATTR_NAME));
        e.setOverrideable(true);
        e.setExpertSetting(true);
    }

    /**
     * @param curi
     * @return user-agent header value to use
     */
    public String getUserAgent(CrawlURI curi) {
        return ((String) httpHeaders.getUncheckedAttribute(curi, ATTR_USER_AGENT));
    }

    /**
     * @param curi
     * @return from header value to use
     */
    public String getFrom(CrawlURI curi) {
        String res = null;
        try {
            res = (String) httpHeaders.getAttribute(ATTR_FROM, curi);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return res;
    }

    /**
     * Returns the set number of maximum toe threads.
     * @return Number of maximum toe threads
     */
    public int getMaxToes() {
        Integer res = null;
        try {
            res = (Integer) getAttribute(null, ATTR_MAX_TOE_THREADS);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return res.intValue();
    }

    /**
     * This method gets the RobotsHonoringPolicy object from the orders file.
     *
     * @return the new RobotsHonoringPolicy
     */
    public RobotsHonoringPolicy getRobotsHonoringPolicy() {
        try {
            return (RobotsHonoringPolicy) getAttribute(null, RobotsHonoringPolicy.ATTR_NAME);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return null;
        } 
    }

    /** Get the name of the order file.
     *
     * @return the name of the order file.
     */
    public String getCrawlOrderName() {
        return getSettingsHandler().getSettingsObject(null).getName();
    }

    /**
     * @return The crawl controller.
     */
    public CrawlController getController() {
        return controller;
    }

    /**
     * @param controller
     */
    public void setController(CrawlController controller) {
        this.controller = controller;
    }

    /**
     * Returns the Map of the StatisticsTracking modules that are included in the
     * configuration that the current instance of this class is representing.
     * @return Map of the StatisticsTracking modules
     */
    public MapType getLoggers() {
        return loggers;
    }

    /**
     * Checks if the User Agent and From field are set 'correctly' in
     * the specified Crawl Order.
     *
     * @throws FatalConfigurationException
     */
    public void checkUserAgentAndFrom() throws FatalConfigurationException {
        // don't start the crawl if they're using the default user-agent
        String userAgent = this.getUserAgent(null);
        String from = this.getFrom(null);
        if (!(userAgent.matches(ACCEPTABLE_USER_AGENT)
            && from.matches(ACCEPTABLE_FROM))) {
            throw new FatalConfigurationException("unacceptable 'user-agent' " +
                    " or 'from' (correct your configuration).");
        }
    }

    /**
     * @return Checkpoint directory.
     */
    public File getCheckpointsDirectory() {
        try {
            return getDirectoryRelativeToDiskPath((String) getAttribute(null,
                    CrawlOrder.ATTR_CHECKPOINTS_PATH));
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private File getDirectoryRelativeToDiskPath(String subpath) {
        File disk;
        try {
            disk = getSettingsHandler().getPathRelativeToWorkingDirectory(
                    (String) getAttribute(null, CrawlOrder.ATTR_DISK_PATH));
            return new File(disk, subpath);
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Return fullpath to the directory named by <code>key</code>
     * in settings.
     * If directory does not exist, it and all intermediary dirs
     * will be created.
     * @param key Key to use going to settings.
     * @return Full path to directory named by <code>key</code>.
     * @throws AttributeNotFoundException
     */
    public File getSettingsDir(String key)
    throws AttributeNotFoundException {
        String path = (String)getAttribute(null, key);
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = getDirectoryRelativeToDiskPath(path);
        }
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }
    
    
}
