/* WriterPoolProcessor
 *
 * $Id: WriterPoolProcessor.java 6631 2009-11-09 21:10:20Z gojomo $
 *
 * Created on July 19th, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.deciderules.recrawl.IdenticalDigestDecideRule;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.crawler.settings.Type;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.io.ObjectPlusFilesInputStream;
import org.archive.io.WriterPool;
import org.archive.io.WriterPoolMember;

/**
 * Abstract implementation of a file pool processor.
 * Subclass to implement for a particular {@link WriterPoolMember} instance.
 * @author Parker Thompson
 * @author stack
 */
public abstract class WriterPoolProcessor extends Processor
implements CoreAttributeConstants, CrawlStatusListener, FetchStatusCodes {
    private static final long serialVersionUID = 1L;
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Key to use asking settings for file compression value.
     */
    public static final String ATTR_COMPRESS = "compress";

    /**
     * Default as to whether we do compression of files.
     */
    public static final boolean DEFAULT_COMPRESS = true;

    /**
     * Key to use asking settings for file prefix value.
     */
    public static final String ATTR_PREFIX = "prefix";    

    /**
     * Key to use asking settings for arc path value.
     */
    public static final String ATTR_PATH ="path";

    /**
     * Key to use asking settings for file suffix value.
     */
    public static final String ATTR_SUFFIX = "suffix";

    /**
     * Key to use asking settings for file max size value.
     */
    public static final String ATTR_MAX_SIZE_BYTES = "max-size-bytes";
    
    /**
     * Key to get maximum pool size.
     *
     * This key is for maximum files active in the pool.
     */
    public static final String ATTR_POOL_MAX_ACTIVE = "pool-max-active";

    /**
     * Key to get maximum wait on pool object before we give up and
     * throw IOException.
     */
    public static final String ATTR_POOL_MAX_WAIT = "pool-max-wait";

    /**
     * Key for the maximum bytes to write attribute.
     */
    public static final String ATTR_MAX_BYTES_WRITTEN =
    	"total-bytes-to-write";
    
    /**
     * Key for whether to skip writing records of content-digest repeats 
     */
    public static final String ATTR_SKIP_IDENTICAL_DIGESTS =
        "skip-identical-digests";
    
    /**
     * CrawlURI annotation indicating no record was written
     */
    protected static final String ANNOTATION_UNWRITTEN = "unwritten";
    
    /**
     * Default maximum file size.
     */
    public abstract long getDefaultMaxFileSize();
    
    /**
     * Default path list.
     * 
     * TODO: Confirm this one gets picked up.
     */
    private static final String [] DEFAULT_PATH = {"crawl-store"};

    /**
     * Reference to pool.
     */
    transient private WriterPool pool = null;
    
    /**
     * Total number of bytes written to disc.
     */
    private long totalBytesWritten = 0;
    
    /**
     * Calculate metadata once only.
     */
    transient private List<String> cachedMetadata = null;


    /**
     * @param name Name of this processor.
     */
    public WriterPoolProcessor(String name) {
    	this(name, "Pool of files processor");
    }
    	
    /**
     * @param name Name of this processor.
     * @param description Description for this processor.
     */
    public WriterPoolProcessor(final String name,
        		final String description) {
        super(name, description);
        Type e = addElementToDefinition(
            new SimpleType(ATTR_COMPRESS, "Compress files when " +
            	"writing to disk.", new Boolean(DEFAULT_COMPRESS)));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new SimpleType(ATTR_PREFIX, 
                "File prefix. " +
                "The text supplied here will be used as a prefix naming " +
                "writer files.  For example if the prefix is 'IAH', " +
                "then file names will look like " +
                "IAH-20040808101010-0001-HOSTNAME.arc.gz " +
                "...if writing ARCs (The prefix will be " +
                "separated from the date by a hyphen).",
                WriterPoolMember.DEFAULT_PREFIX));
        e = addElementToDefinition(
            new SimpleType(ATTR_SUFFIX, "Suffix to tag onto " +
                "files. '${HOSTNAME_ADMINPORT}' in the suffix " + 
                "will be replaced with the local hostname and " +
                "web UI port. '${HOSTNAME}' in the suffix will be " + 
                "replaced with the local hostname. If empty, no "+  
                "suffix will be added.",
                WriterPoolMember.DEFAULT_SUFFIX));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new SimpleType(ATTR_MAX_SIZE_BYTES, "Max size of each file",
                new Long(getDefaultMaxFileSize())));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new StringList(ATTR_PATH, "Where to files. " +
                "Supply absolute or relative path.  If relative, files " +
                "will be written relative to " +
                "the " + CrawlOrder.ATTR_DISK_PATH + "setting." +
                " If more than one path specified, we'll round-robin" +
                " dropping files to each.  This setting is safe" +
                " to change midcrawl (You can remove and add new dirs" +
                " as the crawler progresses).", getDefaultPath()));
        e.setOverrideable(false);
        e = addElementToDefinition(new SimpleType(ATTR_POOL_MAX_ACTIVE,
            "Maximum active files in pool. " +
            "This setting cannot be varied over the life of a crawl.",
            new Integer(WriterPool.DEFAULT_MAX_ACTIVE)));
        e.setOverrideable(false);
        e = addElementToDefinition(new SimpleType(ATTR_POOL_MAX_WAIT,
            "Maximum time to wait on pool element" +
            " (milliseconds). This setting cannot be varied over the life" +
            " of a crawl.",
            new Integer(WriterPool.DEFAULT_MAXIMUM_WAIT)));
        e.setOverrideable(false);
        e = addElementToDefinition(new SimpleType(ATTR_MAX_BYTES_WRITTEN,
            "Total file bytes to write to disk." +
            " Once the size of all files on disk has exceeded this " +
            "limit, this processor will stop the crawler. " +
            "A value of zero means no upper limit.", new Long(0)));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SKIP_IDENTICAL_DIGESTS,
                "Whether to skip the writing of a record when URI " +
                "history information is available and indicates the " +
                "prior fetch had an identical content digest. " +
                "Default is false.", new Boolean(false)));
        e.setOverrideable(true);
        e.setExpertSetting(true);
    }
    
    protected String [] getDefaultPath() {
    	return DEFAULT_PATH;
	}

    public synchronized void initialTasks() {
        // Add this class to crawl state listeners and setup pool.
        getSettingsHandler().getOrder().getController().
            addCrawlStatusListener(this);
        setupPool(new AtomicInteger());
        // Run checkpoint recovery code.
        if (getSettingsHandler().getOrder().getController().
        		isCheckpointRecover()) {
        	checkpointRecover();
        }
    }
    
    protected AtomicInteger getSerialNo() {
        return ((WriterPool)getPool()).getSerialNo();
    }

    /**
     * Set up pool of files.
     */
    protected abstract void setupPool(final AtomicInteger serialNo);

    /**
     * Writes a CrawlURI and its associated data to store file.
     *
     * Currently this method understands the following uri types: dns, http, 
     * and https.
     *
     * @param curi CrawlURI to process.
     */
    protected abstract void innerProcess(CrawlURI curi);
    
    protected void checkBytesWritten() {
        long max = getMaxToWrite();
        if (max <= 0) {
            return;
        }
        if (max <= this.totalBytesWritten) {
            getController().requestCrawlStop("Finished - Maximum bytes (" +
                Long.toString(max) + ") written");
        }
    }
    
    /**
     * Whether the given CrawlURI should be written to archive files. 
     * Annotates CrawlURI with a reason for any negative answer. 
     * 
     * @param curi CrawlURI
     * @return true if URI should be written; false otherwise
     */
    protected boolean shouldWrite(CrawlURI curi) {
        // check for duplicate content write suppression
        if(((Boolean)getUncheckedAttribute(curi, ATTR_SKIP_IDENTICAL_DIGESTS)) 
            && IdenticalDigestDecideRule.hasIdenticalDigest(curi)) {
            curi.addAnnotation(ANNOTATION_UNWRITTEN + ":identicalDigest");
            return false; 
        }
        String scheme = curi.getUURI().getScheme().toLowerCase();
        // TODO: possibly move this sort of isSuccess() test into CrawlURI
        boolean retVal; 
        if (scheme.equals("dns")) {
            retVal = curi.getFetchStatus() == S_DNS_SUCCESS;
        } else if (scheme.equals("http") || scheme.equals("https")) {
            retVal = curi.getFetchStatus() > 0 && curi.isHttpTransaction();
        } else if (scheme.equals("ftp")) {
            retVal = curi.getFetchStatus() > 0;
        } else {
            // unsupported scheme
            curi.addAnnotation(ANNOTATION_UNWRITTEN + ":scheme");
            return false; 
        }
        if (retVal == false) {
            // status not deserving writing
            curi.addAnnotation(ANNOTATION_UNWRITTEN + ":status");
            return false; 
        }
        return true; 
    }
    
    /**
     * Return IP address of given URI suitable for recording (as in a
     * classic ARC 5-field header line).
     * 
     * @param curi CrawlURI
     * @return String of IP address
     */
    protected String getHostAddress(CrawlURI curi) {
        // special handling for DNS URIs: want address of DNS server
        if(curi.getUURI().getScheme().toLowerCase().equals("dns")) {
            return curi.getString(A_DNS_SERVER_IP_LABEL);
        }
        // otherwise, host referenced in URI
        CrawlHost h = getController().getServerCache().getHostFor(curi);
        if (h == null) {
            throw new NullPointerException("Crawlhost is null for " +
                curi + " " + curi.getVia());
        }
        InetAddress a = h.getIP();
        if (a == null) {
            throw new NullPointerException("Address is null for " +
                curi + " " + curi.getVia() + ". Address " +
                ((h.getIpFetched() == CrawlHost.IP_NEVER_LOOKED_UP)?
                     "was never looked up.":
                     (System.currentTimeMillis() - h.getIpFetched()) +
                         " ms ago."));
        }
        return h.getIP().getHostAddress();
    }
    
    /**
     * Version of getAttributes that catches and logs exceptions
     * and returns null if failure to fetch the attribute.
     * @param name Attribute name.
     * @return Attribute or null.
     */
    public Object getAttributeUnchecked(String name) {
        Object result = null;
        try {
            result = super.getAttribute(name);
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        }
        return result;
    }

   /**
    * Max size we want files to be (bytes).
    *
    * Default is ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE.  Note that ARC
    * files will usually be bigger than maxSize; they'll be maxSize + length
    * to next boundary.
    * @return ARC maximum size.
    */
    public long getMaxSize() {
        Object obj = getAttributeUnchecked(ATTR_MAX_SIZE_BYTES);
        return (obj == null)? getDefaultMaxFileSize(): ((Long)obj).longValue();
    }

    public String getPrefix() {
        Object obj = getAttributeUnchecked(ATTR_PREFIX);
        return (obj == null)? WriterPoolMember.DEFAULT_PREFIX: (String)obj;
    }

    @SuppressWarnings("unchecked")
    public List<File> getOutputDirs() {
        Object obj = getAttributeUnchecked(ATTR_PATH);
        List list = (obj == null)? Arrays.asList(DEFAULT_PATH): (StringList)obj;
        ArrayList<File> results = new ArrayList<File>();
        for (Iterator i = list.iterator(); i.hasNext();) {
            String path = (String)i.next();
            File f = new File(path);
            if (!f.isAbsolute()) {
                f = new File(getController().getDisk(), path);
            }
            if (!f.exists()) {
                try {
                    f.mkdirs();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
            results.add(f);
        }
        return results;
    }
    
    public boolean isCompressed() {
        Object obj = getAttributeUnchecked(ATTR_COMPRESS);
        return (obj == null)? DEFAULT_COMPRESS:
            ((Boolean)obj).booleanValue();
    }

    /**
     * @return Returns the poolMaximumActive.
     */
    public int getPoolMaximumActive() {
        Object obj = getAttributeUnchecked(ATTR_POOL_MAX_ACTIVE);
        return (obj == null)? WriterPool.DEFAULT_MAX_ACTIVE:
            ((Integer)obj).intValue();
    }

    /**
     * @return Returns the poolMaximumWait.
     */
    public int getPoolMaximumWait() {
        Object obj = getAttributeUnchecked(ATTR_POOL_MAX_WAIT);
        return (obj == null)? WriterPool.DEFAULT_MAXIMUM_WAIT:
            ((Integer)obj).intValue();
    }

    private String getHostname() {
        String hostname = "localhost.localdomain";
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException ue) {
            logger.severe("Failed getHostAddress for this host: " + ue);
        }
        
        return hostname;
    }
    
    private int getPort() {
        if (Heritrix.getHttpServer() != null) {
            return Heritrix.getHttpServer().getPort();
        } else {
            return 0;
        }
    }

    public String getSuffix() {
        Object obj = getAttributeUnchecked(ATTR_SUFFIX);
        String sfx = (obj == null)?
            WriterPoolMember.DEFAULT_SUFFIX: (String)obj;
        sfx = sfx.trim(); 
        if (sfx.contains(WriterPoolMember.HOSTNAME_ADMINPORT_VARIABLE) 
                || sfx.contains(WriterPoolMember.HOSTNAME_VARIABLE)) {
            String hostname = getHostname();
            sfx = sfx.replace(WriterPoolMember.HOSTNAME_ADMINPORT_VARIABLE, hostname + "-" + getPort());
            sfx = sfx.replace(WriterPoolMember.HOSTNAME_VARIABLE, hostname);
            }
        return sfx;
    }
    
    public long getMaxToWrite() {
        Object obj = getAttributeUnchecked(ATTR_MAX_BYTES_WRITTEN);
        return (obj == null)? 0: ((Long)obj).longValue();
    }

	public void crawlEnding(String sExitMessage) {
	}

	public void crawlEnded(String sExitMessage) {
        // sExitMessage is unused.
	    this.pool.close();
	}

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
    }
    
    protected String getCheckpointStateFile() {
    	return this.getClass().getName() + ".state";
    }
    
    public void crawlCheckpoint(File checkpointDir) throws IOException {
        int serial = getSerialNo().get();
        if (this.pool.getNumActive() > 0) {
            // If we have open active Archive files, up the serial number
            // so after checkpoint, we start at one past current number and
            // so the number we serialize, is one past current serialNo.
            // All this serial number manipulation should be fine in here since
            // we're paused checkpointing (Revisit if this assumption changes).
            serial = getSerialNo().incrementAndGet();
        }
        saveCheckpointSerialNumber(checkpointDir, serial);
        // Close all ARCs on checkpoint.
        try {
            this.pool.close();
        } finally {
            // Reopen on checkpoint.
            setupPool(new AtomicInteger(serial));
        }
    }
    
	public void crawlPausing(String statusMessage) {
        // sExitMessage is unused.
	}

	public void crawlPaused(String statusMessage) {
        // sExitMessage is unused.
	}

	public void crawlResuming(String statusMessage) {
        // sExitMessage is unused.
	}
	
    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        ObjectPlusFilesInputStream coistream =
            (ObjectPlusFilesInputStream)stream;
        coistream.registerFinishTask( new Runnable() {
            public void run() {
            	setupPool(new AtomicInteger());
            }
        });
    }

	protected WriterPool getPool() {
		return pool;
	}

	protected void setPool(WriterPool pool) {
		this.pool = pool;
	}

	protected long getTotalBytesWritten() {
		return totalBytesWritten;
	}

	protected void setTotalBytesWritten(long totalBytesWritten) {
        this.totalBytesWritten = totalBytesWritten;
    }
	
    /**
     * Called out of {@link #initialTasks()} when recovering a checkpoint.
     * Restore state.
     */
    protected void checkpointRecover() {
        int serialNo = loadCheckpointSerialNumber();
        if (serialNo != -1) {
            getSerialNo().set(serialNo);
        }
    }

    /**
     * @return Serial number from checkpoint state file or if unreadable, -1
     * (Client should check for -1).
     */
    protected int loadCheckpointSerialNumber() {
        int result = -1;
        
        // If in recover mode, read in the Writer serial number saved
        // off when we checkpointed.
        File stateFile = new File(getSettingsHandler().getOrder()
                .getController().getCheckpointRecover().getDirectory(),
                getCheckpointStateFile());
        if (!stateFile.exists()) {
            logger.info(stateFile.getAbsolutePath()
                    + " doesn't exist so cannot restore Writer serial number.");
        } else {
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new FileInputStream(stateFile));
                result = dis.readShort();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (dis != null) {
                        dis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    
    protected void saveCheckpointSerialNumber(final File checkpointDir,
            final int serialNo)
    throws IOException {
        // Write out the current state of the ARCWriter serial number.
        File f = new File(checkpointDir, getCheckpointStateFile());
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
        try {
            dos.writeShort(serialNo);
        } finally {
            dos.close();
        }
    }
    
    /**
     * Return list of metadatas to add to first arc file metadata record.
     * 
     * Default is to stylesheet the order file.  To specify stylesheet,
     * override {@link #getFirstrecordStylesheet()}.
     *
     * Get xml files from settingshandler.  Currently order file is the
     * only xml file.  We're NOT adding seeds to meta data.
     *
     * @return List of strings and/or files to add to arc file as metadata or
     * null.
     */
    public synchronized List<String> getMetadata() {
        if (this.cachedMetadata != null) {
            return this.cachedMetadata;
        }
        return cacheMetadata();
    }
    
    protected synchronized List<String> cacheMetadata() {
        
        // If no stylesheet, return empty metadata.
        if (getFirstrecordStylesheet() == null ||
                getFirstrecordStylesheet().length() == 0) {
            this.cachedMetadata = new ArrayList<String>(1);
            this.cachedMetadata.add("");
            return this.cachedMetadata;
        }
        
        List<String> result = null;
        if (!XMLSettingsHandler.class.isInstance(getSettingsHandler())) {
            logger.warning("Expected xml settings handler (No warcinfo).");
            // Early return
            return result;
        }
        
        XMLSettingsHandler xsh = (XMLSettingsHandler)getSettingsHandler();
        File orderFile = xsh.getOrderFile();
        if (!orderFile.exists() || !orderFile.canRead()) {
                logger.severe("File " + orderFile.getAbsolutePath() +
                    " is does not exist or is not readable.");
        } else {
            result = new ArrayList<String>(1);
            result.add(getFirstrecordBody(orderFile));
        }
        this.cachedMetadata = result;
        return this.cachedMetadata;
    }
    
    /**
     * @preturn Full path to stylesheet (Its read off the CLASSPATH
     * as resource).
     */
    protected String getFirstrecordStylesheet() {
        return null;
    }

    /**
     * Write the arc metadata body content.
     *
     * Its based on the order xml file but into this base we'll add other info
     * such as machine ip.
     *
     * @param orderFile Order file.

     *
     * @return String that holds the arc metaheader body.
     */
    protected String getFirstrecordBody(File orderFile) {
        String result = null;
        TransformerFactory factory = TransformerFactory.newInstance();
        Templates templates = null;
        Transformer xformer = null;
        try {
            templates = factory.newTemplates(new StreamSource(
                this.getClass().getResourceAsStream(getFirstrecordStylesheet())));
            xformer = templates.newTransformer();
            // Below parameter names must match what is in the stylesheet.
            xformer.setParameter("software", "Heritrix " +
                Heritrix.getVersion() + " http://crawler.archive.org");
            xformer.setParameter("ip",
                InetAddress.getLocalHost().getHostAddress());
            xformer.setParameter("hostname",
                InetAddress.getLocalHost().getCanonicalHostName());
            StreamSource source = new StreamSource(
                new FileInputStream(orderFile));
            StringWriter writer = new StringWriter();
            StreamResult target = new StreamResult(writer);
            xformer.transform(source, target);
            result= writer.toString();
        } catch (TransformerConfigurationException e) {
            logger.severe("Failed transform " + e);
        } catch (FileNotFoundException e) {
            logger.severe("Failed transform, file not found " + e);
        } catch (UnknownHostException e) {
            logger.severe("Failed transform, unknown host " + e);
        } catch(TransformerException e) {
            SourceLocator locator = e.getLocator();
            int col = locator.getColumnNumber();
            int line = locator.getLineNumber();
            String publicId = locator.getPublicId();
            String systemId = locator.getSystemId();
            logger.severe("Transform error " + e + ", col " + col + ", line " +
                line + ", publicId " + publicId + ", systemId " + systemId);
        }

        return result;
    }
}