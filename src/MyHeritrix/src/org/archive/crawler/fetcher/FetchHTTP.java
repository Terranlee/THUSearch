/* FetchHTTP.java
 *
 * $Id: FetchHTTP.java 6803 2010-04-02 01:03:46Z gojomo $
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

import it.unimi.dsi.mg4j.util.MutableString;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.auth.AuthChallengeParser;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.auth.DigestScheme;
import org.apache.commons.httpclient.auth.MalformedChallengeException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.CredentialStore;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.datamodel.credential.Credential;
import org.archive.crawler.datamodel.credential.CredentialAvatar;
import org.archive.crawler.datamodel.credential.Rfc2617Credential;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecideRuleSequence;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.crawler.settings.Type;
import org.archive.httpclient.ConfigurableX509TrustManager;
import org.archive.httpclient.HttpRecorderGetMethod;
import org.archive.httpclient.HttpRecorderMethod;
import org.archive.httpclient.HttpRecorderPostMethod;
import org.archive.httpclient.SingleHttpConnectionManager;
import org.archive.io.ObjectPlusFilesInputStream;
import org.archive.io.RecorderLengthExceededException;
import org.archive.io.RecorderTimeoutException;
import org.archive.io.RecorderTooMuchHeaderException;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;
import org.archive.util.bdbje.EnhancedEnvironment;

import st.ata.util.AList;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;

/**
 * HTTP fetcher that uses <a
 * href="http://jakarta.apache.org/commons/httpclient/">Apache Jakarta Commons
 * HttpClient</a> library.
 *
 * @author Gordon Mohr
 * @author Igor Ranitovic
 * @author others
 * @version $Id: FetchHTTP.java 6803 2010-04-02 01:03:46Z gojomo $
 */
