/* FetchFTP.java
 *
 * $Id: FetchFTP.java 6541 2009-10-03 20:39:38Z nlevitt $
 *
 * Created on Jun 5, 2003
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
package org.archive.crawler.fetcher;


import static org.archive.crawler.extractor.Link.NAVLINK_HOP;
import static org.archive.crawler.extractor.Link.NAVLINK_MISC;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPCommand;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.io.RecordingInputStream;
import org.archive.io.ReplayCharSequence;
import org.archive.net.ClientFTP;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;


/**
 * Fetches documents and directory listings using FTP.  This class will also
 * try to extract FTP "links" from directory listings.  For this class to
 * archive a directory listing, the remote FTP server must support the NLIST
 * command.  Most modern FTP servers should.
 * 
 * @author pjack
 *
 */
public class FetchFTP extends Processor implements CoreAttributeConstants, FetchStatusCodes {


    /** Serialization ID; robust against trivial API changes. */
    private static final long serialVersionUID =
     ArchiveUtils.classnameBasedUID(FetchFTP.class,1);

    /** Logger for this class. */
    private static Logger logger = Logger.getLogger(FetchFTP.class.getName());

    /** Pattern for matching directory entries. */
    private static Pattern DIR = 
     Pattern.compile("(.+)$", Pattern.MULTILINE);

    
    /** The name for the <code>username</code> attribute. */
    final public static String ATTR_USERNAME = "username";
   
    /** The description for the <code>username</code> attribute. */
    final private static String DESC_USERNAME = "The username to send to " +
     "FTP servers.  By convention, the default value of \"anonymous\" is " +
     "used for publicly available FTP sites.";
    
    /** The default value for the <code>username</code> attribute. */
    final private static String DEFAULT_USERNAME = "anonymous";


    /** The name for the <code>password</code> attribute. */
    final public static String ATTR_PASSWORD = "password";
   
    /** The description for the <code>password</code> attribute. */
    final private static String DESC_PASSWORD = "The password to send to " +
    "FTP servers. By convention, anonymous users send their email address " +
    "in this field. If unset the crawl operator 'From' value is used.";
    
    /** The default value for the <code>password</code> attribute. */
    final private static String DEFAULT_PASSWORD = "";

    
    /** The name for the <code>extract-from-dirs</code> attribute. */
    final private static String ATTR_EXTRACT = "extract-from-dirs";
    
    /** The description for the <code>extract-from-dirs</code> attribute. */
    final private static String DESC_EXTRACT = "Set to true to extract "
     + "further URIs from FTP directories.  Default is true.";
    
    /** The default value for the <code>extract-from-dirs</code> attribute. */
    final private static boolean DEFAULT_EXTRACT = true;

    
    /** The name for the <code>extract-parent</code> attribute. */
    final private static String ATTR_EXTRACT_PARENT = "extract_parent";
    
    /** The description for the <code>extract-parent</code> attribute. */
    final private static String DESC_EXTRACT_PARENT = "Set to true to extract "
     + "the parent URI from all FTP URIs.  Default is true.";
    
    /** The default value for the <code>extract-parent</code> attribute. */
    final private static boolean DEFAULT_EXTRACT_PARENT = true;
    
    
    /** The name for the <code>max-length-bytes</code> attribute. */
    final public static String ATTR_MAX_LENGTH = "max-length-bytes";
    
    /** The description for the <code>max-length-bytes</code> attribute. */
    final private static String DESC_MAX_LENGTH = 
        "Maximum length in bytes to fetch.\n" +
        "Fetch is truncated at this length. A value of 0 means no limit.";
    
    /** The default value for the <code>max-length-bytes</code> attribute. */
    final private static long DEFAULT_MAX_LENGTH = 0;

    
    /** The name for the <code>fetch-bandwidth</code> attribute. */
    final public static String ATTR_BANDWIDTH = "fetch-bandwidth";
    
    /** The description for the <code>fetch-bandwidth</code> attribute. */
    final private static String DESC_BANDWIDTH = "";
    
    /** The default value for the <code>fetch-bandwidth</code> attribute. */
    final private static int DEFAULT_BANDWIDTH = 0;
    
    
    /** The name for the <code>timeout-seconds</code> attribute. */
    final public static String ATTR_TIMEOUT = "timeout-seconds";
    
