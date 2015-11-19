/* Heritrix
 *
 * $Id: Heritrix.java 6794 2010-03-22 23:37:51Z gojomo $
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
 */
package org.archive.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

import org.apache.commons.cli.Option;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.CrawlJobErrorHandler;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.datamodel.CredentialStore;
import org.archive.crawler.datamodel.credential.Credential;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.AlertManager;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.selftest.SelfTestCrawlJobHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.io.SinkHandler;
import org.archive.io.SinkHandlerLogRecord;
import org.archive.net.UURI;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
import org.archive.util.JmxUtils;
import org.archive.util.JndiUtils;
import org.archive.util.PropertyUtils;
import org.archive.util.TextUtils;

import sun.net.www.protocol.file.FileURLConnection;


/**
 * Main class for Heritrix crawler. 
 * 
 * Heritrix is usually launched by a shell script that backgrounds heritrix
 * that redirects all stdout and stderr emitted by heritrix to a log file.  So
 * that startup messages emitted subsequent to the redirection of stdout and
 * stderr show on the console, this class prints usage or startup output
 * such as where the web UI can be found, etc., to a STARTLOG that the shell
 * script is waiting on.  As soon as the shell script sees output in this file,
 * it prints its content and breaks out of its wait.
 * See ${HERITRIX_HOME}/bin/heritrix.
 * 
 * <p>Heritrix can also be embedded or launched by webapp initialization or
 * by JMX bootstrapping.  So far I count 4 methods of instantiation:
 * <ol>
 * <li>From this classes main -- the method usually used;</li>
 * <li>From the Heritrix UI (The local-instances.jsp) page;</li>
 * <li>A creation by a JMX agent at the behest of a remote JMX client; and</li>
 * <li>A container such as tomcat or jboss.</li>
 * </ol>
 *
 * @author gojomo
 * @author Kristinn Sigurdsson
 * @author Stack
 */
public class Heritrix implements DynamicMBean, MBeanRegistration {
    /**
     * Heritrix logging instance.
     */
    private static final Logger logger =
        Logger.getLogger(Heritrix.class.getName());
    
    public static final File TMPDIR =
        new File(System.getProperty("java.io.tmpdir", "/tmp"));

    /**
     * Name of the heritrix properties file.
     */
    public static final String PROPERTIES = "heritrix.properties";

    /**
     * Name of the key to use specifying alternate heritrix properties on
     * command line.
     */
    public static final String PROPERTIES_KEY = PROPERTIES;
    
    /**
     * Prefix used on our properties we'll add to the System.properties list.
     */
    public static final String HERITRIX_PROPERTIES_PREFIX = "heritrix.";

    /**
     * Prefix used on other properties we'll add to the System.properties 
     * list (after stripping this prefix). 
     */
    public static final String SYSTEM_PREFIX = "system.";

    /**
     * Instance of web server if one was started.
     */
    private static SimpleHttpServer httpServer = null;

    /**
     * CrawlJob handler. Manages multiple crawl jobs at runtime.
     */
    private CrawlJobHandler jobHandler = null;

    /**
     * Heritrix start log file.
     *
     * This file contains standard out produced by this main class for startup
     * only.  Used by heritrix shell script.  Name here MUST match that in the
     * <code>bin/heritrix</code> shell script.  This is a DEPENDENCY the shell
     * wrapper has on this here java heritrix.
     */
    public static final String STARTLOG = "heritrix_dmesg.log";

    /**
     * Default encoding.
     * 
     * Used for content when fetching if none specified.
     */
	public static final String DEFAULT_ENCODING = "ISO-8859-1";

    /**
     * Heritrix stderr/stdout log file.
     *
     * This file should have nothing in it except messages over which we have
     * no control (JVM stacktrace, 3rd-party lib emissions).  The wrapper
     * startup script directs stderr/stdout here. This is an INTERDEPENDENCY
     * this program has with the wrapper shell script.  Shell can actually
     * pass us an alternate to use for this file.
     */
    public static String DEFAULT_HERITRIX_OUT = "heritrix_out.log";

    /**
     * Where to write this classes startup output.
     * 
     * This out should only be used if Heritrix is being run from the
     * command-line.
     */
    private static PrintWriter out = null;

    /**
     * The org.archive package
     */
    public static final String ARCHIVE_PACKAGE = "org.archive.";

    /**
     * The crawler package.
     */
	public static final String CRAWLER_PACKAGE = Heritrix.class.getName().
        substring(0, Heritrix.class.getName().lastIndexOf('.'));
    
    /**
     * The root context for a webapp.
     */
    public static final String ROOT_CONTEXT = "/";

    /**
     * Set to true if application is started from command line.
     */
    public static boolean commandLine = false;
    
    /**
     * True if container initialization has been run.
     */
    private static boolean containerInitialized = false;
    
    /**
     * True if properties have been loaded.
     */
    private static boolean propertiesLoaded = false;
    
    public static final String JAR_SUFFIX = ".jar";
    
    private AlertManager alertManager;

    /**
     * The context of the GUI webapp.  Default is root.
     */
    private static String adminContext = ROOT_CONTEXT;
    
    /**
     * True if we're to put up a GUI.
     * Cmdline processing can override.
     */
    public static boolean gui =
        !PropertyUtils.getBooleanProperty("heritrix.cmdline.nowui");
    
    /**
     * Port to put the GUI up on.
     * Cmdline processing can override.
     */
    public static int guiPort = SimpleHttpServer.DEFAULT_PORT;

    
    /**
     * A collection containing only localhost.  Used as default value
     * for guiHosts, and passed to SimpleHttpServer when doing selftest.
     */
    final private static Collection<String> LOCALHOST_ONLY =
     Collections.unmodifiableList(Arrays.asList(new String[] { "127.0.0.1" }));

    
    /**
     * Hosts to bind the GUI webserver to.
     * By default, only contans localhost.
     * Set to an empty collection to indicate that all available network
     * interfaces should be used for the webserver.
     */
    public static Collection<String> guiHosts = LOCALHOST_ONLY;
    
    
    /**
     * Web UI server, realm, context name.
     */
    public static String ADMIN = "admin";
    
    // OpenMBean support.
    /**
     * The MBean server we're registered with (May be null).
     */
    private MBeanServer mbeanServer = null;
    
    /**
     * MBean name we were registered as.
     */
    private ObjectName mbeanName = null;
    
    /**
     * Keep reference to all instances of Heritrix.
     * Used by the UI to figure which of the local Heritrice it should
     * be going against and to figure what to shutdown on the way out (If
     * there was always a JMX Agent, we wouldn't need to keep this list.  We
     * could always ask the JMX Agent for all instances. UPDATE: True we could
     * always ask the JMX Agent but we might keep around this local reference
     * because it will allow faster, less awkward -- think of marshalling the args
     * for JMX invoke operation -- access to local Heritrix instances.  A new
     * usage for this instances Map is in CrawlJob#preRegister to find the hosting
     * Heritrix instance).
     */
    private static Map<String,Heritrix> instances
     = new Hashtable<String,Heritrix>();
    
    private OpenMBeanInfoSupport openMBeanInfo;
    public static final String STATUS_ATTR = "Status";
    public static final String VERSION_ATTR = "Version";
    public static final String ISRUNNING_ATTR = "IsRunning";
    public static final String ISCRAWLING_ATTR = "IsCrawling";
    public static final String ALERTCOUNT_ATTR = "AlertCount";
    public static final String NEWALERTCOUNT_ATTR = "NewAlertCount";
    public static final String CURRENTJOB_ATTR = "CurrentJob";
    public static final List ATTRIBUTE_LIST;
    static {
        ATTRIBUTE_LIST = Arrays.asList(new String [] {STATUS_ATTR,
            VERSION_ATTR, ISRUNNING_ATTR, ISCRAWLING_ATTR,
            ALERTCOUNT_ATTR, NEWALERTCOUNT_ATTR, CURRENTJOB_ATTR});
    }
    
    public static final String START_OPER = "start";
    public static final String STOP_OPER = "stop";
    public static final String DESTROY_OPER = "destroy";
    public static final String INTERRUPT_OPER = "interrupt";
    public static final String START_CRAWLING_OPER = "startCrawling";
    public static final String STOP_CRAWLING_OPER = "stopCrawling";
    public static final String ADD_CRAWL_JOB_OPER = "addJob";
    public static final String TERMINATE_CRAWL_JOB_OPER =
        "terminateCurrentJob";
    public static final String DELETE_CRAWL_JOB_OPER = "deleteJob";
    public static final String ALERT_OPER = "alert";
    public static final String ADD_CRAWL_JOB_BASEDON_OPER = "addJobBasedon";
    public static final String PENDING_JOBS_OPER = "pendingJobs";
    public static final String COMPLETED_JOBS_OPER = "completedJobs";
    public static final String CRAWLEND_REPORT_OPER = "crawlendReport";
    public static final String SHUTDOWN_OPER = "shutdown";
    public static final String LOG_OPER = "log";
    public static final String REBIND_JNDI_OPER = "rebindJNDI";
    public static final List OPERATION_LIST;
    static {
        OPERATION_LIST = Arrays.asList(new String [] {START_OPER, STOP_OPER,
            INTERRUPT_OPER, START_CRAWLING_OPER, STOP_CRAWLING_OPER,
            ADD_CRAWL_JOB_OPER, ADD_CRAWL_JOB_BASEDON_OPER,
            DELETE_CRAWL_JOB_OPER, ALERT_OPER, PENDING_JOBS_OPER,
            COMPLETED_JOBS_OPER, CRAWLEND_REPORT_OPER, SHUTDOWN_OPER,
            LOG_OPER, DESTROY_OPER, TERMINATE_CRAWL_JOB_OPER,
            REBIND_JNDI_OPER});
    }
    private CompositeType jobCompositeType = null;
    private TabularType jobsTabularType = null;
    public static final String [] JOB_KEYS =
        new String [] {"uid", "name", "status"};

    private static String adminUsername;

    private static String adminPassword;
    
    /**
     * Constructor.
     * Does not register the created instance with JMX.  Assumed this
     * constructor is used by such as JMX agent creating an instance of
     * Heritrix at the commmand of a remote client (In this case Heritrix will
     * be registered by the invoking agent).
     * @throws IOException
     */
    public Heritrix() throws IOException {
        this(null, false);
    }
    
    public Heritrix(final boolean jmxregister) throws IOException {
        this(null, jmxregister);
    }
    
    /**
     * Constructor.
     * @param name If null, we bring up the default Heritrix instance.
     * @param jmxregister True if we are to register this instance with JMX
     * agent.
     * @throws IOException
     */
    public Heritrix(final String name, final boolean jmxregister)
    throws IOException {
        this(name, jmxregister, new CrawlJobHandler(getJobsdir()));
    }
    