public class FetchHTTP extends Processor
implements CoreAttributeConstants, FetchStatusCodes, CrawlStatusListener {
    // be robust against trivial implementation changes
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(FetchHTTP.class,1);
    
    private static Logger logger = Logger.getLogger(FetchHTTP.class.getName());

    public static final String ATTR_HTTP_PROXY_HOST = A_HTTP_PROXY_HOST;
    public static final String ATTR_HTTP_PROXY_PORT = A_HTTP_PROXY_PORT;
    public static final String ATTR_TIMEOUT_SECONDS = "timeout-seconds";
    public static final String ATTR_SOTIMEOUT_MS = "sotimeout-ms";
    public static final String ATTR_MAX_LENGTH_BYTES = "max-length-bytes";
    public static final String ATTR_LOAD_COOKIES = "load-cookies-from-file";
    public static final String ATTR_SAVE_COOKIES = "save-cookies-to-file";
    public static final String ATTR_ACCEPT_HEADERS = "accept-headers";
    public static final String ATTR_DEFAULT_ENCODING = "default-encoding";
    public static final String ATTR_DIGEST_CONTENT = "digest-content";
    public static final String ATTR_DIGEST_ALGORITHM = "digest-algorithm";
    public static final String ATTR_FETCH_BANDWIDTH_MAX = "fetch-bandwidth";

    public static final String DESC_DIGEST_CONTENT = "Whether or not to"
        + " perform an on-the-fly digest hash of retrieved content-bodies.";
    public static final String DESC_DIGEST_ALGORITHM = "Which algorithm (for"
        + " example MD5 or SHA-1) to use to perform an on-the-fly digest hash"
        + " of retrieved content-bodies.";

    /**
     * SSL trust level setting attribute name.
     */
    public static final String ATTR_TRUST = "trust-level";
    
    private static Integer DEFAULT_TIMEOUT_SECONDS = new Integer(1200);
    private static Integer DEFAULT_SOTIMEOUT_MS = new Integer(20000);
    private static Long DEFAULT_MAX_LENGTH_BYTES = new Long(0);
    private static Integer DEFAULT_FETCH_BANDWIDTH_MAX = 0;

    /**
     * This is the default value pre-1.4. Needs special handling else
     * treated as negative number doing math later in processing.
     */
    private static long OLD_DEFAULT_MAX_LENGTH_BYTES = 9223372036854775807L;

    /**
     * Default character encoding to use for pages that do not specify.
     */
    private static String DEFAULT_CONTENT_CHARSET = Heritrix.DEFAULT_ENCODING;

    /**
     * Default whether to perform on-the-fly digest hashing of content-bodies.
     */
    static Boolean DEFAULT_DIGEST_CONTENT = new Boolean(true);
          
    /**
     * The different digest algorithms to choose between, 
     * SHA-1 or MD-5 at the moment. 
     */
    public static final String SHA1 = "sha1";
    public static final String MD5 = "md5";
    public static String [] DIGEST_ALGORITHMS = {SHA1, MD5};
   
    /**
     * Default algorithm to use for message disgesting.
     */
    public static final String  DEFAULT_DIGEST_ALGORITHM = SHA1; 
    
    private transient HttpClient http = null;

    /**
     * How many 'instant retries' of HttpRecoverableExceptions have occurred
     * 
     * Would like it to be 'long', but longs aren't atomic
     */
    private int recoveryRetries = 0;

    /**
     * Count of crawl uris handled.
     * Would like to be 'long', but longs aren't atomic
     */
    private int curisHandled = 0;
        
    /**
     * Rules to apply mid-fetch, just after receipt of the response
     * headers before we start to download body.
     */
    public static final String ATTR_MIDFETCH_DECIDE_RULES = "midfetch-decide-rules";
    
    /**
     * What to log if midfetch abort.
     */
    private static final String MIDFETCH_ABORT_LOG = "midFetchAbort";
    
    public static final String ATTR_SEND_CONNECTION_CLOSE =
        "send-connection-close";
    private static final Header HEADER_SEND_CONNECTION_CLOSE =
        new Header("Connection", "close");
    public static final String ATTR_SEND_REFERER = "send-referer";
    public static final String ATTR_SEND_RANGE = "send-range";
    public static final String ATTR_SEND_IF_MODIFIED_SINCE = "send-if-modified-since";
    public static final String ATTR_SEND_IF_NONE_MATCH = "send-if-none-match";
    public static final String REFERER = "Referer";
    public static final String RANGE = "Range";
    public static final String RANGE_PREFIX = "bytes=0-";
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";
    
    public static final String ATTR_IGNORE_COOKIES = "ignore-cookies";
    private static Boolean DEFAULT_IGNORE_COOKIES = new Boolean(false);

    public static final String ATTR_BDB_COOKIES = "use-bdb-for-cookies";
    private static Boolean DEFAULT_BDB_COOKIES = new Boolean(true);
    
    public static final String ATTR_HTTP_BIND_ADDRESS = A_HTTP_BIND_ADDRESS;
    
    /**
     * Database backing cookie map, if using BDB
     */
    protected Database cookieDb; 
    /**
     * Name of cookie BDB Database
     */
    public static final String COOKIEDB_NAME = "http_cookies";
    
    static {
    	Protocol.registerProtocol("http", new Protocol("http",
            new HeritrixProtocolSocketFactory(), 80));
    	try {
			Protocol.registerProtocol("https",
			    new Protocol("https", ((ProtocolSocketFactory)
			        new HeritrixSSLProtocolSocketFactory()), 443));
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
    }
    static final String SERVER_CACHE_KEY = "heritrix.server.cache";
    static final String SSL_FACTORY_KEY = "heritrix.ssl.factory";
    
    /***
     * Socket factory that has the configurable trust manager installed.
     */
    private SSLSocketFactory sslfactory = null;
    

    /**
     * Constructor.
     *
     * @param name Name of this processor.
     */
    public FetchHTTP(String name) {
        super(name, "HTTP Fetcher");

        addElementToDefinition(
            new DecideRuleSequence(ATTR_MIDFETCH_DECIDE_RULES, 
                "DecideRules which, if final decision is REJECT, " +
                "abort fetch after headers before all content is" +
                "read."));
        
        addElementToDefinition(new SimpleType(ATTR_TIMEOUT_SECONDS,
            "If the fetch is not completed in this number of seconds, "
            + "even if it is making progress, give up. The URI will be "
            + "annotated as timeTrunc. Set to zero for no timeout. "
            + "(This is not recommended: threads could wait indefinitely "
            + "for the fetch to end.)",
            DEFAULT_TIMEOUT_SECONDS));
        Type e = addElementToDefinition(new SimpleType(ATTR_SOTIMEOUT_MS,
            "If a socket is unresponsive for this number of milliseconds, " +
            "give up on that connects/read. (This does not necessarily give " +
            "up on the fetch immediately; connects are subject to retries " +
            "and reads will be retried until " + ATTR_TIMEOUT_SECONDS +
            " have elapsed. Set to zero for no socket timeout. (This is " +
            "note recommended: a socket operation could hand indefinitely.",
                DEFAULT_SOTIMEOUT_MS));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_FETCH_BANDWIDTH_MAX,
            "The maximum KB/sec to use when fetching data from a server. " +
            "0 means no maximum.  Default: "+ DEFAULT_FETCH_BANDWIDTH_MAX
             + ".", DEFAULT_FETCH_BANDWIDTH_MAX));
        e.setExpertSetting(true);
        e.setOverrideable(true);
        addElementToDefinition(new SimpleType(ATTR_MAX_LENGTH_BYTES,
            "Maximum length in bytes to fetch.\n" +
            "Fetch is truncated at this length. A value of 0 means no limit.",
            DEFAULT_MAX_LENGTH_BYTES));
        e = addElementToDefinition(new SimpleType(ATTR_IGNORE_COOKIES,
            "Disable cookie-handling.", DEFAULT_IGNORE_COOKIES));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_BDB_COOKIES,
                "Store cookies in BDB-backed map.", DEFAULT_BDB_COOKIES));
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_LOAD_COOKIES,
            "File to preload cookies from", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SAVE_COOKIES,
            "When crawl finishes save cookies to this file", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_TRUST,
            "SSL certificate trust level.  Range is from the default 'open'"
            + " (trust all certs including expired, selfsigned, and those for"
            + " which we do not have a CA) through 'loose' (trust all valid"
            + " certificates including selfsigned), 'normal' (all valid"
            + " certificates not including selfsigned) to 'strict' (Cert is"
            + " valid and DN must match servername)",
            ConfigurableX509TrustManager.DEFAULT,
            ConfigurableX509TrustManager.LEVELS_AS_ARRAY));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        e = addElementToDefinition(new StringList(ATTR_ACCEPT_HEADERS,
            "Accept Headers to include in each request. Each must be the"
            + " complete header, e.g., 'Accept-Language: en'"));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_HTTP_PROXY_HOST,
            "Proxy host IP (set only if needed).", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_HTTP_PROXY_PORT,
            "Proxy port (set only if needed)", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_DEFAULT_ENCODING,
            "The character encoding to use for files that do not have one" +
            " specified in the HTTP response headers.  Default: " +
            DEFAULT_CONTENT_CHARSET + ".",
            DEFAULT_CONTENT_CHARSET));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_DIGEST_CONTENT, DESC_DIGEST_CONTENT,
                DEFAULT_DIGEST_CONTENT));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_DIGEST_ALGORITHM, DESC_DIGEST_ALGORITHM,
                DEFAULT_DIGEST_ALGORITHM, DIGEST_ALGORITHMS));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SEND_IF_MODIFIED_SINCE,
                "Send 'If-Modified-Since' header, if previous 'Last-Modified' " +
                "fetch history information is available in URI history.",
                 new Boolean(true)));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SEND_IF_NONE_MATCH,
                "Send 'If-None-Match' header, if previous 'Etag' fetch " +
                "history information is available in URI history.",
                 new Boolean(true)));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SEND_CONNECTION_CLOSE,
            "Send 'Connection: close' header with every request.",
             new Boolean(true)));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SEND_REFERER,
             "Send 'Referer' header with every request.\n" +
             "The 'Referer' header contans the location the crawler came " +
             " from, " +
             "the page the current URI was discovered in. The 'Referer' " +
             "usually is " +
             "logged on the remote server and can be of assistance to " +
             "webmasters trying to figure how a crawler got to a " +
             "particular area on a site.",
             new Boolean(true)));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SEND_RANGE,
              "Send 'Range' header when a limit (" + ATTR_MAX_LENGTH_BYTES +
              ") on document size.\n" +
              "Be polite to the HTTP servers and send the 'Range' header," +
              "stating that you are only interested in the first n bytes. " +
              "Only pertinent if " + ATTR_MAX_LENGTH_BYTES + " > 0. " +
              "Sending the 'Range' header results in a " +
              "'206 Partial Content' status response, which is better than " +
              "just cutting the response mid-download. On rare occasion, " +
              " sending 'Range' will " +
              "generate '416 Request Range Not Satisfiable' response.",
              new Boolean(false)));
           e.setOverrideable(true);
           e.setExpertSetting(true);
           e = addElementToDefinition(new SimpleType(ATTR_HTTP_BIND_ADDRESS,
               "Local IP address or hostname to use when making connections " +
               "(binding sockets). When not specified, uses default local" +
               "address(es).", ""));
           e.setExpertSetting(true);
    }

    protected void innerProcess(final CrawlURI curi)
    throws InterruptedException {
        if (!canFetch(curi)) {
            // Cannot fetch this, due to protocol, retries, or other problems
            return;
        }

        this.curisHandled++;

        // Note begin time
        curi.putLong(A_FETCH_BEGAN_TIME, System.currentTimeMillis());

        // Get a reference to the HttpRecorder that is set into this ToeThread.
        HttpRecorder rec = HttpRecorder.getHttpRecorder();
        
        // Shall we get a digest on the content downloaded?
        boolean digestContent  = ((Boolean)getUncheckedAttribute(curi,
                ATTR_DIGEST_CONTENT)).booleanValue();
        String algorithm = null;
        if (digestContent) {
            algorithm = ((String)getUncheckedAttribute(curi,
                ATTR_DIGEST_ALGORITHM));
            rec.getRecordedInput().setDigest(algorithm);
        } else {
            // clear
            rec.getRecordedInput().setDigest((MessageDigest)null);
        }        
        
        // Below we do two inner classes that add check of midfetch
        // filters just as we're about to receive the response body.
        String curiString = curi.getUURI().toString();
        HttpMethodBase method = null;
        if (curi.isPost()) {
            method = new HttpRecorderPostMethod(curiString, rec) {
                protected void readResponseBody(HttpState state,
                        HttpConnection conn)
                throws IOException, HttpException {
                    addResponseContent(this, curi);
                    if (checkMidfetchAbort(curi, this.httpRecorderMethod, conn)) {
                        doAbort(curi, this, MIDFETCH_ABORT_LOG);
                    } else {
                        super.readResponseBody(state, conn);
                    }
                }
            };
        } else {
            method = new HttpRecorderGetMethod(curiString, rec) {
                protected void readResponseBody(HttpState state,
                        HttpConnection conn)
                throws IOException, HttpException {
                    addResponseContent(this, curi);
                    if (checkMidfetchAbort(curi, this.httpRecorderMethod,
                            conn)) {
                        doAbort(curi, this, MIDFETCH_ABORT_LOG);
                    } else {
                        super.readResponseBody(state, conn);
                    }
                }
            };
        }

        HostConfiguration customConfigOrNull = configureMethod(curi, method);
        
        // Set httpRecorder into curi. Subsequent code both here and later
        // in extractors expects to find the HttpRecorder in the CrawlURI.
        curi.setHttpRecorder(rec);
        
        // Populate credentials. Set config so auth. is not automatic.
        boolean addedCredentials = populateCredentials(curi, method);
        method.setDoAuthentication(addedCredentials);
        
        // set hardMax on bytes (if set by operator)
        long hardMax = getMaxLength(curi);
        // set overall timeout (if set by operator)
        long timeoutMs = 1000 * getTimeout(curi);
        // Get max fetch rate (bytes/ms). It comes in in KB/sec
        long maxRateKBps = getMaxFetchRate(curi);
        rec.getRecordedInput().setLimits(hardMax, timeoutMs, maxRateKBps);
        
        try {
            this.http.executeMethod(customConfigOrNull, method);
        } catch (RecorderTooMuchHeaderException ex) {
            // when too much header material, abort like other truncations
            doAbort(curi, method, HEADER_TRUNC);
        } catch (IOException e) {
        	failedExecuteCleanup(method, curi, e);
        	return;
        } catch (ArrayIndexOutOfBoundsException e) {
            // For weird windows-only ArrayIndex exceptions in native
            // code... see
            // http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
            // treating as if it were an IOException
            failedExecuteCleanup(method, curi, e);
            return;
        }
        
        // set softMax on bytes to get (if implied by content-length) 
        long softMax = method.getResponseContentLength();

        try {
            if (!method.isAborted()) {
                // Force read-to-end, so that any socket hangs occur here,
                // not in later modules.
                rec.getRecordedInput().readFullyOrUntil(softMax);
            }
        } catch (RecorderTimeoutException ex) {
            doAbort(curi, method, TIMER_TRUNC);
        } catch (RecorderLengthExceededException ex) {
            doAbort(curi, method, LENGTH_TRUNC);
        } catch (IOException e) {
            cleanup(curi, e, "readFully", S_CONNECT_LOST);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            // For weird windows-only ArrayIndex exceptions from native code
            // see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
            // treating as if it were an IOException
            cleanup(curi, e, "readFully", S_CONNECT_LOST);
            return;
        } finally {
            // ensure recording has stopped
            rec.closeRecorders();
            if (!method.isAborted()) {
                method.releaseConnection();
            }
            // Note completion time
            curi.putLong(A_FETCH_COMPLETED_TIME, System.currentTimeMillis());
            // Set the response charset into the HttpRecord if available.
            setCharacterEncoding(rec, method);
            setSizes(curi, rec);
        }
 
        if (digestContent) {
            curi.setContentDigest(algorithm,
                rec.getRecordedInput().getDigestValue());
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info((curi.isPost()? "POST": "GET") + " " +
                curi.getUURI().toString() + " " + method.getStatusCode() +
                " " + rec.getRecordedInput().getSize() + " " +
                curi.getContentType());
        }

        if (curi.isSuccess() && addedCredentials) {
            // Promote the credentials from the CrawlURI to the CrawlServer
            // so they are available for all subsequent CrawlURIs on this
            // server.
            promoteCredentials(curi);
            if (logger.isLoggable(Level.FINE)) {
                // Print out the cookie.  Might help with the debugging.
                Header setCookie = method.getResponseHeader("set-cookie");
                if (setCookie != null) {
                    logger.fine(setCookie.toString().trim());
                }
            }
        } else if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            // 401 is not 'success'.
            handle401(method, curi);
        }
        
        if (rec.getRecordedInput().isOpen()) {
            logger.severe(curi.toString() + " RIS still open. Should have" +
                " been closed by method release: " +
                Thread.currentThread().getName());
            try {
                rec.getRecordedInput().close();
            } catch (IOException e) {
                logger.log(Level.SEVERE,"second-chance RIS close failed",e);
            }
        }
    }

    /**
     * Update CrawlURI internal sizes based on current transaction (and
     * in the case of 304s, history) 
     * 
     * @param curi CrawlURI
     * @param rec HttpRecorder
     */
    protected void setSizes(final CrawlURI curi, HttpRecorder rec) {
        // set reporting size
        curi.setContentSize(rec.getRecordedInput().getSize());
        // special handling for 304-not modified
        if (curi.getFetchStatus() == HttpStatus.SC_NOT_MODIFIED
                && curi.containsKey(A_FETCH_HISTORY)) {
            AList history[] = curi.getAList().getAListArray(A_FETCH_HISTORY);
            if (history[0] != null
                    && history[0]
                            .containsKey(CoreAttributeConstants.A_REFERENCE_LENGTH)) {
                long referenceLength = history[0].getLong(A_REFERENCE_LENGTH);
                // carry-forward previous 'reference-length' for future
                curi.putLong(A_REFERENCE_LENGTH, referenceLength);
                // increase content-size to virtual-size for reporting
                curi.setContentSize(rec.getRecordedInput().getSize()
                        + referenceLength);
            }
        }
    }
    
    protected void doAbort(CrawlURI curi, HttpMethod method,
            String annotation) {
        curi.addAnnotation(annotation);
        curi.getHttpRecorder().close();
        method.abort();
    }
    
    protected boolean checkMidfetchAbort(CrawlURI curi,
            HttpRecorderMethod method, HttpConnection conn) {
        if (curi.isPrerequisite() || rulesAccept(getMidfetchRule(curi), curi)) {
            return false;
        }
        method.markContentBegin(conn);
        return true;
    }
    
    protected DecideRule getMidfetchRule(Object o) {
        try {
            return (DecideRule)getAttribute(o, ATTR_MIDFETCH_DECIDE_RULES);
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * This method populates <code>curi</code> with response status and
     * content type.
     * @param curi CrawlURI to populate.
     * @param method Method to get response status and headers from.
     */
    protected void addResponseContent (HttpMethod method, CrawlURI curi) {
        curi.setFetchStatus(method.getStatusCode());
        Header ct = method.getResponseHeader("content-type");
        curi.setContentType((ct == null)? null: ct.getValue());
        // Save method into curi too.  Midfetch filters may want to leverage
        // info in here.
        curi.putObject(A_HTTP_TRANSACTION, method);
    }

    /**
     * Set the character encoding based on the result headers or default.
     *
     * The HttpClient returns its own default encoding ("ISO-8859-1") if one
     * isn't specified in the Content-Type response header. We give the user
     * the option of overriding this, so we need to detect the case where the
     * default is returned.
     *
     * Now, it may well be the case that the default returned by HttpClient
     * and the default defined by the user are the same.
     * 
     * @param rec Recorder for this request.
     * @param method Method used for the request.
     */
    private void setCharacterEncoding(final HttpRecorder rec,
        final HttpMethod method) {
        String encoding = null;

        try {
            encoding = ((HttpMethodBase) method).getResponseCharSet();
            if (encoding == null ||
                    encoding.equals(DEFAULT_CONTENT_CHARSET)) {
                encoding = (String) getAttribute(ATTR_DEFAULT_ENCODING);
            }
        } catch (Exception e) {
            logger.warning("Failed get default encoding: " +
                e.getLocalizedMessage());
        }
        rec.setCharacterEncoding(encoding);
    }

    /**
     * Cleanup after a failed method execute.
     * @param curi CrawlURI we failed on.
     * @param method Method we failed on.
     * @param exception Exception we failed with.
     */
    private void failedExecuteCleanup(final HttpMethod method,
            final CrawlURI curi, final Exception exception) {
        cleanup(curi, exception, "executeMethod", (method.isRequestSent() ? S_CONNECT_LOST : S_CONNECT_FAILED));
        method.releaseConnection();
    }
    
    /**
     * Cleanup after a failed method execute.
     * @param curi CrawlURI we failed on.
     * @param exception Exception we failed with.
     * @param message Message to log with failure.
     * @param status Status to set on the fetch.
     */
    private void cleanup(final CrawlURI curi, final Exception exception,
            final String message, final int status) {
        curi.addLocalizedError(this.getName(), exception, message);
        curi.setFetchStatus(status);
        curi.getHttpRecorder().close();
    }

    /**
     * Can this processor fetch the given CrawlURI. May set a fetch
     * status if this processor would usually handle the CrawlURI,
     * but cannot in this instance.
     *
     * @param curi
     * @return True if processor can fetch.
     */
    private boolean canFetch(CrawlURI curi) {
        if(curi.getFetchStatus()<0) {
            // already marked as errored, this pass through
            // skip to end
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            return false;             
        }
        String scheme = curi.getUURI().getScheme();
         if (!(scheme.equals("http") || scheme.equals("https"))) {
             // handles only plain http and https
             return false;
         }
         CrawlHost host = getController().getServerCache().getHostFor(curi);
         // make sure the dns lookup succeeded
         if (host.getIP() == null && host.hasBeenLookedUp()) {
             curi.setFetchStatus(S_DOMAIN_PREREQUISITE_FAILURE);
             return false;
         }
        return true;
    }

    /**
     * Configure the HttpMethod setting options and headers.
     *
     * @param curi CrawlURI from which we pull configuration.
     * @param method The Method to configure.
     * @return HostConfiguration copy customized for this CrawlURI
     */
    protected HostConfiguration configureMethod(CrawlURI curi, HttpMethod method) {
        // Don't auto-follow redirects
        method.setFollowRedirects(false);
        
//        // set soTimeout
//        method.getParams().setSoTimeout(
//                ((Integer) getUncheckedAttribute(curi, ATTR_SOTIMEOUT_MS))
//                        .intValue());
        
        // Set cookie policy.
        method.getParams().setCookiePolicy(
            (((Boolean)getUncheckedAttribute(curi, ATTR_IGNORE_COOKIES)).
                booleanValue())?
                    CookiePolicy.IGNORE_COOKIES:
                CookiePolicy.BROWSER_COMPATIBILITY);

        // Use only HTTP/1.0 (to avoid receiving chunked responses)
        method.getParams().setVersion(HttpVersion.HTTP_1_0);

        CrawlOrder order = getSettingsHandler().getOrder();
        String userAgent = curi.getUserAgent();
        if (userAgent == null) {
            userAgent = order.getUserAgent(curi);
        }
        method.setRequestHeader("User-Agent", userAgent);
        method.setRequestHeader("From", order.getFrom(curi));
        
        // Set retry handler.
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
            new HeritrixHttpMethodRetryHandler());
        
        final long maxLength = getMaxLength(curi);
        if(maxLength > 0 &&
                ((Boolean)getUncheckedAttribute(curi, ATTR_SEND_RANGE)).
                    booleanValue()) {
            method.addRequestHeader(RANGE,
                RANGE_PREFIX.concat(Long.toString(maxLength - 1)));
        }
        
        if (((Boolean)getUncheckedAttribute(curi,
                ATTR_SEND_CONNECTION_CLOSE)).booleanValue()) {
            method.addRequestHeader(HEADER_SEND_CONNECTION_CLOSE);
        }
        
        if (((Boolean)getUncheckedAttribute(curi,
                ATTR_SEND_REFERER)).booleanValue() 
                && (curi.getViaContext()==null || 
                        !Link.PREREQ_MISC.equals(
                                curi.getViaContext().toString()))) {
            // RFC2616 says no referer header if referer is https and the url
            // is not
            String via = curi.flattenVia();
            if (via != null && via.length() > 0 &&
                !(via.startsWith(HTTPS_SCHEME) &&
                    curi.getUURI().getScheme().equals(HTTP_SCHEME))) {
                method.setRequestHeader(REFERER, via);
            }
        }
        
        if(!curi.isPrerequisite()) {
            setConditionalGetHeader(curi, method, ATTR_SEND_IF_MODIFIED_SINCE, 
                    CoreAttributeConstants.A_LAST_MODIFIED_HEADER, "If-Modified-Since");
            setConditionalGetHeader(curi, method, ATTR_SEND_IF_NONE_MATCH, 
                    CoreAttributeConstants.A_ETAG_HEADER, "If-None-Match");
        }
        
        // TODO: What happens if below method adds a header already
        // added above: e.g. Connection, Range, or Referer?
        setAcceptHeaders(curi, method);
        
        HostConfiguration config = new HostConfiguration(http.getHostConfiguration());
        configureProxy(curi, config);
        configureBindAddress(curi, config);
        return config;
    }

    /**
     * Set the given conditional-GET header, if the setting is enabled and
     * a suitable value is available in the URI history. 
     * @param curi source CrawlURI
     * @param method HTTP operation pending
     * @param setting true/false enablement setting name to consult
     * @param sourceHeader header to consult in URI history
     * @param targetHeader header to set if possible
     */
    protected void setConditionalGetHeader(CrawlURI curi, HttpMethod method, 
            String setting, String sourceHeader, String targetHeader) {
        if(((Boolean)getUncheckedAttribute(curi,setting))) {
            try {
                int previousStatus = curi.getAList().getAListArray(
                        A_FETCH_HISTORY)[0].getInt(A_STATUS);
                if(previousStatus<=0) {
                    // do not reuse headers from any broken fetch
                    return; 
                }
                String previousValue = curi.getAList().getAListArray(
                        A_FETCH_HISTORY)[0].getString(sourceHeader);
                if(previousValue!=null) {
                    method.setRequestHeader(targetHeader, previousValue);
                }
            } catch (RuntimeException e) {
                // for absent key, bad index, etc. just do nothing
            }
        }
    }

    /**
     * Setup proxy, based on attributes in CrawlURI and settings, 
     * in the given HostConfiguration
     */
    private void configureProxy(CrawlURI curi, HostConfiguration config) {
        String proxy = (String) getAttributeEither(curi, ATTR_HTTP_PROXY_HOST);
        int port = -1; 
        if(proxy.length()==0) {
            proxy = null; 
        } else {
            String portString = (String)getAttributeEither(curi, ATTR_HTTP_PROXY_PORT);
            port = portString.length()>0 ? Integer.parseInt(portString) : -1; 
        }
        if(proxy!=null) {
            config.setProxy(proxy,port);
        }
    }

    /**
     * Setup local bind address, based on attributes in CrawlURI and settings, 
     * in the given HostConfiguration 
     */
    private void configureBindAddress(CrawlURI curi, HostConfiguration config) {
        String addressString = (String) getAttributeEither(curi, ATTR_HTTP_BIND_ADDRESS);
        if(addressString != null && addressString.length() > 0) {
            try {
                InetAddress localAddress = InetAddress.getByName(addressString);
                config.setLocalAddress(localAddress);
            } catch (UnknownHostException e) {
                // Convert all to RuntimeException so get an exception out
                // if initialization fails.
                throw new RuntimeException("Unknown host " + addressString
                    + " in " + ATTR_HTTP_BIND_ADDRESS);
            }
        }
    }
    
    /**
     * Get a value either from inside the CrawlURI instance, or from 
     * settings (module attributes). 
     * 
     * @param curi CrawlURI to consult
     * @param key key to lookup
     * @return value from either CrawlURI (preferred) or settings
     */
    protected Object getAttributeEither(CrawlURI curi, String key) {
        Object obj = curi!=null ? curi.getObject(key) : null;
        if(obj==null) {
            obj = getUncheckedAttribute(curi, key);
        }
        return obj;
    }

    /**
     * Add credentials if any to passed <code>method</code>.
     *
     * Do credential handling.  Credentials are in two places.  1. Credentials
     * that succeeded are added to the CrawlServer (Or rather, avatars for
     * credentials are whats added because its not safe to keep around
     * references to credentials).  2. Credentials to be tried are in the curi.
     * Returns true if found credentials to be tried.
     *
     * @param curi Current CrawlURI.
     * @param method The method to add to.
     * @return True if prepopulated <code>method</code> with credentials AND the
     * credentials came from the <code>curi</code>, not from the CrawlServer.
     * The former is  special in that if the <code>curi</curi> credentials
     * succeed, then the caller needs to promote them from the CrawlURI to the
     * CrawlServer so they are available for all subsequent CrawlURIs on this
     * server.
     */
    private boolean populateCredentials(CrawlURI curi, HttpMethod method) {
        // First look at the server avatars. Add any that are to be volunteered
        // on every request (e.g. RFC2617 credentials).  Every time creds will
        // return true when we call 'isEveryTime().
        CrawlServer server =
            getController().getServerCache().getServerFor(curi);
        if (server.hasCredentialAvatars()) {
            Set avatars = server.getCredentialAvatars();
            for (Iterator i = avatars.iterator(); i.hasNext();) {
                CredentialAvatar ca = (CredentialAvatar)i.next();
                Credential c = ca.getCredential(getSettingsHandler(), curi);
                if (c.isEveryTime()) {
                    c.populate(curi, this.http, method, ca.getPayload());
                }
            }
        }

        boolean result = false;

        // Now look in the curi.  The Curi will have credentials loaded either
        // by the handle401 method if its a rfc2617 or it'll have been set into
        // the curi by the preconditionenforcer as this login uri came through.
        if (curi.hasCredentialAvatars()) {
            Set avatars = curi.getCredentialAvatars();
            for (Iterator i = avatars.iterator(); i.hasNext();) {
                CredentialAvatar ca = (CredentialAvatar)i.next();
                Credential c = ca.getCredential(getSettingsHandler(), curi);
                if (c.populate(curi, this.http, method, ca.getPayload())) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Promote successful credential to the server.
     *
     * @param curi CrawlURI whose credentials we are to promote.
     */
    private void promoteCredentials(final CrawlURI curi) {
        if (!curi.hasCredentialAvatars()) {
            logger.severe("No credentials to promote when there should be " +
                curi);
        } else {
            Set avatars = curi.getCredentialAvatars();
            for (Iterator i = avatars.iterator(); i.hasNext();) {
                CredentialAvatar ca = (CredentialAvatar)i.next();
                curi.removeCredentialAvatar(ca);
                // The server to attach too may not be the server that hosts
                // this passed curi.  It might be of another subdomain.
                // The avatar needs to be added to the server that is dependent
                // on this precondition.  Find it by name.  Get the name from
                // the credential this avatar represents.
                Credential c = ca.getCredential(getSettingsHandler(), curi);
                String cd = null;
                try {
                    cd = c.getCredentialDomain(curi);
                }
                catch (AttributeNotFoundException e) {
                    logger.severe("Failed to get cred domain for " + curi +
                        " for " + ca + ": " + e.getMessage());
                }
                if (cd != null) {
                    CrawlServer cs
                        = getController().getServerCache().getServerFor(cd);
                    if (cs != null) {
                        cs.addCredentialAvatar(ca);
                    }
                }
            }
        }
    }

    /**
     * Server is looking for basic/digest auth credentials (RFC2617). If we have
     * any, put them into the CrawlURI and have it come around again. Presence
     * of the credential serves as flag to frontier to requeue promptly. If we
     * already tried this domain and still got a 401, then our credentials are
     * bad. Remove them and let this curi die.
     *
     * @param method Method that got a 401.
     * @param curi CrawlURI that got a 401.
     */
    protected void handle401(final HttpMethod method, final CrawlURI curi) {
        AuthScheme authscheme = getAuthScheme(method, curi);
        if (authscheme == null) {
        	return;
        }
        String realm = authscheme.getRealm();
        
        // Look to see if this curi had rfc2617 avatars loaded.  If so, are
        // any of them for this realm?  If so, then the credential failed
        // if we got a 401 and it should be let die a natural 401 death.
        Set curiRfc2617Credentials = getCredentials(getSettingsHandler(),
        		curi, Rfc2617Credential.class);
        Rfc2617Credential extant = Rfc2617Credential.
		    getByRealm(curiRfc2617Credentials, realm, curi);
        if (extant != null) {
        	// Then, already tried this credential.  Remove ANY rfc2617
        	// credential since presence of a rfc2617 credential serves
        	// as flag to frontier to requeue this curi and let the curi
        	// die a natural death.
        	extant.detachAll(curi);
        	logger.warning("Auth failed (401) though supplied realm " +
        			realm + " to " + curi.toString());
        } else {
        	// Look see if we have a credential that corresponds to this
        	// realm in credential store.  Filter by type and credential
        	// domain.  If not, let this curi die. Else, add it to the
        	// curi and let it come around again. Add in the AuthScheme
        	// we got too.  Its needed when we go to run the Auth on
        	// second time around.
        	CredentialStore cs =
        		CredentialStore.getCredentialStore(getSettingsHandler());
        	if (cs == null) {
        		logger.severe("No credential store for " + curi);
        	} else {
                CrawlServer server = getController().getServerCache().
                    getServerFor(curi);
        		Set storeRfc2617Credentials = cs.subset(curi,
        		    Rfc2617Credential.class, server.getName());
        		if (storeRfc2617Credentials == null ||
        				storeRfc2617Credentials.size() <= 0) {
        			logger.info("No rfc2617 credentials for " + curi);
        		} else {
        			Rfc2617Credential found = Rfc2617Credential.
					    getByRealm(storeRfc2617Credentials, realm, curi);
        			if (found == null) {
        				logger.info("No rfc2617 credentials for realm " +
        						realm + " in " + curi);
        			} else {
        				found.attach(curi, authscheme.getRealm());
        				logger.info("Found credential for realm " + realm +
        				    " in store for " + curi.toString());
        			}
        		}
        	}
        }
    }
    
    /**
     * @param method Method that got a 401.
     * @param curi CrawlURI that got a 401.
     * @return Returns first wholesome authscheme found else null.
     */
    protected AuthScheme getAuthScheme(final HttpMethod method,
            final CrawlURI curi) {
        Header [] headers = method.getResponseHeaders("WWW-Authenticate");
        if (headers == null || headers.length <= 0) {
            logger.info("We got a 401 but no WWW-Authenticate challenge: " +
                curi.toString());
            return null;
        }

        Map authschemes = null;
        try {
            authschemes = AuthChallengeParser.parseChallenges(headers);
        } catch(MalformedChallengeException e) {
            logger.info("Failed challenge parse: " + e.getMessage());
        }
        if (authschemes == null || authschemes.size() <= 0) {
            logger.info("We got a 401 and WWW-Authenticate challenge" +
                " but failed parse of the header " + curi.toString());
            return null;
        }            
         
        AuthScheme result = null;
        // Use the first auth found.
        for (Iterator i = authschemes.keySet().iterator();
                result == null && i.hasNext();) {
        	String key = (String)i.next();
            String challenge = (String)authschemes.get(key);
            if (key == null || key.length() <= 0 || challenge == null ||
                  challenge.length() <= 0) {
            	logger.warning("Empty scheme: " + curi.toString() +
                  ": " + headers);
            }
        	AuthScheme authscheme = null;
        	if (key.equals("basic")) {
        		authscheme = new BasicScheme();
        	} else if (key.equals("digest")) {
        		authscheme = new DigestScheme();
        	} else {
        		logger.info("Unsupported scheme: " + key);
        		continue;
        	}
            
            try {
				authscheme.processChallenge(challenge);
			} catch (MalformedChallengeException e) {
				logger.info(e.getMessage() + " " + curi + " " + headers);
                continue;
			}
        	if (authscheme.isConnectionBased()) {
        		logger.info("Connection based " + authscheme);
        		continue;
        	}
        	
        	if (authscheme.getRealm() == null ||
        			authscheme.getRealm().length() <= 0) {
        		logger.info("Empty realm " + authscheme + " for " + curi);
        		continue;
        	}
        	result = authscheme;
        }
        
        return result;
    }
        
    /**
     * @param handler Settings Handler.
     * @param curi CrawlURI that got a 401.
     * @param type Class of credential to get from curi.
     * @return Set of credentials attached to this curi.
     */
    private Set<Credential> getCredentials(SettingsHandler handler, 
            CrawlURI curi, Class type) {
        Set<Credential> result = null;

        if (curi.hasCredentialAvatars()) {
            for (Iterator i = curi.getCredentialAvatars().iterator();
                    i.hasNext();) {
                CredentialAvatar ca = (CredentialAvatar)i.next();
                if (ca.match(type)) {
                    if (result == null) {
                        result = new HashSet<Credential>();
                    }
                    result.add(ca.getCredential(handler, curi));
                }
            }
        }
        return result;
    }

    public void initialTasks() {
        super.initialTasks();
        this.getController().addCrawlStatusListener(this);
        configureHttp();

        // load cookies from a file if specified in the order file.
        loadCookies();

        // I tried to get the default KeyManagers but doesn't work unless you
        // point at a physical keystore. Passing null seems to do the right
        // thing so we'll go w/ that.
        try {
        	SSLContext context = SSLContext.getInstance("SSL");
			context.init(null, new TrustManager[] {
			    new ConfigurableX509TrustManager((String)
			        getAttribute(ATTR_TRUST))}, null);
	        this.sslfactory = context.getSocketFactory();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed configure of ssl context "
			    + e.getMessage(), e);
		}
    }
    
    public void finalTasks() {
        // At the end save cookies to the file specified in the order file.
        saveCookies();
        cleanupHttp();
        super.finalTasks();
    }

    /**
     * Perform any final cleanup related to the HttpClient instance.
     */
    protected void cleanupHttp() {
        if(cookieDb!=null) {
            try {
                cookieDb.sync();
                cookieDb.close();
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected void configureHttp() throws RuntimeException {
        // Get timeout.  Use it for socket and for connection timeout.
        int timeout = (getSoTimeout(null) > 0)? getSoTimeout(null): 0;
        
        // HttpConnectionManager cm = new ThreadLocalHttpConnectionManager();
        HttpConnectionManager cm = new SingleHttpConnectionManager();
        
        // TODO: The following settings should be made in the corresponding
        // HttpConnectionManager, not here.
        HttpConnectionManagerParams hcmp = cm.getParams();
        hcmp.setConnectionTimeout(timeout);
        hcmp.setStaleCheckingEnabled(true);
        // Minimizes bandwidth usage.  Setting to true disables Nagle's
        // algorithm.  IBM JVMs < 142 give an NPE setting this boolean
        // on ssl sockets.
        hcmp.setTcpNoDelay(false);
        
        this.http = new HttpClient(cm);
        HttpClientParams hcp = this.http.getParams();
        // Set default socket timeout.
        hcp.setSoTimeout(timeout);
        // Set client to be version 1.0.
        hcp.setVersion(HttpVersion.HTTP_1_0);

		configureHttpCookies();
        
        // Configure how we want the method to act.
        this.http.getParams().setParameter(
            HttpMethodParams.SINGLE_COOKIE_HEADER, new Boolean(true));
        this.http.getParams().setParameter(
            HttpMethodParams.UNAMBIGUOUS_STATUS_LINE , new Boolean(false));
        this.http.getParams().setParameter(
            HttpMethodParams.STRICT_TRANSFER_ENCODING, new Boolean(false));
        this.http.getParams().setIntParameter(
            HttpMethodParams.STATUS_LINE_GARBAGE_LIMIT, 10);
        
        // modify the default config with any global settings
        HostConfiguration config = this.http.getHostConfiguration();
        configureProxy(null, config);
        configureBindAddress(null,config);
        
        // Use our own protocol factory, one that gets IP to use from
        // heritrix cache (They're cached in CrawlHost instances).
        final ServerCache cache = getController().getServerCache();
        hcmp.setParameter(SERVER_CACHE_KEY, cache);
        hcmp.setParameter(SSL_FACTORY_KEY, this.sslfactory);
	}

    /**
     * Set the HttpClient HttpState instance to use a BDB-backed
     * StoredSortedMap for cookie storage, if that option is chosen.
     */
    private void configureHttpCookies() {
        // If Bdb-backed cookies chosen, replace map in HttpState
        if(((Boolean)getUncheckedAttribute(null, ATTR_BDB_COOKIES)).
                booleanValue()) {
            try {
                EnhancedEnvironment env = getController().getBdbEnvironment();
                StoredClassCatalog classCatalog = env.getClassCatalog();
                DatabaseConfig dbConfig = new DatabaseConfig();
                dbConfig.setTransactional(false);
                dbConfig.setAllowCreate(true);
                dbConfig.setDeferredWrite(true);
                cookieDb = env.openDatabase(null, COOKIEDB_NAME, dbConfig);
                StoredSortedMap cookiesMap = new StoredSortedMap(cookieDb,
                        new StringBinding(), new SerialBinding(classCatalog,
                                Cookie.class), true);
                this.http.getState().setCookiesMap(cookiesMap);
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                logger.severe(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * @param curi Current CrawlURI.  Used to get context.
     * @return Socket timeout value.
     */
    private int getSoTimeout(CrawlURI curi) {
        Integer res = null;
        try {
            res = (Integer) getAttribute(ATTR_SOTIMEOUT_MS, curi);
        } catch (Exception e) {
            res = DEFAULT_SOTIMEOUT_MS;
        }
        return res.intValue();
    }

    /**
     * @param curi Current CrawlURI.  Used to get context.
     * @return Timeout value for total request.
     */
    private int getTimeout(CrawlURI curi) {
        Integer res;
        try {
            res = (Integer) getAttribute(ATTR_TIMEOUT_SECONDS, curi);
        } catch (Exception e) {
            res = DEFAULT_TIMEOUT_SECONDS;
        }
        return res.intValue();
    }

    private int getMaxFetchRate(CrawlURI curi) {
        Integer res;
        try {
            res = (Integer)getAttribute(ATTR_FETCH_BANDWIDTH_MAX, curi);
        }
        catch (Exception e) {
            res = DEFAULT_FETCH_BANDWIDTH_MAX;
        }
        return res.intValue();
    }

    private long getMaxLength(CrawlURI curi) {
        Long res;
        try {
            res = (Long) getAttribute(ATTR_MAX_LENGTH_BYTES, curi);
            if (res.longValue() == OLD_DEFAULT_MAX_LENGTH_BYTES) {
                res = DEFAULT_MAX_LENGTH_BYTES;
            }
        } catch (Exception e) {
            res = DEFAULT_MAX_LENGTH_BYTES;
        }
        return res.longValue();
    }

    /**
     * Load cookies from a file before the first fetch.
     * <p>
     * The file is a text file in the Netscape's 'cookies.txt' file format.<br>
     * Example entry of cookies.txt file:<br>
     * <br>
     * www.archive.org FALSE / FALSE 1074567117 details-visit texts-cralond<br>
     * <br>
     * Each line has 7 tab-separated fields:<br>
     * <li>1. DOMAIN: The domain that created and have access to the cookie
     * value.
     * <li>2. FLAG: A TRUE or FALSE value indicating if hosts within the given
     * domain can access the cookie value.
     * <li>3. PATH: The path within the domain that the cookie value is valid
     * for.
     * <li>4. SECURE: A TRUE or FALSE value indicating if to use a secure
     * connection to access the cookie value.
     * <li>5. EXPIRATION: The expiration time of the cookie value (unix style.)
     * <li>6. NAME: The name of the cookie value
     * <li>7. VALUE: The cookie value
     *
     * @param cookiesFile file in the Netscape's 'cookies.txt' format.
     */
    public void loadCookies(String cookiesFile) {
        // Do nothing if cookiesFile is not specified.
        if (cookiesFile == null || cookiesFile.length() <= 0) {
            return;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(cookiesFile, "r");
            String[] cookieParts;
            String line;
            Cookie cookie = null;
            while ((line = raf.readLine()) != null) {
                // Line that starts with # is commented line, therefore skip it.
                if (!line.startsWith("#")) {
                    cookieParts = line.split("\\t");
                    if (cookieParts.length == 7) {
                        // Create cookie with not expiration date (-1 value).
                        // TODO: add this as an option.
                        cookie =
                            new Cookie(cookieParts[0], cookieParts[5],
                                cookieParts[6], cookieParts[2], -1,
                                Boolean.valueOf(cookieParts[3]).booleanValue());

                        if (cookieParts[1].toLowerCase().equals("true")) {
                            cookie.setDomainAttributeSpecified(true);
                        } else {
                            cookie.setDomainAttributeSpecified(false);
                        }
                        this.http.getState().addCookie(cookie);
                        logger.fine(
                            "Adding cookie: " + cookie.toExternalForm());
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // We should probably throw FatalConfigurationException.
            System.out.println("Could not find file: " + cookiesFile
                    + " (Element: " + ATTR_LOAD_COOKIES + ")");

        } catch (IOException e) {
            // We should probably throw FatalConfigurationException.
            e.printStackTrace();
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.fetcher.FetchHTTP\n");
        ret.append("  Function:          Fetch HTTP URIs\n");
        ret.append("  CrawlURIs handled: " + this.curisHandled + "\n");
        ret.append("  Recovery retries:   " + this.recoveryRetries + "\n\n");

        return ret.toString();
    }


    /**
     * Load cookies from the file specified in the order file.
     *
     * <p>
     * The file is a text file in the Netscape's 'cookies.txt' file format.<br>
     * Example entry of cookies.txt file:<br>
     * <br>
     * www.archive.org FALSE / FALSE 1074567117 details-visit texts-cralond<br>
     * <br>
     * Each line has 7 tab-separated fields:<br>
     * <li>1. DOMAIN: The domain that created and have access to the cookie
     * value.
     * <li>2. FLAG: A TRUE or FALSE value indicating if hosts within the given
     * domain can access the cookie value.
     * <li>3. PATH: The path within the domain that the cookie value is valid
     * for.
     * <li>4. SECURE: A TRUE or FALSE value indicating if to use a secure
     * connection to access the cookie value.
     * <li>5. EXPIRATION: The expiration time of the cookie value (unix style.)
     * <li>6. NAME: The name of the cookie value
     * <li>7. VALUE: The cookie value
     */
    public void loadCookies() {
        try {
            loadCookies((String) getAttribute(ATTR_LOAD_COOKIES));
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        }
    }
    /**
     * Saves cookies to the file specified in the order file.
     *
     * Output file is in the Netscape 'cookies.txt' format.
     *
     */
    public void saveCookies() {
        try {
            saveCookies((String) getAttribute(ATTR_SAVE_COOKIES));
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        }
    }
    /**
     * Saves cookies to a file.
     *
     * Output file is in the Netscape 'cookies.txt' format.
     *
     * @param saveCookiesFile output file.
     */
    public void saveCookies(String saveCookiesFile) {
        // Do nothing if cookiesFile is not specified.
        if (saveCookiesFile == null || saveCookiesFile.length() <= 0) {
            return;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(saveCookiesFile));
            @SuppressWarnings("unchecked")
            Map<String,Cookie> cookies = http.getState().getCookiesMap();
            String tab ="\t";
            out.write("# Heritrix Cookie File\n".getBytes());
            out.write(
                "# This file is the Netscape cookies.txt format\n\n".getBytes());
            for (Cookie cookie: cookies.values()) {
                MutableString line =
                    new MutableString(1024 * 2 /*Guess an initial size*/);
                line.append(cookie.getDomain());
                line.append(tab);
                line.append(
                    cookie.isDomainAttributeSpecified() == true
                        ? "TRUE"
                        : "FALSE");
                line.append(tab);
                line.append(cookie.getPath());
                line.append(tab);
                line.append(
                    cookie.getSecure() == true ? "TRUE" : "FALSE");
                line.append(tab);
                line.append(cookie.getName());
                line.append(tab);
                line.append((null==cookie.getValue())?"":cookie.getValue());
                line.append("\n");
                out.write(line.toString().getBytes());
            }
        } catch (FileNotFoundException e) {
            // We should probably throw FatalConfigurationException.
            System.out.println("Could not find file: " + saveCookiesFile
                    + " (Element: " + ATTR_SAVE_COOKIES + ")");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.ModuleType#listUsedFiles(java.util.List)
     */
    protected void listUsedFiles(List<String> list) {
        // List the cookies files
        // Add seed file
        try {
            String tmp = (String)getAttribute(ATTR_LOAD_COOKIES);
            if(tmp != null && tmp.length() > 0){
                File file = getSettingsHandler().
                        getPathRelativeToWorkingDirectory(tmp);
                list.add(file.getAbsolutePath());
            }
            tmp = (String)getAttribute(ATTR_SAVE_COOKIES);
            if(tmp != null && tmp.length() > 0){
                File file = getSettingsHandler().
                        getPathRelativeToWorkingDirectory(tmp);
                list.add(file.getAbsolutePath());
            }
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ReflectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void setAcceptHeaders(CrawlURI curi, HttpMethod get) {
        try {
            StringList accept_headers = (StringList) getAttribute(ATTR_ACCEPT_HEADERS, curi);
            if (!accept_headers.isEmpty()) {
                for (ListIterator i = accept_headers.listIterator(); i.hasNext();) {
                    String hdr = (String) i.next();
                    String[] nvp = hdr.split(": +");
                    if (nvp.length == 2) {
                        get.setRequestHeader(nvp[0], nvp[1]);
                    }
                    else {
                        logger.warning("Invalid accept header: " + hdr);
                    }
                }
            }
        }
        catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
    }

    // custom serialization
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        // save cookies
        @SuppressWarnings("unchecked")
        Collection<Cookie> c = http.getState().getCookiesMap().values();
        Cookie[] cookies = c.toArray(new Cookie[c.size()]);
        stream.writeObject(cookies);
    }
    
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        Cookie cookies[] = (Cookie[]) stream.readObject();
        ObjectPlusFilesInputStream coistream = (ObjectPlusFilesInputStream)stream;
        coistream.registerFinishTask( new PostRestore(cookies) );
    }
    
    /**
     * @return Returns the http instance.
     */
    protected HttpClient getHttp() {
        return this.http;
    }
    
    class PostRestore implements Runnable {
        Cookie cookies[];
        public PostRestore(Cookie cookies[]) {
            this.cookies = cookies;
        }
    	public void run() {
            configureHttp();
            for(int i = 0; i < cookies.length; i++) {
                getHttp().getState().addCookie(cookies[i]);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlCheckpoint(File checkpointDir) {
        if(cookieDb!=null) {
            try {
                cookieDb.sync();
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        this.http = null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        // TODO Auto-generated method stub
    }
}
