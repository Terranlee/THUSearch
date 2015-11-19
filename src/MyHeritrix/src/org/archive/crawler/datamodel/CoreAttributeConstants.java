/* Copyright (C) 2003 Internet Archive.
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
 * CoreAttributeConstants.java
 * Created on Jun 17, 2003
 *
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src//**
 * @author gojomo
 *
 */
package org.archive.crawler.datamodel;

/**
 * CrawlURI attribute keys used by the core crawler
 * classes.
 *
 * @author gojomo
 *
 */
public interface CoreAttributeConstants {

    /**
     * Extracted MIME type of fetched content; should be
     * set immediately by fetching module if possible
     * (rather than waiting for a later analyzer)
     */
    public static String A_CONTENT_TYPE = "content-type";

    /**
     * Multiplier of last fetch duration to wait before
     * fetching another item of the same class (eg host)
     */
    public static String A_DELAY_FACTOR = "delay-factor";
    /**
     * Minimum delay before fetching another item of th
     * same class (eg host). Even if lastFetchTime*delayFactor
     * is less than this, this period will be waited.
     */
    public static String A_MINIMUM_DELAY = "minimum-delay";

    public static String A_RRECORD_SET_LABEL = "dns-records";
    public static String A_DNS_FETCH_TIME    = "dns-fetch-time";
    public static String A_DNS_SERVER_IP_LABEL = "dns-server-ip";
    public static String A_FETCH_BEGAN_TIME= "fetch-began-time";
    public static String A_FETCH_COMPLETED_TIME = "fetch-completed-time";
    public static String A_HTTP_TRANSACTION = "http-transaction";
    public static String A_FTP_CONTROL_CONVERSATION = "ftp-control-conversation";
    public static String A_FTP_FETCH_STATUS = "ftp-fetch-status";

    public static String A_RUNTIME_EXCEPTION = "runtime-exception";
    public static String A_LOCALIZED_ERRORS = "localized-errors";

    /** shorthand string tokens indicating notable occurences,
     * separated by commas */
    public static String A_ANNOTATIONS = "annotations";

    public static String A_PREREQUISITE_URI = "prerequisite-uri";
    public static String A_DISTANCE_FROM_SEED = "distance-from-seed";
    public static String A_HTML_BASE = "html-base-href";
    public static String A_RETRY_DELAY = "retry-delay";

    public static String A_META_ROBOTS = "meta-robots";
    /** 
     * Define for org.archive.crawler.writer.MirrorWriterProcessor.
     */
    public static String A_MIRROR_PATH = "mirror-path";

    /**
     * Key to get credential avatars from A_LIST.
     */
    public static final String A_CREDENTIAL_AVATARS_KEY =
        "credential-avatars";
    
    /** a 'source' (usu. URI) that's inherited by discovered URIs */
    public static String A_SOURCE_TAG = "source";
    
    /**
     * Key to (optional) attribute specifying a list of keys that
     * are passed to CandidateURIs that 'descend' (are discovered 
     * via) this URI. 
     */
    public static final String A_HERITABLE_KEYS = "heritable";
    
    /** flag indicating the containing queue should be retired */ 
    public static final String A_FORCE_RETIRE = "force-retire";
    
    /** local override of proxy host */ 
    public static final String A_HTTP_PROXY_HOST = "http-proxy-host";
    /** local override of proxy port */ 
    public static final String A_HTTP_PROXY_PORT = "http-proxy-port";

    /** local override of origin bind address */ 
    public static final String A_HTTP_BIND_ADDRESS = "http-bind-address";
    
    /**
     * Fetch truncation codes present in {@link CrawlURI} annotations.
     * All truncation annotations have a <code>TRUNC_SUFFIX</code> suffix (TODO:
     * Make for-sure unique or redo truncation so definitive flag marked
     * against {@link CrawlURI}).
     */
    public static final String TRUNC_SUFFIX = "Trunc";
    // headerTrunc
    public static final String HEADER_TRUNC = "header" + TRUNC_SUFFIX; 
    // timeTrunc
    public static final String TIMER_TRUNC = "time" + TRUNC_SUFFIX;
    // lenTrunc
    public static final String LENGTH_TRUNC = "len" + TRUNC_SUFFIX;

    /* Duplication-reduction / recrawl / history constants */
    
    /** fetch history array */ 
    public static final String A_FETCH_HISTORY = "fetch-history";
    /** content digest */
    public static final String A_CONTENT_DIGEST = "content-digest";
	/** header name (and AList key) for last-modified timestamp */
    public static final String A_LAST_MODIFIED_HEADER = "last-modified";
	/** header name (and AList key) for ETag */
    public static final String A_ETAG_HEADER = "etag"; 
    /** key for status (when in history) */
    public static final String A_STATUS = "status"; 
    /** reference length (content length or virtual length */
    public static final String A_REFERENCE_LENGTH = "reference-length";

}