    /**
     * Constructor.
     * @param name If null, we bring up the default Heritrix instance.
     * @param jmxregister True if we are to register this instance with JMX
     * agent.
     * @param cjh CrawlJobHandler to use.
     * @throws IOException
     */
    public Heritrix(final String name, final boolean jmxregister,
            final CrawlJobHandler cjh)
    throws IOException {
        super();
        containerInitialization();
        this.jobHandler = cjh;
        this.openMBeanInfo = buildMBeanInfo();
        // Set up the alerting system.  SinkHandler is also a global so will
        // catch alerts for all running Heritrix instances.  Will need to
        // address (Add name of instance that threw the alert to SinkRecord?).
        final SinkHandler sinkHandler = SinkHandler.getInstance();
        if (sinkHandler == null) {
            throw new NullPointerException("SinkHandler not found.");
        }
        // Adapt the alerting system to use SinkHandler.
        this.alertManager = new AlertManager() {
            public void add(SinkHandlerLogRecord record) {
                sinkHandler.publish(record);
            }

            public Vector getAll() {
                return sinkHandler.getAll();
            }

            public Vector getNewAll() {
                return sinkHandler.getAllUnread();
            }

            public SinkHandlerLogRecord get(String alertID) {
                return sinkHandler.get(Long.parseLong(alertID));
            }
            
            public int getCount() {
                return sinkHandler.getCount();
            }

            public int getNewCount() {
                return sinkHandler.getUnreadCount();
            }

            public void remove(String alertID) {
                sinkHandler.remove(Long.parseLong(alertID));
            }

            public void read(String alertID) {
                sinkHandler.read(Long.parseLong(alertID));
            }
        };
        
        try {
            Heritrix.registerHeritrix(this, name, jmxregister);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException(e);
        } catch (MBeanRegistrationException e) {
            throw new RuntimeException(e);
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException(e);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Run setup tasks for this 'container'. Idempotent.
     * 
     * @throws IOException
     */
    protected static void containerInitialization() throws IOException {
        if (Heritrix.containerInitialized) {
            return;
        }
        Heritrix.containerInitialized = true;
        // Load up the properties.  This invocation adds heritrix properties
        // to system properties so all available via System.getProperty.
        // Note, loadProperties and patchLogging have global effects.  May be an
        // issue if we're running inside a container such as tomcat or jboss.
        Heritrix.loadProperties();
        Heritrix.patchLogging();
        Heritrix.configureTrustStore();
        // Will run on SIGTERM but not on SIGKILL, unfortunately.
        // Otherwise, ensures we cleanup after ourselves (Deregister from
        // JMX and JNDI).
        Runtime.getRuntime().addShutdownHook(
            Heritrix.getShutdownThread(false, 0, "Heritrix shutdown hook"));
        // Register this heritrix 'container' though we may be inside another
        // tomcat or jboss container.
        try {
            registerContainerJndi();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed jndi container registration.", e);
        }
    }
    
    /**
     * Do inverse of construction. Used by anyone who does a 'new Heritrix' when
     * they want to cleanup the instance.
     * Of note, there may be Heritrix threads still hanging around after the
     * call to destroy completes.  They'll eventually go down after they've
     * finished their cleanup routines.  In particular, if you are watching
     * Heritrix via JMX, you can see the Heritrix instance JMX bean unregister
     * ahead of the CrawlJob JMX bean that its hosting.
     */
    public void destroy() {
        stop();
        try {
            Heritrix.unregisterHeritrix(this);
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        this.jobHandler = null;
        this.openMBeanInfo = null;
    }
    
    /**
     * Launch program.
     * Optionally will launch a web server to host UI.  Will also register
     * Heritrix MBean with first found JMX Agent (Usually the 1.5.0 JVM
     * Agent).
     * 
     * @param args Command line arguments.
     * @throws Exception
     */
    public static void main(String[] args)
    throws Exception {
        Heritrix.commandLine = true;
        
        // Set timezone here.  Would be problematic doing it if we're running
        // inside in a container.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        
        File startLog = new File(getHeritrixHome(), STARTLOG);
        Heritrix.out = new PrintWriter(isDevelopment()? 
            System.out: new PrintStream(new FileOutputStream(startLog)));
        
        try {
            containerInitialization();
            String status = doCmdLineArgs(args);
            if (status != null) {
                Heritrix.out.println(status);
            }
        }

        catch(Exception e) {
            // Show any exceptions in STARTLOG.
            e.printStackTrace(Heritrix.out);
            throw e;
        }

        finally {
            // If not development, close the file that signals the wrapper
            // script that we've started.  Otherwise, just flush it; if in
            // development, the output is probably a console.
            if (!isDevelopment()) {
                if (Heritrix.out != null) {
                    Heritrix.out.close();
                }
                System.out.println("Heritrix version: " +
                        Heritrix.getVersion());
            } else {
                if (Heritrix.out != null) {
                    Heritrix.out.flush();
                }
            }
        }
    }
    
    protected static String doCmdLineArgs(final String [] args)
    throws Exception {
        // Get defaults for commandline arguments from the properties file.
        String tmpStr = PropertyUtils.
            getPropertyOrNull("heritrix.context");
        if (tmpStr != null)  {
            Heritrix.adminContext = tmpStr;
        }
        tmpStr = PropertyUtils.getPropertyOrNull("heritrix.cmdline.port");
        if (tmpStr != null) {
            Heritrix.guiPort = Integer.parseInt(tmpStr);
        }
        tmpStr = PropertyUtils.getPropertyOrNull("heritrix.cmdline.admin");
        String adminLoginPassword = (tmpStr == null)? "": tmpStr;
        String crawlOrderFile =
            PropertyUtils.getPropertyOrNull("heritrix.cmdline.order");
        tmpStr = PropertyUtils.getPropertyOrNull("heritrix.cmdline.run");
        boolean runMode =
            PropertyUtils.getBooleanProperty("heritrix.cmdline.run");
        boolean selfTest = false;
        String selfTestName = null;
        CommandLineParser clp = new CommandLineParser(args, Heritrix.out,
            Heritrix.getVersion());
        List arguments = clp.getCommandLineArguments();
        Option [] options = clp.getCommandLineOptions();

        // Check passed argument.  Only one argument, the ORDER_FILE is allowed.
        // If one argument, make sure exists and xml suffix.
        if (arguments.size() > 1) {
            clp.usage(1);
        } else if (arguments.size() == 1) {
            crawlOrderFile = (String)arguments.get(0);
            if (!(new File(crawlOrderFile).exists())) {
                clp.usage("ORDER.XML <" + crawlOrderFile +
                    "> specified does not exist.", 1);
            }
            // Must end with '.xml'
            if (crawlOrderFile.length() > 4 &&
                    !crawlOrderFile.substring(crawlOrderFile.length() - 4).
                        equalsIgnoreCase(".xml")) {
                clp.usage("ORDER.XML <" + crawlOrderFile +
                    "> does not have required '.xml' suffix.", 1);
            }
        }

        // Now look at options passed.
        for (int i = 0; i < options.length; i++) {
            switch(options[i].getId()) {
                case 'h':
                    clp.usage();
                    break;

                case 'a':
                    adminLoginPassword = options[i].getValue();
                    break;

                case 'n':
                    if (crawlOrderFile == null) {
                        clp.usage("You must specify an ORDER_FILE with" +
                            " '--nowui' option.", 1);
                    }
                    Heritrix.gui = false;
                    break;
                
                case 'b':
                    Heritrix.guiHosts = parseHosts(options[i].getValue());
                    break;

                case 'p':
                    try {
                        Heritrix.guiPort =
                            Integer.parseInt(options[i].getValue());
                    } catch (NumberFormatException e) {
                        clp.usage("Failed parse of port number: " +
                            options[i].getValue(), 1);
                    }
                    if (Heritrix.guiPort <= 0) {
                        clp.usage("Nonsensical port number: " +
                            options[i].getValue(), 1);
                    }
                    break;

                case 'r':
                    runMode = true;
                    break;

                case 's':
                    selfTestName = options[i].getValue();
                    selfTest = true;
                    break;

                default:
                    assert false: options[i].getId();
            }
        }

        // Ok, we should now have everything to launch the program.
        String status = null;
        if (selfTest) {
            // If more than just '--selftest' and '--port' passed, then
            // there is confusion on what is being asked of us.  Print usage
            // rather than proceed.
            for (int i = 0; i < options.length; i++) {
                if (options[i].getId() != 'p' && options[i].getId() != 's') {
                    clp.usage(1);
                }
            }

            if (arguments.size() > 0) {
                // No arguments accepted by selftest.
                clp.usage(1);
            }
            status = selftest(selfTestName, Heritrix.guiPort);
        } else {
			if (!Heritrix.gui) {
				if (options.length > 1) {
					// If more than just '--nowui' passed, then there is
					// confusion on what is being asked of us. Print usage
					// rather than proceed.
					clp.usage(1);
				}
				Heritrix h = new Heritrix(true);
				status = h.doOneCrawl(crawlOrderFile);
			} else {
                if (!isValidLoginPasswordString(adminLoginPassword)) {
                    // exit printing usage info if no webui login:password given
                    clp.usage("Invalid admin login:password value, or none "
                            + "specified. ", 1);
                }
				status = startEmbeddedWebserver(
                        Heritrix.guiHosts, Heritrix.guiPort,
						adminLoginPassword);
				Heritrix h = new Heritrix(true);

				String tmp = h.launch(crawlOrderFile, runMode);
				if (tmp != null) {
					status += ('\n' + tmp);
				}
			}
		}
        return status;
    }
    
    /**
	 * @return The file we dump stdout and stderr into.
	 */
    public static String getHeritrixOut() {
        String tmp = System.getProperty("heritrix.out");
        if (tmp == null || tmp.length() == 0) {
            tmp = Heritrix.DEFAULT_HERITRIX_OUT;
        }
        return tmp;
    }

    /**
     * Exploit <code>-Dheritrix.home</code> if available to us.
     * Is current working dir if no heritrix.home property supplied.
     * @return Heritrix home directory.
     * @throws IOException
     */
    protected static File getHeritrixHome()
    throws IOException {
        File heritrixHome = null;
        String home = System.getProperty("heritrix.home");
        if (home != null && home.length() > 0) {
            heritrixHome = new File(home);
            if (!heritrixHome.exists()) {
                throw new IOException("HERITRIX_HOME <" + home +
                    "> does not exist.");
            }
        } else {
            heritrixHome = new File(new File("").getAbsolutePath());
        }
        return heritrixHome;
    }
    
    /**
     * @return The directory into which we put jobs.  If the system property
     * 'heritrix.jobsdir' is set, we will use its value in place of the default
     * 'jobs' directory in the current working directory.
     * @throws IOException
     */
    public static File getJobsdir() throws IOException {
        Heritrix.loadProperties(); // if called in constructor
        String jobsdirStr = System.getProperty("heritrix.jobsdir", "jobs");
        File jobsdir = new File(jobsdirStr);
        return (jobsdir.isAbsolute())?
            jobsdir:
            new File(getHeritrixHome(), jobsdirStr);
    }
    
    /**
     * Get and check for existence of expected subdir.
     *
     * If development flag set, then look for dir under src dir.
     *
     * @param subdirName Dir to look for.
     * @return The extant subdir.  Otherwise null if we're running
     * in a webapp context where there is no conf directory available.
     * @throws IOException if unable to find expected subdir.
     */
    protected static File getSubDir(String subdirName)
    throws IOException {
        return getSubDir(subdirName, true);
    }
    
    /**
     * Get and optionally check for existence of subdir.
     *
     * If development flag set, then look for dir under src dir.
     *
     * @param subdirName Dir to look for.
     * @param fail True if we are to fail if directory does not
     * exist; false if we are to return false if the directory does not exist.
     * @return The extant subdir.  Otherwise null if we're running
     * in a webapp context where there is no subdir directory available.
     * @throws IOException if unable to find expected subdir.
     */
    protected static File getSubDir(String subdirName, boolean fail)
    throws IOException {
        String path = isDevelopment()?
            "src" + File.separator + subdirName:
            subdirName;
        File dir = new File(getHeritrixHome(), path);
        if (!dir.exists()) {
            if (fail) {
                throw new IOException("Cannot find subdir: " + subdirName);
            }
            dir = null;
        }
        return dir;
    }
    
    /**
     * Test string is valid login/password string.
     *
     * A valid login/password string has the login and password compounded
     * w/ a ':' delimiter.
     *
     * @param str String to test.
     * @return True if valid password/login string.
     */
    protected static boolean isValidLoginPasswordString(String str) {
        boolean isValid = false;
        StringTokenizer tokenizer = new StringTokenizer(str,  ":");
        if (tokenizer.countTokens() == 2) {
            String login = ((String)tokenizer.nextElement()).trim();
            String password = ((String)tokenizer.nextElement()).trim();
            if (login.length() > 0 && password.length() > 0) {
                isValid = true;
            }
        }
        return isValid;
    }

    protected static boolean isDevelopment() {
        return System.getProperty("heritrix.development") != null;
    }

    /**
     * Load the heritrix.properties file.
     * 
     * Adds any property that starts with
     * <code>HERITRIX_PROPERTIES_PREFIX</code>
     * or <code>ARCHIVE_PACKAGE</code>
     * into system properties (except logging '.level' directives).
     * @return Loaded properties.
     * @throws IOException
     */
    protected static Properties loadProperties()
    throws IOException {
        if (Heritrix.propertiesLoaded) {
            return System.getProperties();
        }
        Heritrix.propertiesLoaded = true;
            
        Properties properties = new Properties();
        properties.load(getPropertiesInputStream());
        
        // Any property that begins with ARCHIVE_PACKAGE, make it
        // into a system property. While iterating, check to see if anything
        // defined on command-line, and if so, it overrules whats in
        // heritrix.properties.
        for (Enumeration e = properties.keys(); e.hasMoreElements();) {
            String key = ((String)e.nextElement()).trim();
        	if (key.startsWith(ARCHIVE_PACKAGE) ||
                    key.startsWith(HERITRIX_PROPERTIES_PREFIX)) {
                // Don't add the heritrix.properties entries that are
                // changing the logging level of particular classes.
                String value = properties.getProperty(key).trim();
                if (key.indexOf(".level") < 0) {
                    copyToSystemProperty(key, value);
                }
            }  else if (key.startsWith(SYSTEM_PREFIX)) {
                String value = properties.getProperty(key).trim();
                copyToSystemProperty(key.substring(SYSTEM_PREFIX.length()), value); 
            }
        }
        return properties;
    }

    /**
     * Copy the given key-value into System properties, as long as there
     * is no existing value. 
     * @param key property key 
     * @param value property value
     */
    protected static void copyToSystemProperty(String key, String value) {
        if (System.getProperty(key) == null ||
            System.getProperty(key).length() == 0) {
            System.setProperty(key, value);
        }
    }

    protected static InputStream getPropertiesInputStream()
    throws IOException {
        File file = null;
        // Look to see if properties have been passed on the cmd-line.
        String alternateProperties = System.getProperty(PROPERTIES_KEY);
        if (alternateProperties != null && alternateProperties.length() > 0) {
            file = new File(alternateProperties);
        }
        // Get properties from conf directory if one available.
        if ((file == null || !file.exists()) && getConfdir(false) != null) {
            file = new File(getConfdir(), PROPERTIES);
            if (!file.exists()) {
                // If no properties file in the conf dir, set file back to
                // null so we go looking for heritrix.properties on classpath.
                file = null;
            }
        }
        // If not on the command-line, there is no conf dir. Then get the
        // properties from the CLASSPATH (Classpath file separator is always
        // '/', whatever the platform.
        InputStream is = (file != null)?
            new FileInputStream(file):
            Heritrix.class.getResourceAsStream("/" + PROPERTIES_KEY);
        if (is == null) {
            throw new IOException("Failed to load properties file from" +
                " filesystem or from classpath.");
        }
        return is;
    }

    /**
     * If the user hasn't altered the default logging parameters, tighten them
     * up somewhat: some of our libraries are way too verbose at the INFO or
     * WARNING levels.
     * 
     * This might be a problem running inside in someone else's
     * container.  Container's seem to prefer commons logging so we
     * ain't messing them doing the below.
     *
     * @throws IOException
     * @throws SecurityException
     */
    protected static void patchLogging()
    throws SecurityException, IOException {
        if (System.getProperty("java.util.logging.config.class") != null) {
            return;
        }

        if (System.getProperty("java.util.logging.config.file") != null) {
            return;
        }

        // No user-set logging properties established; use defaults
        // from distribution-packaged 'heritrix.properties'.
        LogManager.getLogManager().
            readConfiguration(getPropertiesInputStream());
    }

    /**
     * Configure our trust store.
     *
     * If system property is defined, then use it for our truststore.  Otherwise
     * use the heritrix truststore under conf directory if it exists.
     * 
     * <p>If we're not launched from the command-line, we will not be able
     * to find our truststore.  The truststore is nor normally used so rare
     * should this be a problem (In case where we don't use find our trust
     * store, we'll use the 'default' -- either the JVMs or the containers).
     */
    protected static void configureTrustStore() {
        // Below must be defined in jsse somewhere but can' find it.
        final String TRUSTSTORE_KEY = "javax.net.ssl.trustStore";
        String value = System.getProperty(TRUSTSTORE_KEY);
        File confdir = null;
        try {
            confdir = getConfdir(false);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to get confdir.", e);
        }
        if ((value == null || value.length() <= 0) && confdir != null) {
            // Use the heritrix store if it exists on disk.
            File heritrixStore = new File(confdir, "heritrix.cacerts");
            if(heritrixStore.exists()) {
                value = heritrixStore.getAbsolutePath();
            }
        }

        if (value != null && value.length() > 0) {
            System.setProperty(TRUSTSTORE_KEY, value);
        }
    }

    /**
     * Run the selftest
     *
     * @param oneSelfTestName Name of a test if we are to run one only rather
     * than the default running all tests.
     * @param port Port number to use for web UI.
     *
     * @exception Exception
     * @return Status of how selftest startup went.
     */
    protected static String selftest(final String oneSelfTestName,
            final int port)
        throws Exception {
        // Put up the webserver w/ the root and selftest webapps only.
        final String SELFTEST = "selftest";
        Heritrix.httpServer = new SimpleHttpServer(SELFTEST,
            Heritrix.adminContext, LOCALHOST_ONLY, port, true);
        // Set up digest auth for a section of the server so selftest can run
        // auth tests.  Looks like can only set one login realm going by the
        // web.xml dtd.  Otherwise, would be nice to selftest basic and digest.
        // Have login, password and role all be SELFTEST.  Must match what is
        // in the selftest order.xml file.
        Heritrix.httpServer.setAuthentication(SELFTEST, Heritrix.adminContext,
            SELFTEST, SELFTEST, SELFTEST);
        Heritrix.httpServer.startServer();
        // Get the order file from the CLASSPATH unless we're running in dev
        // environment.
        File selftestDir = (isDevelopment())?
            new File(getConfdir(), SELFTEST):
            new File(File.separator + SELFTEST);
        File crawlOrderFile = new File(selftestDir, "order.xml");
        // Create a job based off the selftest order file.  Then use this as
        // a template to pass jobHandler.newJob().  Doing this gets our
        // selftest output to show under the jobs directory.
        // Pass as a seed a pointer to the webserver we just put up.
        final String ROOTURI = "127.0.0.1:" + Integer.toString(port);
        String selfTestUrl = "http://" + ROOTURI + '/';
        if (oneSelfTestName != null && oneSelfTestName.length() > 0) {
            selfTestUrl += (oneSelfTestName + '/');
        }
        CrawlJobHandler cjh = new SelfTestCrawlJobHandler(getJobsdir(),
                oneSelfTestName, selfTestUrl);
        Heritrix h = new Heritrix("Selftest", true, cjh);
        CrawlJob job = createCrawlJob(cjh, crawlOrderFile, "Template");
        job = h.getJobHandler().newJob(job, null, SELFTEST,
            "Integration self test", selfTestUrl, CrawlJob.PRIORITY_AVERAGE);
        h.getJobHandler().addJob(job);
        // Before we start, need to change some items in the settings file.
        CredentialStore cs = (CredentialStore)job.getSettingsHandler().
            getOrder().getAttribute(CredentialStore.ATTR_NAME);
        for (Iterator i = cs.iterator(null); i.hasNext();) {
            ((Credential)i.next()).setCredentialDomain(null, ROOTURI);
        }
        h.getJobHandler().startCrawler();
        StringBuffer buffer = new StringBuffer();
        buffer.append("Heritrix " + Heritrix.getVersion() +
                " selftest started.");
        buffer.append("\nSelftest first crawls " + selfTestUrl +
            " and then runs an analysis.");
        buffer.append("\nResult of analysis printed to " +
            getHeritrixOut() + " when done.");
        buffer.append("\nSelftest job directory for logs and arcs:\n" +
            job.getDirectory().getAbsolutePath());
        return buffer.toString();
    }

    /**
     * Launch the crawler without a web UI and run the passed crawl only.
     * 
     * Specialized version of {@link #launch()}.
     *
     * @param crawlOrderFile The crawl order to crawl.
     * @throws InitializationException
     * @throws InvalidAttributeValueException
     * @return Status string.
     */
    protected String doOneCrawl(String crawlOrderFile)
    throws InitializationException, InvalidAttributeValueException {
        return doOneCrawl(crawlOrderFile, null);
    }
    
    /**
     * Launch the crawler without a web UI and run passed crawl only.
     * 
     * Specialized version of {@link #launch()}.
     *
     * @param crawlOrderFile The crawl order to crawl.
     * @param listener Register this crawl status listener before starting
     * crawl (You can use this listener to notice end-of-crawl).
     * @throws InitializationException
     * @throws InvalidAttributeValueException
     * @return Status string.
     */
    protected String doOneCrawl(String crawlOrderFile,
        CrawlStatusListener listener)
    throws InitializationException, InvalidAttributeValueException {
        XMLSettingsHandler handler =
            new XMLSettingsHandler(new File(crawlOrderFile));
        handler.initialize();
        CrawlController controller = new CrawlController();
        controller.initialize(handler);
        if (listener != null) {
            controller.addCrawlStatusListener(listener);
        }
        controller.requestCrawlStart();
        return "Crawl started using " + crawlOrderFile + ".";
    }
    
    /**
     * Launch the crawler for a web UI.
     *
     * Crawler hangs around waiting on jobs.
     *
     * @exception Exception
     * @return A status string describing how the launch went.
     * @throws Exception
     */
    public String launch() throws Exception {
        return launch(null, false);
    }

    /**
     * Launch the crawler for a web UI.
     *
     * Crawler hangs around waiting on jobs.
     * 
     * @param crawlOrderFile File to crawl.  May be null.
     * @param runMode Whether crawler should be set to run mode.
     *
     * @exception Exception
     * @return A status string describing how the launch went.
     */
    public String launch(String crawlOrderFile, boolean runMode)
    throws Exception {
        String status = null;
        if (crawlOrderFile != null) {
            addCrawlJob(crawlOrderFile, "Autolaunched", "", "");
            if(runMode) {
                this.jobHandler.startCrawler();
                status = "Job being crawled: " + crawlOrderFile;
            } else {
                status = "Crawl job ready and pending: " + crawlOrderFile;
            }
        } else if(runMode) {
            // The use case is that jobs are to be run on a schedule and that
            // if the crawler is in run mode, then the scheduled job will be
            // run at appropriate time.  Otherwise, not.
            this.jobHandler.startCrawler();
            status = "Crawler set to run mode.";
        }
        return status;
    }
    
    /**
     * Start up the embedded Jetty webserver instance.
     * This is done when we're run from the command-line.
     * @param port Port number to use for web UI.
     * @param adminLoginPassword Compound of login and password.
     * @throws Exception
     * @return Status on webserver startup.
     * @deprecated  Use startEmbeddedWebserver(hosts, port, adminLoginPassword)
     */
    protected static String startEmbeddedWebserver(final int port,
        final boolean lho, final String adminLoginPassword)
    throws Exception {
        ArrayList<String> hosts = new ArrayList<String>();
        if (lho) {
            hosts.add("127.0.0.1");
        }
        return startEmbeddedWebserver(hosts, port, adminLoginPassword);
    }

    
    /**
     * Parses a list of host names.
     * 
     * <p>If the given string is <code>/</code>, then an empty
     * collection is returned.  This indicates that all available network
     * interfaces should be used.
     * 
     * <p>Otherwise, the string must contain a comma-separated list of 
     * IP addresses or host names.  The parsed list is then returned.
     * 
     * @param hosts  the string to parse
     * @return  the parsed collection of hosts 
     */
    private static Collection<String> parseHosts(String hosts) {
        hosts = hosts.trim();
        if (hosts.equals("/")) {
            return new ArrayList<String>(1);
        }
        String[] hostArray = hosts.split(",");
        for (int i = 0; i < hostArray.length; i++) {
            hostArray[i] = hostArray[i].trim();
        }
        return Arrays.asList(hostArray);
    }
    
    /**
     * Start up the embedded Jetty webserver instance.
     * This is done when we're run from the command-line.
     * 
     * @param hosts  a list of IP addresses or hostnames to bind to, or an
     *               empty collection to bind to all available network 
     *               interfaces
     * @param port Port number to use for web UI.
     * @param adminLoginPassword Compound of login and password.
     * @throws Exception
     * @return Status on webserver startup.
     */
    protected static String startEmbeddedWebserver(Collection<String> hosts, 
        int port, String adminLoginPassword) 
    throws Exception {
        adminUsername = adminLoginPassword.
            substring(0, adminLoginPassword.indexOf(":"));
        adminPassword = adminLoginPassword.
            substring(adminLoginPassword.indexOf(":") + 1);
        Heritrix.httpServer = new SimpleHttpServer("admin",
            Heritrix.adminContext, hosts, port, false);
        
        final String DOTWAR = ".war";
        final String SELFTEST = "selftest";
        
        // Look for additional WAR files beyond 'selftest' and 'admin'.
        File[] wars = getWarsdir().listFiles();
        for(int i = 0; i < wars.length; i++) {
            if(wars[i].isFile()) {
                final String warName = wars[i].getName();
                final String warNameNC = warName.toLowerCase();
                if(warNameNC.endsWith(DOTWAR) &&
                        !warNameNC.equals(ADMIN + DOTWAR) &&
                        !warNameNC.equals(SELFTEST + DOTWAR)) {
                    int dot = warName.indexOf('.');
                    Heritrix.httpServer.addWebapp(warName.substring(0, dot),
                            null, true);
                }
            }
        }
        
        // Name of passed 'realm' must match what is in configured in web.xml.
        // We'll use ROLE for 'realm' and 'role'.
        final String ROLE = ADMIN;
        Heritrix.httpServer.setAuthentication(ROLE, Heritrix.adminContext,
            adminUsername, adminPassword, ROLE);
        Heritrix.httpServer.startServer();
        StringBuffer buffer = new StringBuffer();
        buffer.append("Heritrix " + Heritrix.getVersion() + " is running.");
        for (String host: httpServer.getHosts()) {
            buffer.append("\nWeb console is at: http://");
            buffer.append(host).append(':').append(port);
        }
        buffer.append("\nWeb console login and password: " +
            adminUsername + "/" + adminPassword);
        return buffer.toString();
    }
    
    /**
     * Replace existing administrator login info with new info.
     * 
     * @param newUsername new administrator login username
     * @param newPassword new administrator login password
     */
    public static void resetAuthentication(String newUsername,
            String newPassword) {
        Heritrix.httpServer.resetAuthentication(ADMIN, adminUsername,
                newUsername, newPassword);
        adminUsername = newUsername;
        adminPassword = newPassword; 
        logger.info("administrative login changed to "
                +newUsername+":"+newPassword);
    }

    protected static CrawlJob createCrawlJob(CrawlJobHandler handler,
            File crawlOrderFile, String name)
    throws InvalidAttributeValueException {
        XMLSettingsHandler settings = new XMLSettingsHandler(crawlOrderFile);
        settings.initialize();
        return new CrawlJob(handler.getNextJobUID(), name, settings,
            new CrawlJobErrorHandler(Level.SEVERE),
            CrawlJob.PRIORITY_HIGH,
            crawlOrderFile.getAbsoluteFile().getParentFile());
    }
    
    /**
     * This method is called when we have an order file to hand that we want
     * to base a job on.  It leaves the order file in place and just starts up
     * a job that uses all the order points to for locations for logs, etc.
     * @param orderPathOrUrl Path to an order file or to a seeds file.
     * @param name Name to use for this job.
     * @param description 
     * @param seeds 
     * @return A status string.
     * @throws IOException 
     * @throws FatalConfigurationException 
     */
    public String addCrawlJob(String orderPathOrUrl, String name,
            String description, String seeds)
    throws IOException, FatalConfigurationException {
        if (!UURI.hasScheme(orderPathOrUrl)) {
            // Assume its a file path.
            return addCrawlJob(new File(orderPathOrUrl), name, description,
                    seeds);
        }

        // Otherwise, must be an URL.
        URL url = new URL(orderPathOrUrl);

        // Handle http and file only for now (Tried to handle JarUrlConnection
        // but too awkward undoing jar stream.  Rather just look for URLs that
        // end in '.jar').
        String result = null;
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) {
            result = addCrawlJob(url, (HttpURLConnection)connection, name,
                description, seeds);
        } else if (connection instanceof FileURLConnection) {
            result = addCrawlJob(new File(url.getPath()), name, description,
                seeds);
        } else {
            throw new UnsupportedOperationException("No support for "
                + connection);
        }

        return result;
    }
    
    protected String addCrawlJob(final URL url,
            final HttpURLConnection connection,
            final String name, final String description, final String seeds)
    throws IOException, FatalConfigurationException {
        connection.connect();
        // Look see if its a jar file.  If it is undo it.
        boolean isJar = url.getPath() != null
                && url.getPath().toLowerCase().endsWith(JAR_SUFFIX)
                || "application/java-archive".equals(connection
                        .getHeaderField("Content-Type"));
        // If http url connection, bring down the resource local.
        File localFile = File.createTempFile(Heritrix.class.getName(),
           isJar? JAR_SUFFIX: null, TMPDIR);
        String result = null;
        try {
            IoUtils.readFullyToFile(connection.getInputStream(), localFile);
            result = addCrawlJob(localFile, name, description, seeds);
        } catch (IOException ioe) {
            // Cleanup if an Exception.
            localFile.delete();
            localFile = null;
        } finally {
             connection.disconnect();
             // If its a jar file, then we made a job based on the jar contents.
             // Its no longer needed.  Remove it.  If not a jar file, then leave
             // the file around because the job depends on it.
             if (isJar && localFile != null && localFile.exists()) {
                 localFile.delete();
             }
        }
        return result;
    }
    
    protected String addCrawlJob(final File order, final String name,
            final String description, final String seeds)
    throws FatalConfigurationException, IOException {
        CrawlJob addedJob = null;
        if (this.jobHandler == null) {
            throw new NullPointerException("Heritrix jobhandler is null.");
        }
        try {
            if (order.getName().toLowerCase().endsWith(JAR_SUFFIX)) {
                return addCrawlJobBasedonJar(order, name, description, seeds);
            }
            addedJob = this.jobHandler.
                addJob(createCrawlJob(this.jobHandler, order, name));
        } catch (InvalidAttributeValueException e) {
            FatalConfigurationException fce = new FatalConfigurationException(
                "Converted InvalidAttributeValueException on " +
                order.getAbsolutePath() + ": " + e.getMessage());
            fce.setStackTrace(e.getStackTrace());
        }
        return addedJob != null? addedJob.getUID(): null;
    }
    
    /**
     * Undo jar file and use as basis for a new job.
     * @param jarFile Pointer to file that holds jar.
     * @param name Name to use for new job.
     * @param description 
     * @param seeds 
     * @return Message.
     * @throws IOException
     * @throws FatalConfigurationException
     */
    protected String addCrawlJobBasedonJar(final File jarFile,
            final String name, final String description, final String seeds)
    throws IOException, FatalConfigurationException {
        if (jarFile == null || !jarFile.exists()) {
            throw new FileNotFoundException(jarFile.getAbsolutePath());
        }
        // Create a directory with a tmp name.  Do it by first creating file,
        // removing it, then creating the directory. There is a hole during
        // which the OS may put a file of same exact name in our way but
        // unlikely.
        File dir = File.createTempFile(Heritrix.class.getName(), ".expandedjar",
            TMPDIR);
        dir.delete();
        dir.mkdir();
        try {
            org.archive.crawler.util.IoUtils.unzip(jarFile, dir);
            // Expect to find an order file at least.
            File orderFile = new File(dir, "order.xml");
            if (!orderFile.exists()) {
                throw new IOException("Missing order: " +
                    orderFile.getAbsolutePath());
            }
            CrawlJob job =
                createCrawlJobBasedOn(orderFile, name, description, seeds);
            // Copy into place any seeds and settings directories before we
            // add job to Heritrix to crawl.
            File seedsFile = new File(dir, "seeds.txt");
            if (seedsFile.exists()) {
                FileUtils.copyFiles(seedsFile, new File(job.getDirectory(),
                    seedsFile.getName()));
            }
            addCrawlJob(job);
            return job.getUID();
        } catch (RuntimeException e) {
            logger.severe("problem adding crawl job from order jar " + jarFile + ": " + e);
            throw new FatalConfigurationException(e.toString());
         } finally {
             // After job has been added, no more need of expanded content.
             // (Let the caller be responsible for cleanup of jar. Sometimes
             // its should be deleted -- when its a local copy of a jar pulled
             // across the net -- wherease other times, if its a jar passed
             // in w/ a 'file' scheme, it shouldn't be deleted.
             org.archive.util.FileUtils.deleteDir(dir);
         }
    }
    
    public String addCrawlJobBasedOn(String jobUidOrProfile,
            String name, String description, String seeds) {
        try {
            CrawlJob cj = getJobHandler().getJob(jobUidOrProfile);
            if (cj == null) {
                throw new InvalidAttributeValueException(jobUidOrProfile +
                    " is not a job UID or profile name (Job UIDs are " +
                    " usually the 14 digit date portion of job name).");
            }
            CrawlJob job = addCrawlJobBasedOn(
                cj.getSettingsHandler().getOrderFile(), name, description,
                    seeds);
            return job.getUID();
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception on " + jobUidOrProfile + ": " + e.getMessage();
        } 
    }
    
    protected CrawlJob addCrawlJobBasedOn(final File orderFile,
        final String name, final String description, final String seeds)
    throws FatalConfigurationException {
        return addCrawlJob(createCrawlJobBasedOn(orderFile, name, description,
                seeds));
    }
    
    protected CrawlJob createCrawlJobBasedOn(final File orderFile,
            final String name, final String description, final String seeds)
    throws FatalConfigurationException {
        CrawlJob job = getJobHandler().newJob(orderFile, name, description,
                seeds);
        return CrawlJobHandler.ensureNewJobWritten(job, name, description);
    }
    
    protected CrawlJob addCrawlJob(final CrawlJob job) {
        return getJobHandler().addJob(job);
    }
    
    public void startCrawling() {
        if (getJobHandler() == null) {
            throw new NullPointerException("Heritrix jobhandler is null.");
        }
        getJobHandler().startCrawler();
    }

    public void stopCrawling() {
        if (getJobHandler() == null) {
            throw new NullPointerException("Heritrix jobhandler is null.");
        }
        getJobHandler().stopCrawler();
    }
    
    /**
     * Get the heritrix version.
     *
     * @return The heritrix version.  May be null.
     */
    public static String getVersion() {
        return System.getProperty("heritrix.version");
    }

    /**
     * Get the job handler
     *
     * @return The CrawlJobHandler being used.
     */
    public CrawlJobHandler getJobHandler() {
        return this.jobHandler;
    }

    /**
     * Get the configuration directory.
     * @return The conf directory under HERITRIX_HOME or null if none can
     * be found.
     * @throws IOException
     */
    public static File getConfdir()
    throws IOException {
        return getConfdir(true);
    }

    /**
     * Get the configuration directory.
     * @param fail Throw IOE if can't find directory if true, else just
     * return null.
     * @return The conf directory under HERITRIX_HOME or null (or an IOE) if
     * can't be found.
     * @throws IOException
     */
    public static File getConfdir(final boolean fail)
    throws IOException {
        final String key = "heritrix.conf";
        // Look to see if heritrix.conf property passed on the cmd-line.
        String tmp = System.getProperty(key);
        // if not fall back to default $HERITIX_HOME/conf
        if (tmp == null || tmp.length() == 0) {
            return getSubDir("conf", fail);
        }
        File dir = new File(tmp);
        if (!dir.exists()) {
            if (fail) {
                throw new IOException("Cannot find conf dir: " + tmp);
            } else {
                logger.log(Level.WARNING, "Specified " + key +
                    " dir does not exist.  Falling back on default");
            }
            dir = getSubDir("conf", fail);
        }
        return dir;
    }

    /**
     * @return Returns the httpServer. May be null if one was not started.
     */
    public static SimpleHttpServer getHttpServer() {
        return Heritrix.httpServer;
    }

    /**
     * @throws IOException
     * @return Returns the directory under which reside the WAR files
     * we're to load into the servlet container.
     */
    public static File getWarsdir()
    throws IOException {
        return getSubDir("webapps");
    }

    /**
     * Prepars for program shutdown. This method does it's best to prepare the
     * program so that it can exit normally. It will kill the httpServer and
     * terminate any running job.<br>
     * It is advisible to wait a few (~1000) millisec after calling this method
     * and before calling performHeritrixShutDown() to allow as many threads as
     * possible to finish what they are doing.
     */
    public static void prepareHeritrixShutDown() {
        // Stop and destroy all running Heritrix instances.
        // Get array of the key set to avoid CCEs for case where call to
        // destroy does a remove of an instance from Heritrix.instances.
        final Object [] keys = Heritrix.instances.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            ((Heritrix)Heritrix.instances.get(keys[i])).destroy();
        }
        
        try {
            deregisterJndi(getJndiContainerName());
        } catch (NameNotFoundException e) {
            // We were probably unbound already. Ignore.
            logger.log(Level.WARNING, "deregistration of jndi", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if(Heritrix.httpServer != null) {
            // Shut down the web access.
            try {
                Heritrix.httpServer.stopServer();
            } catch (InterruptedException e) {
                // Generally this can be ignored, but we'll print a stack trace
                // just in case.
                e.printStackTrace();
            } finally {
                Heritrix.httpServer = null;
            }
        }
    }

    /**
     * Exit program. Recommended that prepareHeritrixShutDown() be invoked
     * prior to this method.
     */
    public static void performHeritrixShutDown() {
        performHeritrixShutDown(0);
    }

    /**
     * Exit program. Recommended that prepareHeritrixShutDown() be invoked
     * prior to this method.
     *
     * @param exitCode Code to pass System.exit.
     *
     */
    public static void performHeritrixShutDown(int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Shutdown all running heritrix instances and the JVM.
     * Assumes stop has already been called.
	 * @param exitCode Exit code to pass system exit.
	 */
	public static void shutdown(final int exitCode) {
        getShutdownThread(true, exitCode, "Heritrix shutdown").start();
	}
    
    protected static Thread getShutdownThread(final boolean sysexit,
            final int exitCode, final String name) {
        Thread t = new Thread(name) {
            public void run() {
                Heritrix.prepareHeritrixShutDown();
                if (sysexit) {
                    Heritrix.performHeritrixShutDown(exitCode);
                }
            }
        };
        t.setDaemon(true);
        return t;
    }
    
    public static void shutdown() {
        shutdown(0);
    }
    
    /**
     * Register Heritrix with JNDI, JMX, and with the static hashtable of all
     * Heritrix instances known to this JVM.
     * 
     * If launched from cmdline, register Heritrix MBean if an agent to register
     * ourselves with. Usually this method will only have effect if we're
     * running in a 1.5.0 JDK and command line options such as
     * '-Dcom.sun.management.jmxremote.port=8082
     * -Dcom.sun.management.jmxremote.authenticate=false
     * -Dcom.sun.management.jmxremote.ssl=false' are supplied.
     * See <a href="http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html">Monitoring
     * and Management Using JMX</a>
     * for more on the command line options and how to connect to the
     * Heritrix bean using the JDK 1.5.0 jconsole tool.  We register currently
     * with first server we find (TODO: Make configurable).
     * 
     * <p>If we register successfully with a JMX agent, then part of the
     * registration will include our registering ourselves with JNDI.
     * 
     * <p>Finally, add the heritrix instance to the hashtable of all the
     * Heritrix instances floating in the current VM.  This latter registeration
     * happens whether or no there is a JMX agent to register with.  This is
     * a list we keep out of convenience so its easy iterating over all
     *  all instances calling stop when main application is going down.
     * 
     * @param h Instance of heritrix to register.
     * @param name Name to use for this Heritrix instance.
     * @param jmxregister True if we are to register this instance with JMX.
     * @throws NullPointerException
     * @throws MalformedObjectNameException
     * @throws NotCompliantMBeanException 
     * @throws MBeanRegistrationException 
     * @throws InstanceAlreadyExistsException 
     */
    protected static void registerHeritrix(final Heritrix h,
            final String name, final boolean jmxregister)
    throws MalformedObjectNameException, InstanceAlreadyExistsException,
    MBeanRegistrationException, NotCompliantMBeanException {
        MBeanServer server = getMBeanServer();
        if (server != null) {
            // Are we to manage the jmx registration?  Or is it being done for
            // us by an external process: e.g. This instance was created by
            // MBeanAgent.
            if (jmxregister) {
                ObjectName objName = (name == null || name.length() <= 0)?
                    getJmxObjectName(): getJmxObjectName(name);
                registerMBean(server, h, objName);
            }
        } else {
            // JMX ain't available. Put this instance into the list of Heritrix
            // instances so findable by the UI (Normally this is done in the
            // JMX postRegister routine below).  When no JMX, can only have
            // one instance of Heritrix so no need to do the deregisteration.
            Heritrix.instances.put(h.getNoJmxName(), h);
        }
    }
    
    protected static void unregisterHeritrix(final Heritrix h)
    throws InstanceNotFoundException, MBeanRegistrationException,
            NullPointerException {
        MBeanServer server = getMBeanServer();
        if (server != null) {
            server.unregisterMBean(h.mbeanName);
        } else {
            // JMX ain't available. Remove from list of Heritrix instances.
            // Usually this is done by the JMX postDeregister below.
            Heritrix.instances.remove(h.getNoJmxName());
        }
    }
    
    /**
     * Get MBeanServer.
     * Currently uses first MBeanServer found.  This will definetly not be whats
     * always wanted. TODO: Make which server settable. Also, if none, put up
     * our own MBeanServer.
     * @return An MBeanServer to register with or null.
     */
    public static MBeanServer getMBeanServer() {
        MBeanServer result = null;
        List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers == null) {
            return result;
        }
        for (Iterator i = servers.iterator(); i.hasNext();) {
            MBeanServer server = (MBeanServer)i.next();
            if (server == null) {
                continue;
            }
            result = server;
            break;
        }
        return result;
    }
    
    public static MBeanServer registerMBean(final Object objToRegister,
            final String name, final String type)
    throws InstanceAlreadyExistsException, MBeanRegistrationException,
    NotCompliantMBeanException {
        MBeanServer server = getMBeanServer();
        if (server != null) {
            server = registerMBean(server, objToRegister, name, type);
        }
        return server;
    }
    
    public static MBeanServer registerMBean(final MBeanServer server,
            final Object objToRegister, final String name, final String type)
    throws InstanceAlreadyExistsException, MBeanRegistrationException,
    NotCompliantMBeanException {
        try {
            Hashtable<String,String> ht = new Hashtable<String,String>();
            ht.put(JmxUtils.NAME, name);
            ht.put(JmxUtils.TYPE, type);
            registerMBean(server, objToRegister,
                new ObjectName(CRAWLER_PACKAGE, ht));
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        return server;
    }
        
    public static MBeanServer registerMBean(final MBeanServer server,
                final Object objToRegister, final ObjectName objName)
    throws InstanceAlreadyExistsException, MBeanRegistrationException,
    NotCompliantMBeanException {
        server.registerMBean(objToRegister, objName);
        return server;
    }
    
    public static void unregisterMBean(final MBeanServer server,
            final String name, final String type) {
        if (server == null) {
            return;
        }
        try {
            unregisterMBean(server, getJmxObjectName(name, type));
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
    }
            
    public static void unregisterMBean(final MBeanServer server,
            final ObjectName name) {
        try {
            server.unregisterMBean(name);
            logger.info("Unregistered bean " + name.getCanonicalName());
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * @return Name to use when no JMX agent available.
     */
    protected String getNoJmxName() {
        return this.getClass().getName();
    }
    
    public static ObjectName getJmxObjectName()
    throws MalformedObjectNameException, NullPointerException {
        return getJmxObjectName("Heritrix", JmxUtils.SERVICE);
    }
    
    public static ObjectName getJmxObjectName(final String name)
    throws MalformedObjectNameException, NullPointerException {
        return getJmxObjectName(name, JmxUtils.SERVICE);
    }
    
    public static ObjectName getJmxObjectName(final String name,
            final String type)
    throws MalformedObjectNameException, NullPointerException {
        Hashtable<String,String> ht = new Hashtable<String,String>();
        ht.put(JmxUtils.NAME, name);
        ht.put(JmxUtils.TYPE, type);
        return new ObjectName(CRAWLER_PACKAGE, ht);
    }
    
    /**
     * @return Returns true if Heritrix was launched from the command line.
     * (When launched from command line, we do stuff like put up a web server
     * to manage our web interface and we register ourselves with the first
     * available jmx agent).
     */
    public static boolean isCommandLine() {
        return Heritrix.commandLine;
    }
    
    /**
     * @return True if heritrix has been started.
     */
    public boolean isStarted() {
        return this.jobHandler != null;
    }
    
    public String getStatus() {
        StringBuffer buffer = new StringBuffer();
        if (this.getJobHandler() != null) {
            buffer.append("isRunning=");
            buffer.append(this.getJobHandler().isRunning());
            buffer.append(" isCrawling=");
            buffer.append(this.getJobHandler().isCrawling());
            buffer.append(" alertCount=");
            buffer.append(getAlertsCount());
            buffer.append(" newAlertCount=");
            buffer.append(getNewAlertsCount());
            if (this.getJobHandler().isCrawling()) {
                buffer.append(" currentJob=");
                buffer.append(this.getJobHandler().getCurrentJob().
                    getJmxJobName());
            }
        }
        return buffer.toString();
    }
    
    // Alert methods.
    public int getAlertsCount() {
        return this.alertManager.getCount();
    }
    
    public int getNewAlertsCount() {
        return this.alertManager.getNewCount();
    }
    
    public Vector getAlerts() {
        return this.alertManager.getAll();
    }
    
    public Vector getNewAlerts() {
        return this.alertManager.getNewAll();
    }
    
    public SinkHandlerLogRecord getAlert(final String id) {
        return this.alertManager.get(id);
    }
    
    public void readAlert(final String id) {
        this.alertManager.read(id);
    }
    
    public void removeAlert(final String id) {
        this.alertManager.remove(id);
    }
    
    /**
     * Start Heritrix.
     * 
     * Used by JMX and webapp initialization for starting Heritrix.
     * Not by the cmdline launched Heritrix. Idempotent.
     * If start is called by JMX, then new instance of Heritrix is automatically
     * registered w/ JMX Agent.  If started by webapp, need to register the new
     * Heritrix instance.
     */
    public void start() {
        // Don't start if we've been launched from the command line.
        // Don't start if already started.
        if (!Heritrix.isCommandLine() && !isStarted()) {
            try {
                logger.info(launch());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Stop Heritrix.
     * 
     * Used by JMX and webapp initialization for stopping Heritrix.
     */
    public void stop() {
        if (this.jobHandler != null) {
            this.jobHandler.stop();
        }
    }

    public String interrupt(String threadName) {
        String result = "Thread " + threadName + " not found";
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        if (group == null) {
            return result;
        }
        // Back up to the root threadgroup before starting
        // to iterate over threads.
        ThreadGroup parent = null;
        while((parent = group.getParent()) != null) {
            group = parent;
        }
        // Do an array that is twice the size of active
        // thread count.  That should be big enough.
        final int max = group.activeCount() * 2;
        Thread [] threads = new Thread[max];
        int threadCount = group.enumerate(threads, true);
        if (threadCount >= max) {
            logger.info("Some threads not found...array too small: " +
                max);
        }
        for (int j = 0; j < threadCount; j++) {
            if (threads[j].getName().equals(threadName)) {
                threads[j].interrupt();
                result = "Interrupt sent to " + threadName;
                break;
            }
        }
        return result;
    }

    // OpenMBean implementation.
    
    /**
     * Build up the MBean info for Heritrix main.
     * @return Return created mbean info instance.
     */
    protected OpenMBeanInfoSupport buildMBeanInfo() {
        OpenMBeanAttributeInfoSupport[] attributes =
            new OpenMBeanAttributeInfoSupport[Heritrix.ATTRIBUTE_LIST.size()];
        OpenMBeanConstructorInfoSupport[] constructors =
            new OpenMBeanConstructorInfoSupport[1];
        OpenMBeanOperationInfoSupport[] operations =
            new OpenMBeanOperationInfoSupport[Heritrix.OPERATION_LIST.size()];
        MBeanNotificationInfo[] notifications =
            new MBeanNotificationInfo[0];

        // Attributes.
        attributes[0] =
            new OpenMBeanAttributeInfoSupport(Heritrix.STATUS_ATTR,
                "Short basic status message", SimpleType.STRING, true,
                false, false);
        // Attributes.
        attributes[1] =
            new OpenMBeanAttributeInfoSupport(Heritrix.VERSION_ATTR,
                "Heritrix version", SimpleType.STRING, true, false, false);
        // Attributes.
        attributes[2] =
            new OpenMBeanAttributeInfoSupport(Heritrix.ISRUNNING_ATTR,
                "Whether the crawler is running", SimpleType.BOOLEAN, true,
                false, false);
        // Attributes.
        attributes[3] =
            new OpenMBeanAttributeInfoSupport(Heritrix.ISCRAWLING_ATTR,
                "Whether the crawler is crawling", SimpleType.BOOLEAN, true,
                false, false);
        // Attributes.
        attributes[4] =
            new OpenMBeanAttributeInfoSupport(Heritrix.ALERTCOUNT_ATTR,
                "The number of alerts", SimpleType.INTEGER, true, false, false);
        // Attributes.
        attributes[5] =
            new OpenMBeanAttributeInfoSupport(Heritrix.NEWALERTCOUNT_ATTR,
                "The number of new alerts", SimpleType.INTEGER, true, false,
                false);
        // Attributes.
        attributes[6] =
            new OpenMBeanAttributeInfoSupport(Heritrix.CURRENTJOB_ATTR,
                "The name of the job currently being crawled", 
                SimpleType.STRING, true, false, false);

        // Constructors.
        constructors[0] = new OpenMBeanConstructorInfoSupport(
            "HeritrixOpenMBean", "Constructs Heritrix OpenMBean instance ",
            new OpenMBeanParameterInfoSupport[0]);

        // Operations.
        operations[0] = new OpenMBeanOperationInfoSupport(
            Heritrix.START_OPER, "Start Heritrix instance", null,
                SimpleType.VOID, MBeanOperationInfo.ACTION);
        
        operations[1] = new OpenMBeanOperationInfoSupport(
            Heritrix.STOP_OPER, "Stop Heritrix instance", null,
                SimpleType.VOID, MBeanOperationInfo.ACTION);
        
        OpenMBeanParameterInfo[] args = new OpenMBeanParameterInfoSupport[1];
        args[0] = new OpenMBeanParameterInfoSupport("threadName",
            "Name of thread to send interrupt", SimpleType.STRING);
        operations[2] = new OpenMBeanOperationInfoSupport(
            Heritrix.INTERRUPT_OPER, "Send thread an interrupt " +
                "(Used debugging)", args, SimpleType.STRING,
                MBeanOperationInfo.ACTION_INFO);
        
        operations[3] = new OpenMBeanOperationInfoSupport(
            Heritrix.START_CRAWLING_OPER, "Set Heritrix instance " +
                "into crawling mode", null, SimpleType.VOID,
                MBeanOperationInfo.ACTION);
        
        operations[4] = new OpenMBeanOperationInfoSupport(
            Heritrix.STOP_CRAWLING_OPER, "Unset Heritrix instance " +
                " crawling mode", null, SimpleType.VOID,
                MBeanOperationInfo.ACTION);
        
        args = new OpenMBeanParameterInfoSupport[4];
        args[0] = new OpenMBeanParameterInfoSupport("pathOrURL",
            "Path/URL to order or jar of order+seed",
            SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("name",
            "Basename for new job", SimpleType.STRING);
        args[2] = new OpenMBeanParameterInfoSupport("description",
            "Description to save with new job", SimpleType.STRING);
        args[3] = new OpenMBeanParameterInfoSupport("seeds",
            "Initial seed(s)", SimpleType.STRING);
        operations[5] = new OpenMBeanOperationInfoSupport(
            Heritrix.ADD_CRAWL_JOB_OPER, "Add new crawl job", args,
                SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);
        
        args = new OpenMBeanParameterInfoSupport[4];
        args[0] = new OpenMBeanParameterInfoSupport("uidOrName",
            "Job UID or profile name", SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("name",
            "Basename for new job", SimpleType.STRING);
        args[2] = new OpenMBeanParameterInfoSupport("description",
            "Description to save with new job", SimpleType.STRING);
        args[3] = new OpenMBeanParameterInfoSupport("seeds",
            "Initial seed(s)", SimpleType.STRING);
        operations[6] = new OpenMBeanOperationInfoSupport(
            Heritrix.ADD_CRAWL_JOB_BASEDON_OPER,
            "Add a new crawl job based on passed Job UID or profile",
            args, SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);
        
        args = new OpenMBeanParameterInfoSupport[1];
        args[0] = new OpenMBeanParameterInfoSupport("UID",
            "Job UID", SimpleType.STRING);
        operations[7] = new OpenMBeanOperationInfoSupport(DELETE_CRAWL_JOB_OPER,
            "Delete/stop this crawl job", args, SimpleType.VOID,
            MBeanOperationInfo.ACTION);
        
        args = new OpenMBeanParameterInfoSupport[1];
        args[0] = new OpenMBeanParameterInfoSupport("index",
            "Zero-based index into array of alerts", SimpleType.INTEGER);
        operations[8] = new OpenMBeanOperationInfoSupport(
            Heritrix.ALERT_OPER, "Return alert at passed index", args,
                SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);
        
        try {
            this.jobCompositeType = new CompositeType("job",
                    "Job attributes", JOB_KEYS,
                    new String [] {"Job unique ID", "Job name", "Job status"},
                    new OpenType [] {SimpleType.STRING, SimpleType.STRING,
                        SimpleType.STRING});
            this.jobsTabularType = new TabularType("jobs", "List of jobs",
                    this.jobCompositeType, new String [] {"uid"});
        } catch (OpenDataException e) {
            // This should never happen.
            throw new RuntimeException(e);
        }
        operations[9] = new OpenMBeanOperationInfoSupport(
            Heritrix.PENDING_JOBS_OPER,
                "List of pending jobs (or null if none)", null,
                this.jobsTabularType, MBeanOperationInfo.INFO);
        operations[10] = new OpenMBeanOperationInfoSupport(
                Heritrix.COMPLETED_JOBS_OPER,
                    "List of completed jobs (or null if none)", null,
                    this.jobsTabularType, MBeanOperationInfo.INFO);
        
        args = new OpenMBeanParameterInfoSupport[2];
        args[0] = new OpenMBeanParameterInfoSupport("uid",
            "Job unique ID", SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("name",
                "Report name (e.g. crawl-report, etc.)",
                SimpleType.STRING);
        operations[11] = new OpenMBeanOperationInfoSupport(
            Heritrix.CRAWLEND_REPORT_OPER, "Return crawl-end report", args,
                SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);
        
        operations[12] = new OpenMBeanOperationInfoSupport(
            Heritrix.SHUTDOWN_OPER, "Shutdown container", null,
                SimpleType.VOID, MBeanOperationInfo.ACTION);
        
        args = new OpenMBeanParameterInfoSupport[2];
        args[0] = new OpenMBeanParameterInfoSupport("level",
            "Log level: e.g. SEVERE, WARNING, etc.", SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("message",
            "Log message", SimpleType.STRING);
        operations[13] = new OpenMBeanOperationInfoSupport(Heritrix.LOG_OPER,
            "Add a log message", args, SimpleType.VOID,
            MBeanOperationInfo.ACTION);
        
        operations[14] = new OpenMBeanOperationInfoSupport(
            Heritrix.DESTROY_OPER, "Destroy Heritrix instance", null,
                SimpleType.VOID, MBeanOperationInfo.ACTION);
        
        operations[15] = new OpenMBeanOperationInfoSupport(
            Heritrix.TERMINATE_CRAWL_JOB_OPER,
            "Returns false if no current job", null, SimpleType.BOOLEAN,
            MBeanOperationInfo.ACTION);
        
        operations[16] = new OpenMBeanOperationInfoSupport(
            Heritrix.REBIND_JNDI_OPER,
            "Rebinds this Heritrix with JNDI.", null,
            SimpleType.VOID, MBeanOperationInfo.ACTION);

        // Build the info object.
        return new OpenMBeanInfoSupport(this.getClass().getName(),
            "Heritrix Main OpenMBean", attributes, constructors, operations,
            notifications);
    }
    
    public Object getAttribute(String attribute_name)
    throws AttributeNotFoundException {
        if (attribute_name == null) {
            throw new RuntimeOperationsException(
                 new IllegalArgumentException("Attribute name cannot be null"),
                 "Cannot call getAttribute with null attribute name");
        }
        if (!Heritrix.ATTRIBUTE_LIST.contains(attribute_name)) {
            throw new AttributeNotFoundException("Attribute " +
                 attribute_name + " is unimplemented.");
        }
        // The pattern in the below is to match an attribute and when found
        // do a return out of if clause.  Doing it this way, I can fall
        // on to the AttributeNotFoundException for case where we've an
        // attribute but no handler.
        if (attribute_name.equals(STATUS_ATTR)) {
            return getStatus();
        }
        if (attribute_name.equals(VERSION_ATTR)) {
            return getVersion();
        }

        if (attribute_name.equals(ISRUNNING_ATTR)) {
            return new Boolean(this.getJobHandler().isRunning());
        }
        if (attribute_name.equals(ISCRAWLING_ATTR)) {
            return new Boolean(this.getJobHandler().isCrawling());
        }
        if (attribute_name.equals(ALERTCOUNT_ATTR)) {
            return new Integer(getAlertsCount());
        }
        if (attribute_name.equals(NEWALERTCOUNT_ATTR)) {
            return new Integer(getNewAlertsCount());
        }
        if (attribute_name.equals(CURRENTJOB_ATTR)) {
            if (this.getJobHandler().isCrawling()) {
                return this.getJobHandler().getCurrentJob().getJmxJobName();
            }
            return null;
        }
        throw new AttributeNotFoundException("Attribute " +
            attribute_name + " not found.");
    }

    public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException {
        throw new AttributeNotFoundException("No attribute can be set in " +
            "this MBean");
    }

    public AttributeList getAttributes(String [] attributeNames) {
        if (attributeNames == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("attributeNames[] cannot be " +
                "null"), "Cannot call getAttributes with null attribute " +
                "names");
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

    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList(); // always empty
    }

    public Object invoke(final String operationName, final Object[] params,
        final String[] signature)
    throws ReflectionException {
        if (operationName == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Operation name cannot be null"),
                "Cannot call invoke with null operation name");
        }
        // INFO logging of JMX invokes: [#HER-907]
        if (logger.isLoggable(Level.INFO)) {
            // String paramsString = "";
            StringBuilder buf = new StringBuilder();
            for (Object o : params) {
                buf.append("\"" + o + "\", ");
            }
            logger.info("JMX invoke: " + operationName + "(" + buf + ")");
        } 
        // The pattern in the below is to match an operation and when found
        // do a return out of if clause.  Doing it this way, I can fall
        // on to the MethodNotFoundException for case where we've an
        // attribute but no handler.
        if (operationName.equals(START_OPER)) {
            JmxUtils.checkParamsCount(START_OPER, params, 0);
            start();
            return null;
        }
        if (operationName.equals(STOP_OPER)) {
            JmxUtils.checkParamsCount(STOP_OPER, params, 0);
            stop();
            return null;
        }
        if (operationName.equals(DESTROY_OPER)) {
            JmxUtils.checkParamsCount(DESTROY_OPER, params, 0);
            destroy();
            return null;
        }
        if (operationName.equals(TERMINATE_CRAWL_JOB_OPER)) {
            JmxUtils.checkParamsCount(TERMINATE_CRAWL_JOB_OPER, params, 0);
            return new Boolean(this.jobHandler.terminateCurrentJob());
        }
        if (operationName.equals(REBIND_JNDI_OPER)) {
            JmxUtils.checkParamsCount(REBIND_JNDI_OPER, params, 0);
            try {
				registerContainerJndi();
			} catch (MalformedObjectNameException e) {
				throw new RuntimeOperationsException(new RuntimeException(e));
			} catch (UnknownHostException e) {
				throw new RuntimeOperationsException(new RuntimeException(e));
			} catch (NamingException e) {
				throw new RuntimeOperationsException(new RuntimeException(e));
			}
            return null;
        }
        if (operationName.equals(SHUTDOWN_OPER)) {
            JmxUtils.checkParamsCount(SHUTDOWN_OPER, params, 0);
            Heritrix.shutdown();
            return null;
        }
        if (operationName.equals(LOG_OPER)) {
            JmxUtils.checkParamsCount(LOG_OPER, params, 2);
            logger.log(Level.parse((String)params[0]), (String)params[1]);
            return null;
        }
        if (operationName.equals(INTERRUPT_OPER)) {
            JmxUtils.checkParamsCount(INTERRUPT_OPER, params, 1);
            return interrupt((String)params[0]);
        }       
        if (operationName.equals(START_CRAWLING_OPER)) {
            JmxUtils.checkParamsCount(START_CRAWLING_OPER, params, 0);
            startCrawling();
            return null;
        }
        if (operationName.equals(STOP_CRAWLING_OPER)) {
            JmxUtils.checkParamsCount(STOP_CRAWLING_OPER, params, 0);
            stopCrawling();
            return null;
        }
        if (operationName.equals(ADD_CRAWL_JOB_OPER)) {
            JmxUtils.checkParamsCount(ADD_CRAWL_JOB_OPER, params, 4);
            try {
                return addCrawlJob((String)params[0], (String)params[1],
                    checkForEmptyPlaceHolder((String)params[2]),
                    checkForEmptyPlaceHolder((String)params[3]));
            } catch (IOException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            } catch (FatalConfigurationException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        if (operationName.equals(DELETE_CRAWL_JOB_OPER)) {
            JmxUtils.checkParamsCount(DELETE_CRAWL_JOB_OPER, params, 1);
            this.jobHandler.deleteJob((String)params[0]);
            return null;
        }
        
        if (operationName.equals(ADD_CRAWL_JOB_BASEDON_OPER)) {
            JmxUtils.checkParamsCount(ADD_CRAWL_JOB_BASEDON_OPER, params, 4);
            return addCrawlJobBasedOn((String)params[0], (String)params[1],
                    checkForEmptyPlaceHolder((String)params[2]),
                    checkForEmptyPlaceHolder((String)params[3]));
        }       
        if (operationName.equals(ALERT_OPER)) {
            JmxUtils.checkParamsCount(ALERT_OPER, params, 1);
            SinkHandlerLogRecord slr = null;
            if (this.alertManager.getCount() > 0) {
                // This is creating a vector of all alerts just so I can then
                // use passed index into resultant vector -- needs to be
                // improved.
                slr = (SinkHandlerLogRecord)this.alertManager.getAll().
                    get(((Integer)params[0]).intValue());
            }
            return (slr != null)? slr.toString(): null;
        }
        
        if (operationName.equals(PENDING_JOBS_OPER)) {
                JmxUtils.checkParamsCount(PENDING_JOBS_OPER, params, 0);
            try {
                return makeJobsTabularData(getJobHandler().getPendingJobs());
            } catch (OpenDataException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        
        if (operationName.equals(COMPLETED_JOBS_OPER)) {
                JmxUtils.checkParamsCount(COMPLETED_JOBS_OPER, params, 0);
            try {
                return makeJobsTabularData(getJobHandler().getCompletedJobs());
            } catch (OpenDataException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        
        if (operationName.equals(CRAWLEND_REPORT_OPER)) {
            JmxUtils.checkParamsCount(CRAWLEND_REPORT_OPER, params, 2);
            try {
                return getCrawlendReport((String)params[0], (String) params[1]);
            } catch (IOException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        
        throw new ReflectionException(
            new NoSuchMethodException(operationName),
                "Cannot find the operation " + operationName);
    }
    
    /**
     * Return named crawl end report for job with passed uid.
     * Crawler makes reports when its finished its crawl.  Use this method
     * to get a String version of one of these files.
     * @param jobUid The unique ID for the job whose reports you want to see
     * (Must be a completed job).
     * @param reportName Name of report minus '.txt' (e.g. crawl-report).
     * @return String version of the on-disk report.
     * @throws IOException 
     */
    protected String getCrawlendReport(String jobUid, String reportName)
    throws IOException {
        CrawlJob job = getJobHandler().getJob(jobUid);
        if (job == null) {
            throw new IOException("No such job: " + jobUid);
        }
        File report = new File(job.getDirectory(), reportName + ".txt");
        if (!report.exists()) {
            throw new FileNotFoundException(report.getAbsolutePath());
        }
        return FileUtils.readFileAsString(report);
    }
    
    protected TabularData makeJobsTabularData(List jobs)
    throws OpenDataException {
        if (jobs == null || jobs.size() == 0) {
            return null;
        }
        TabularData td = new TabularDataSupport(this.jobsTabularType);
        for (Iterator i = jobs.iterator(); i.hasNext();) {
            CrawlJob job = (CrawlJob)i.next();
            CompositeData cd = new CompositeDataSupport(this.jobCompositeType,
                JOB_KEYS,
                new String [] {job.getUID(), job.getJobName(), job.getStatus()});
            td.put(cd);
        }
        return td;
    }
    
    /**
     * If passed str has placeholder for the empty string, return the empty
     * string else return orginal.
     * Dumb jmx clients can't pass empty string so they'll pass a representation
     * of empty string such as ' ' or '-'.  Convert such strings to empty
     * string.
     * @param str String to check.
     * @return Original <code>str</code> or empty string if <code>str</code>
     * contains a placeholder for the empty-string (e.g. '-', or ' ').
     */
    protected String checkForEmptyPlaceHolder(String str) {
        return TextUtils.matches("-| +", str)? "": str;
    }

    public MBeanInfo getMBeanInfo() {
        return this.openMBeanInfo;
    }
    
    /**
     * @return Name this instance registered in JMX (Only available after JMX
     * registration).
     */
    public ObjectName getMBeanName() {
        return this.mbeanName;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name)
    throws Exception {
        this.mbeanServer = server;
        @SuppressWarnings("unchecked")
        Hashtable<String,String> ht = name.getKeyPropertyList();
        if (!ht.containsKey(JmxUtils.NAME)) {
            throw new IllegalArgumentException("Name property required" +
                name.getCanonicalName());
        }
        if (!ht.containsKey(JmxUtils.TYPE)) {
            ht.put(JmxUtils.TYPE, JmxUtils.SERVICE);
            name = new ObjectName(name.getDomain(), ht);
        }
        this.mbeanName = addGuiPort(addVitals(name));
        Heritrix.instances.put(this.mbeanName.
            getCanonicalKeyPropertyListString(), this);
        return this.mbeanName;
    }
    
    /**
     * Add vital stats to passed in ObjectName.
     * @param name ObjectName to add to.
     * @return name with host, guiport, and jmxport added.
     * @throws UnknownHostException
     * @throws MalformedObjectNameException
     * @throws NullPointerException
     */
    protected static ObjectName addVitals(ObjectName name)
    throws UnknownHostException, MalformedObjectNameException,
    NullPointerException {
        @SuppressWarnings("unchecked")
        Hashtable<String,String> ht = name.getKeyPropertyList();
        if (!ht.containsKey(JmxUtils.HOST)) {
            ht.put(JmxUtils.HOST, InetAddress.getLocalHost().getCanonicalHostName());
            name = new ObjectName(name.getDomain(), ht);
        }
        if (!ht.containsKey(JmxUtils.JMX_PORT)) {
            // Add jdk jmx-port. This will be present if we've attached
            // ourselves to the jdk jmx agent.  Otherwise, we've been
            // deployed in a j2ee container with its own jmx agent.  In
            // this case we won't know how to get jmx port.
            String p = System.getProperty("com.sun.management.jmxremote.port");
            if (p != null && p.length() > 0) {
                ht.put(JmxUtils.JMX_PORT, p);
                name = new ObjectName(name.getDomain(), ht);
            }
        }
        return name;
    }
    
    protected static ObjectName addGuiPort(ObjectName name)
    throws MalformedObjectNameException, NullPointerException {
        @SuppressWarnings("unchecked")
        Hashtable<String,String> ht = name.getKeyPropertyList();
        if (!ht.containsKey(JmxUtils.GUI_PORT)) {
            // Add gui port if this instance was started with a gui.
            if (Heritrix.gui) {
                ht.put(JmxUtils.GUI_PORT, Integer.toString(Heritrix.guiPort));
                name = new ObjectName(name.getDomain(), ht);
            }
        }
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(
                JmxUtils.getLogRegistrationMsg(this.mbeanName.getCanonicalName(),
                this.mbeanServer, registrationDone.booleanValue()));
        }
        try {
            registerJndi(this.mbeanName);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed jndi registration", e);
        }
    }

    public void preDeregister() throws Exception {
        deregisterJndi(this.mbeanName);
    }

    public void postDeregister() {
        Heritrix.instances.
            remove(this.mbeanName.getCanonicalKeyPropertyListString());
        if (logger.isLoggable(Level.INFO)) {
            logger.info(JmxUtils.getLogUnregistrationMsg(
                    this.mbeanName.getCanonicalName(), this.mbeanServer));
        }
    }
    
    protected static void registerContainerJndi()
    throws MalformedObjectNameException, NullPointerException,
    		UnknownHostException, NamingException {
    	registerJndi(getJndiContainerName());
    }

    protected static void registerJndi(final ObjectName name)
    throws NullPointerException, NamingException {
    	Context c = getJndiContext();
    	if (c == null) {
    		return;
    	}
        CompoundName key = JndiUtils.bindObjectName(c, name);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Bound '"  + key + "' to '" + JndiUtils.
               getCompoundName(c.getNameInNamespace()).toString()
               + "' jndi context");
        }
    }
    
    protected static void deregisterJndi(final ObjectName name)
    throws NullPointerException, NamingException {
    	Context c = getJndiContext();
    	if (c == null) {
    		return;
    	}
        CompoundName key = JndiUtils.unbindObjectName(c, name);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Unbound '" + key + "' from '" +
                JndiUtils.getCompoundName(c.getNameInNamespace()).toString() +
                	"' jndi context");
        }
    }
    
    /**
     * @return Jndi context for the crawler or null if none found.
     * @throws NamingException 
     */
    protected static Context getJndiContext() throws NamingException {
    	Context c = null;
    	try {
    		c = JndiUtils.getSubContext(CRAWLER_PACKAGE);
    	} catch (NoInitialContextException e) {
    		logger.fine("No JNDI Context: " + e.toString());
    	}
    	return c;
    }
    
    /**
     * @return Jndi container name -- the name to use for the 'container' that
     * can host zero or more heritrix instances (Return a JMX ObjectName.  We
     * use ObjectName because then we're sync'd with JMX naming and ObjectName
     * has nice parsing).
     * @throws NullPointerException 
     * @throws MalformedObjectNameException 
     * @throws UnknownHostException 
     */
    protected static ObjectName getJndiContainerName()
    throws MalformedObjectNameException, NullPointerException,
    UnknownHostException {
        ObjectName objName = new ObjectName(CRAWLER_PACKAGE, "type",
            "container");
        return addVitals(objName);
    }
    
    /**
     * @return Return all registered instances of Heritrix (Rare are there 
     * more than one).
     */
    public static Map getInstances() {
        return Heritrix.instances;
    }
    
    /**
     * @return True if only one instance of Heritrix.
     */
    public static boolean isSingleInstance() {
        return Heritrix.instances != null && Heritrix.instances.size() == 1;
    }
    
    /**
     * @return Returns single instance or null if no instance or multiple.
     */
    public static Heritrix getSingleInstance() {
        return !isSingleInstance()?
            null:
            (Heritrix)Heritrix.instances.
                get(Heritrix.instances.keySet().iterator().next());
    }
}