    /** The description for the <code>timeout-seconds</code> attribute. */
    final private static String DESC_TIMEOUT = "If the fetch is not "
     + "completed in this number of seconds, give up (and retry later).";
    
    /** The default value for the <code>timeout-seconds</code> attribute. */
    final private static int DEFAULT_TIMEOUT = 1200;


    /**
     * Constructs a new <code>FetchFTP</code>.
     * 
     * @param name  the name of this processor
     */
    public FetchFTP(String name) {
        super(name, "FTP Fetcher.");
        add(ATTR_USERNAME, DESC_USERNAME, DEFAULT_USERNAME);
        add(ATTR_PASSWORD, DESC_PASSWORD, DEFAULT_PASSWORD);
        add(ATTR_EXTRACT, DESC_EXTRACT, DEFAULT_EXTRACT);
        add(ATTR_EXTRACT_PARENT, DESC_EXTRACT_PARENT, DEFAULT_EXTRACT_PARENT);
        add(ATTR_MAX_LENGTH, DESC_MAX_LENGTH, DEFAULT_MAX_LENGTH);
        add(ATTR_BANDWIDTH, DESC_BANDWIDTH, DEFAULT_BANDWIDTH);
        add(ATTR_TIMEOUT, DESC_TIMEOUT, DEFAULT_TIMEOUT);
        
        org.archive.crawler.settings.Type e = addElementToDefinition(new SimpleType(
                FetchHTTP.ATTR_DIGEST_CONTENT, FetchHTTP.DESC_DIGEST_CONTENT,
                FetchHTTP.DEFAULT_DIGEST_CONTENT));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(
                FetchHTTP.ATTR_DIGEST_ALGORITHM, 
                FetchHTTP.DESC_DIGEST_ALGORITHM,
                FetchHTTP.DEFAULT_DIGEST_ALGORITHM,
                FetchHTTP.DIGEST_ALGORITHMS));
        e.setExpertSetting(true);
    }
    
    /**
     * Convenience method for adding an attribute.
     * 
     * @param name   The name of the attribute
     * @param desc   The description of the attribute
     * @param def    The default value for the attribute
     */
    private void add(String name, String desc, Object def) {
        SimpleType st = new SimpleType(name, desc, def);
        addElementToDefinition(st);
    }
    
    
    /**
     * Convenience method for extracting an attribute.
     * If a value for the specified name cannot be found,
     * a warning is written to the log and the specified
     * default value is returned instead.
     * 
     * @param context  The context for the attribute fetch
     * @param name     The name of the attribute to fetch
     * @param def      The value to return if the attribute isn't found
     * @return         The value of that attribute
     */
    private Object get(Object context, String name, Object def) {
        try {
            return getAttribute(context, name);
        } catch (AttributeNotFoundException e) {
            logger.warning("Attribute not found (using default): " + name);
            return def;
        }
    }
    

    /**
     * Processes the given URI.  If the given URI is not an FTP URI, then
     * this method does nothing.  Otherwise an attempt is made to connect
     * to the FTP server.
     * 
     * <p>If the connection is successful, an attempt will be made to CD to 
     * the path specified in the URI.  If the remote CD command succeeds, 
     * then it is assumed that the URI represents a directory.  If the
     * CD command fails, then it is assumed that the URI represents
     * a file.
     * 
     * <p>For directories, the directory listing will be fetched using
     * the FTP LIST command, and saved to the HttpRecorder.  If the
     * <code>extract.from.dirs</code> attribute is set to true, then
     * the files in the fetched list will be added to the curi as
     * extracted FTP links.  (It was easier to do that here, rather
     * than writing a separate FTPExtractor.)
     * 
     * <p>For files, the file will be fetched using the FTP RETR
     * command, and saved to the HttpRecorder.
     * 
     * <p>All file transfers (including directory listings) occur using
     * Binary mode transfer.  Also, the local passive transfer mode
     * is always used, to play well with firewalls.
     * 
     * @param curi  the curi to process
     * @throws InterruptedException  if the thread is interrupted during
     *   processing
     */
    public void innerProcess(CrawlURI curi) throws InterruptedException {
        if (!curi.getUURI().getScheme().equals("ftp")) {
            return;
        }
        
        curi.putLong(A_FETCH_BEGAN_TIME, System.currentTimeMillis());
        ClientFTP client = new ClientFTP();
        HttpRecorder recorder = HttpRecorder.getHttpRecorder();
        
        try {
            if (logger.isLoggable(Level.INFO)) {
                logger.info("attempting to fetch ftp uri: " + curi);
            }
            fetch(curi, client, recorder);
        } catch (IOException e) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info(curi + ": " + e);
            }
            curi.addLocalizedError(this.getName(), e, "uhhhh");
            curi.setFetchStatus(S_CONNECT_FAILED);
        } finally {
            disconnect(client);
            curi.putLong(A_FETCH_COMPLETED_TIME, System.currentTimeMillis());
            curi.putString(A_FTP_CONTROL_CONVERSATION, client.getControlConversation());
            if (logger.isLoggable(Level.INFO)) {
                logger.info("finished attempt to fetch ftp uri: " + curi);
            }
        }
    }


    /**
     * Fetches a document from an FTP server.
     * 
     * @param curi      the URI of the document to fetch
     * @param client    the FTPClient to use for the fetch
     * @param recorder  the recorder to preserve the document in
     * @throws IOException  if a network or protocol error occurs
     * @throws InterruptedException  if the thread is interrupted
     */
    private void fetch(CrawlURI curi, ClientFTP client, HttpRecorder recorder) 
    throws IOException, InterruptedException {
        // Connect to the FTP server.
        UURI uuri = curi.getUURI();
        int port = uuri.getPort();
        if (port == -1) {
            port = 21;
        }
        
        client.connect(uuri.getHost(), port);
        
        // Authenticate.
        String[] auth = getAuth(curi);
        client.login(auth[0], auth[1]);
        
        // The given resource may or may not be a directory.
        // To figure out which is which, execute a CD command to
        // the UURI's path.  If CD works, it's a directory.
        boolean isDirectory = client.changeWorkingDirectory(uuri.getPath());
        
        // Get a data socket.  This will either be the result of a NLST
        // command for a directory, or a RETR command for a file.
        int command;
        String path;
        if (isDirectory) {
            curi.addAnnotation("ftpDirectoryList");
            command = FTPCommand.NLST;
            client.setFileType(FTP.ASCII_FILE_TYPE);
            path = ".";
        } else { 
            command = FTPCommand.RETR;
            client.setFileType(FTP.BINARY_FILE_TYPE);
            path = uuri.getPath();
        }

        client.enterLocalPassiveMode();
        Socket socket = null;

        try {
            socket = client.openDataConnection(command, path);

            // if "227 Entering Passive Mode" these will get reset later
            curi.setFetchStatus(client.getReplyCode());
            curi.putString(A_FTP_FETCH_STATUS, client.getReplyStrings()[0]);

        } catch (IOException e) {
            // try it again, see AbstractFrontier.needsRetrying()
            curi.setFetchStatus(S_CONNECT_LOST);
        }

        // Save the streams in the CURI, where downstream processors
        // expect to find them.
        if (socket != null) {
            // Shall we get a digest on the content downloaded?
            boolean digestContent  = ((Boolean)getUncheckedAttribute(curi,
                    FetchHTTP.ATTR_DIGEST_CONTENT)).booleanValue();
            String algorithm = null; 
            if (digestContent) {
                algorithm = ((String)getUncheckedAttribute(curi,
                    FetchHTTP.ATTR_DIGEST_ALGORITHM));
                recorder.getRecordedInput().setDigest(algorithm);
                recorder.getRecordedInput().startDigest();
            } else {
                // clear
                recorder.getRecordedInput().setDigest((MessageDigest)null);
            }
                    
            try {
                saveToRecorder(curi, socket, recorder);
            } finally {
                recorder.close();
                client.closeDataConnection(); // does socket.close()
                curi.setContentSize(recorder.getRecordedInput().getSize());

                // "226 Transfer complete."
                client.getReply();
                curi.setFetchStatus(client.getReplyCode());
                curi.putString(A_FTP_FETCH_STATUS, client.getReplyStrings()[0]);
                
                if (isDirectory) {
                    curi.setContentType("text/plain");
                } else {
                    curi.setContentType("application/octet-stream");
                }
                
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("read " + recorder.getRecordedInput().getSize()
                            + " bytes from ftp data socket");
                }

                if (digestContent) {
                    curi.setContentDigest(algorithm,
                        recorder.getRecordedInput().getDigestValue());
                }
            }

            if (isDirectory) {
                extract(curi, recorder);
            }
        }

        addParent(curi);
    }

    /**
     * Saves the given socket to the given recorder.
     * 
     * @param curi      the curi that owns the recorder
     * @param socket    the socket whose streams to save
     * @param recorder  the recorder to save them to
     * @throws IOException  if a network or file error occurs
     * @throws InterruptedException  if the thread is interrupted
     */
    private void saveToRecorder(CrawlURI curi,
            Socket socket, HttpRecorder recorder) 
    throws IOException, InterruptedException {
        curi.setHttpRecorder(recorder);
        recorder.inputWrap(socket.getInputStream());
        recorder.outputWrap(socket.getOutputStream());
        recorder.markContentBegin();

        // Read the remote file/dir listing in its entirety.
        long softMax = 0;
        long hardMax = getMaxLength(curi);
        long timeout = (long)getTimeout(curi) * 1000;
        int maxRate = getFetchBandwidth(curi);
        RecordingInputStream input = recorder.getRecordedInput();
        input.setLimits(hardMax, timeout, maxRate); 
        input.readFullyOrUntil(softMax);
    }
    
    
    /**
     * Extract FTP links in a directory listing.
     * The listing must already be saved to the given recorder.
     * 
     * @param curi      The curi to save extracted links to
     * @param recorder  The recorder containing the directory listing
     */
    private void extract(CrawlURI curi, HttpRecorder recorder) {
        if (!getExtractFromDirs(curi)) {
            return;
        }
        
        ReplayCharSequence seq = null;
        try {
            seq = recorder.getReplayCharSequence();
            extract(curi, seq);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO error during extraction.", e);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "IO error during extraction.", e);
        } finally {
            close(seq);
        }
    }
    
    
    /**
     * Extracts FTP links in a directory listing.
     * 
     * @param curi  The curi to save extracted links to
     * @param dir   The directory listing to extract links from
     * @throws URIException  if an extracted link is invalid
     */
    private void extract(CrawlURI curi, ReplayCharSequence dir) {
        logger.log(Level.FINEST, "Extracting URIs from FTP directory.");
        Matcher matcher = DIR.matcher(dir);
        while (matcher.find()) {
            String file = matcher.group(1);
            addExtracted(curi, file);
        }
    }


    /**
     * Adds an extracted filename to the curi.  A new URI will be formed
     * by taking the given curi (which should represent the directory the
     * file lives in) and appending the file.
     * 
     * @param curi  the curi to store the discovered link in
     * @param file  the filename of the discovered link
     */
    private void addExtracted(CrawlURI curi, String file) {
        try {
            file = URLEncoder.encode(file, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Found " + file);
        }
        String base = curi.toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        try {
            UURI n = UURIFactory.getInstance(base + "/" + file);
            Link link = new Link(curi.getUURI(), n, NAVLINK_MISC, NAVLINK_HOP);
            curi.addOutLink(link);
        } catch (URIException e) {
            logger.log(Level.WARNING, "URI error during extraction.", e);            
        }
    }
    

    /**
     * Extracts the parent URI from the given curi, then adds that parent
     * URI as a discovered link to the curi. 
     * 
     * <p>If the <code>extract-parent</code> attribute is false, then this
     * method does nothing.  Also, if the path of the given curi is 
     * <code>/</code>, then this method does nothing.
     * 
     * <p>Otherwise the parent is determined by eliminated the lowest part
     * of the URI's path.  Eg, the parent of <code>ftp://foo.com/one/two</code>
     * is <code>ftp://foo.com/one</code>.
     * 
     * @param curi  the curi whose parent to add
     */
    private void addParent(CrawlURI curi) {
        if (!getExtractParent(curi)) {
            return;
        }
        UURI uuri = curi.getUURI();
        try {
            if (uuri.getPath().equals("/")) {
                // There's no parent to add.
                return;
            }
            String scheme = uuri.getScheme();
            String auth = uuri.getEscapedAuthority();
            String path = uuri.getEscapedCurrentHierPath();
            UURI parent = UURIFactory.getInstance(scheme + "://" + auth + path);

            Link link = new Link(uuri, parent, NAVLINK_MISC, NAVLINK_HOP);
            curi.addOutLink(link);
        } catch (URIException e) {
            logger.log(Level.WARNING, "URI error during extraction.", e);
        }
    }
    
    
    /**
     * Returns the <code>extract.from.dirs</code> attribute for this
     * <code>FetchFTP</code> and the given curi.
     * 
     * @param curi  the curi whose attribute to return
     * @return  that curi's <code>extract.from.dirs</code>
     */
    public boolean getExtractFromDirs(CrawlURI curi) {
        return (Boolean)get(curi, ATTR_EXTRACT, DEFAULT_EXTRACT);
    }
    
    
    /**
     * Returns the <code>extract.parent</code> attribute for this
     * <code>FetchFTP</code> and the given curi.
     * 
     * @param curi  the curi whose attribute to return
     * @return  that curi's <code>extract-parent</code>
     */
    public boolean getExtractParent(CrawlURI curi) {
        return (Boolean)get(curi, ATTR_EXTRACT_PARENT, DEFAULT_EXTRACT_PARENT);
    }


    /**
     * Returns the <code>timeout-seconds</code> attribute for this
     * <code>FetchFTP</code> and the given curi.
     * 
     * @param curi   the curi whose attribute to return
     * @return   that curi's <code>timeout-seconds</code>
     */
    public int getTimeout(CrawlURI curi) {
        return (Integer)get(curi, ATTR_TIMEOUT, DEFAULT_TIMEOUT);
    }


    /**
     * Returns the <code>max-length-bytes</code> attribute for this
     * <code>FetchFTP</code> and the given curi.
     * 
     * @param curi  the curi whose attribute to return
     * @return  that curi's <code>max-length-bytes</code>
     */
    public long getMaxLength(CrawlURI curi) {
        return (Long)get(curi, ATTR_MAX_LENGTH, DEFAULT_MAX_LENGTH);
    }


    /**
     * Returns the <code>fetch-bandwidth</code> attribute for this
     * <code>FetchFTP</code> and the given curi.
     * 
     * @param curi  the curi whose attribute to return
     * @return  that curi's <code>fetch-bandwidth</code>
     */
    public int getFetchBandwidth(CrawlURI curi) {
        return (Integer)get(curi, ATTR_BANDWIDTH, DEFAULT_BANDWIDTH);
    }

    /**
     * Returns the username and password for the given URI.  This method
     * always returns an array of length 2.  The first element in the returned
     * array is the username for the URI, and the second element is the
     * password.
     * 
     * <p>If the URI itself contains the username and password (i.e., it looks
     * like <code>ftp://username:password@host/path</code>) then that username
     * and password are returned.
     * 
     * <p>Otherwise the settings system is probed for the <code>username</code>
     * and <code>password</code> attributes for this <code>FTPFetch</code>
     * and the given <code>curi</code> context.  The values of those 
     * attributes are then returned. If the FetchFTP password attribute is 
     * not set, the crawl operator 'From' attribute is used.
     * 
     * @param curi  the curi whose username and password to return
     * @return  an array containing the username and password
     */
    private String[] getAuth(CrawlURI curi) {
        String[] result = new String[2];
        UURI uuri = curi.getUURI();
        String userinfo;
        try {
            userinfo = uuri.getUserinfo();
        } catch (URIException e) {
            assert false;
            logger.finest("getUserinfo raised URIException.");
            userinfo = null;
        }
        if (userinfo != null) {
            int p = userinfo.indexOf(':');
            if (p > 0) {
                result[0] = userinfo.substring(0,p);
                result[1] = userinfo.substring(p + 1);
                return result;
            }
        }

        result[0] = (String)get(curi, ATTR_USERNAME, DEFAULT_USERNAME);
        result[1] = (String)get(curi, ATTR_PASSWORD, DEFAULT_PASSWORD);
        if (result[1].equals("")) {
            result[1] = getSettingsHandler().getOrder().getFrom(curi);
        }

        return result;
    }

    /**
     * Quietly closes the given sequence.
     * If an IOException is raised, this method logs it as a warning.
     * 
     * @param seq  the sequence to close
     */
    private static void close(ReplayCharSequence seq) {
        if (seq == null) {
            return;
        }
        try {
            seq.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "IO error closing ReplayCharSequence.", 
             e);
        }
    }

    /**
     * Quietly disconnects from the given FTP client.
     * If an IOException is raised, this method logs it as a warning.
     * 
     * @param client  the client to disconnect
     */
    private static void disconnect(ClientFTP client) {
        if (client.isConnected()) try {
            client.logout();
        } catch (IOException e) {
        }

        if (client.isConnected()) try {
            client.disconnect();
        } catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Could not disconnect from FTP client: " 
                        + e.getMessage());
            }
        }
    }
}
