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
 * CrawlURI.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.credential.CredentialAvatar;
import org.archive.crawler.datamodel.credential.Rfc2617Credential;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.ProcessorChain;
import org.archive.crawler.util.Transform;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.Base32;
import org.archive.util.HttpRecorder;

import st.ata.util.AList;
import st.ata.util.HashtableAList;


/**
 * Represents a candidate URI and the associated state it
 * collects as it is crawled.
 *
 * <p>Core state is in instance variables but a flexible
 * attribute list is also available. Use this 'bucket' to carry
 * custom processing extracted data and state across CrawlURI
 * processing.  See the {@link #putString(String, String)},
 * {@link #getString(String)}, etc. 
 *
 * @author Gordon Mohr
 */
public class CrawlURI extends CandidateURI
implements FetchStatusCodes {

    private static final long serialVersionUID = 7874096757350100472L;

    public static final int UNCALCULATED = -1;
    
    // INHERITED FROM CANDIDATEURI
    // uuri: core identity: the "usable URI" to be crawled
    // isSeed
    // inScopeVersion
    // pathFromSeed
    // via

    // Processing progress
    transient private Processor nextProcessor;
    transient private ProcessorChain nextProcessorChain;
    private int fetchStatus = 0;    // default to unattempted
    private int deferrals = 0;     // count of postponements for prerequisites
    private int fetchAttempts = 0; // the number of fetch attempts that have been made
    transient private int threadNumber;

    // dynamic context
    /** @deprecated */
    private int linkHopCount = UNCALCULATED; // from seeds
    /** @deprecated */
    private int embedHopCount = UNCALCULATED; // from a sure link; reset upon any link traversal

    // User agent to masquerade as when crawling this URI. If null, globals should be used
    private String userAgent = null;

    // Once a link extractor has finished processing this curi this will be
    // set as true
    transient private boolean linkExtractorFinished = false;

    /**
     * Protection against outlink overflow.
     * Change value by setting alternate maximum in heritrix.properties.
     */
    public static final int MAX_OUTLINKS = Integer.
        parseInt(System.getProperty(CrawlURI.class.getName() + ".maxOutLinks",
            "6000"));
    
    transient private int discardedOutlinks = 0; 
    
////////////////////////////////////////////////////////////////////
    private long contentSize = UNCALCULATED;
    private long contentLength = UNCALCULATED;

    /**
     * Current http recorder.
     *
     * Gets set upon successful request.  Reset at start of processing chain.
     */
    private transient HttpRecorder httpRecorder = null;

    /**
     * Content type of a successfully fetched URI.
     *
     * May be null even on successfully fetched URI.
     */
    private String contentType = null;

    /**
     * True if this CrawlURI has been deemed a prerequisite by the
     * {@link org.archive.crawler.prefetch.PreconditionEnforcer}.
     *
     * This flag is used at least inside in the precondition enforcer so that
     * subsequent prerequisite tests know to let this CrawlURI through because
     * its a prerequisite needed by an earlier prerequisite tests (e.g. If
     * this is a robots.txt, then the subsequent login credentials prereq
     * test must not throw it out because its not a login curi).
     */
    private boolean prerequisite = false;

    /**
     * Set to true if this <code>curi</code> is to be POST'd rather than GET-d.
     */
    private boolean post = false;

    /** 
     * Monotonically increasing number within a crawl;
     * useful for tending towards breadth-first ordering.
     * Will sometimes be truncated to 48 bits, so behavior
     * over 281 trillion instantiated CrawlURIs may be 
     * buggy
     */
    protected long ordinal;

    /**
     * Cache of this candidate uuri as a string.
     *
     * Profiling shows us spending about 1-2% of total elapsed time in
     * toString.
     */
    private String cachedCrawlURIString = null;
    
    /**
     * Array to hold keys of alist members that persist across URI processings.
     * Any key mentioned in this list will not be cleared out at the end
     * of a pass down the processing chain.
     */
    private static final List<Object> alistPersistentMember
     = new CopyOnWriteArrayList<Object>(
            new String [] {A_CREDENTIAL_AVATARS_KEY});

    /**
     * A digest (hash, usually SHA1) of retrieved content-body. 
     * 
     */
    private byte[] contentDigest = null;
    private String contentDigestScheme = null;


    /**
     * Create a new instance of CrawlURI from a {@link UURI}.
     *
     * @param uuri the UURI to base this CrawlURI on.
     */
    public CrawlURI(UURI uuri) {
        super(uuri);
    }

    /**
     * Create a new instance of CrawlURI from a {@link CandidateURI}
     *
     * @param caUri the CandidateURI to base this CrawlURI on.
     * @param o Monotonically increasing number within a crawl.
     */
    @SuppressWarnings("deprecation")
    public CrawlURI(CandidateURI caUri, long o) {
        super(caUri.getUURI(), caUri.getPathFromSeed(), caUri.getVia(),
            caUri.getViaContext());
        ordinal = o;
        setIsSeed(caUri.isSeed());
        setSchedulingDirective(caUri.getSchedulingDirective());
        setAList(caUri.getAList());
    }

    /**
     * Takes a status code and converts it into a human readable string.
     *
     * @param code the status code
     * @return a human readable string declaring what the status code is.
     */
    public static String fetchStatusCodesToString(int code){
        switch(code){
            // DNS
            case S_DNS_SUCCESS : return "DNS-1-OK";
            // HTTP Informational 1xx
            case 100  : return "HTTP-100-Info-Continue";
            case 101  : return "HTTP-101-Info-Switching Protocols";
            // HTTP Successful 2xx
            case 200  : return "HTTP-200-Success-OK";
            case 201  : return "HTTP-201-Success-Created";
            case 202  : return "HTTP-202-Success-Accepted";
            case 203  : return "HTTP-203-Success-Non-Authoritative";
            case 204  : return "HTTP-204-Success-No Content ";
            case 205  : return "HTTP-205-Success-Reset Content";
            case 206  : return "HTTP-206-Success-Partial Content";
            // HTTP Redirection 3xx
            case 300  : return "HTTP-300-Redirect-Multiple Choices";
            case 301  : return "HTTP-301-Redirect-Moved Permanently";
            case 302  : return "HTTP-302-Redirect-Found";
            case 303  : return "HTTP-303-Redirect-See Other";
            case 304  : return "HTTP-304-Redirect-Not Modified";
            case 305  : return "HTTP-305-Redirect-Use Proxy";
            case 307  : return "HTTP-307-Redirect-Temporary Redirect";
            // HTTP Client Error 4xx
            case 400  : return "HTTP-400-ClientErr-Bad Request";
            case 401  : return "HTTP-401-ClientErr-Unauthorized";
            case 402  : return "HTTP-402-ClientErr-Payment Required";
            case 403  : return "HTTP-403-ClientErr-Forbidden";
            case 404  : return "HTTP-404-ClientErr-Not Found";
            case 405  : return "HTTP-405-ClientErr-Method Not Allowed";
            case 407  : return "HTTP-406-ClientErr-Not Acceptable";
            case 408  : return "HTTP-407-ClientErr-Proxy Authentication Required";
            case 409  : return "HTTP-408-ClientErr-Request Timeout";
            case 410  : return "HTTP-409-ClientErr-Conflict";
            case 406  : return "HTTP-410-ClientErr-Gone";
            case 411  : return "HTTP-411-ClientErr-Length Required";
            case 412  : return "HTTP-412-ClientErr-Precondition Failed";
            case 413  : return "HTTP-413-ClientErr-Request Entity Too Large";
            case 414  : return "HTTP-414-ClientErr-Request-URI Too Long";
            case 415  : return "HTTP-415-ClientErr-Unsupported Media Type";
            case 416  : return "HTTP-416-ClientErr-Requested Range Not Satisfiable";
            case 417  : return "HTTP-417-ClientErr-Expectation Failed";
            // HTTP Server Error 5xx
            case 500  : return "HTTP-500-ServerErr-Internal Server Error";
            case 501  : return "HTTP-501-ServerErr-Not Implemented";
            case 502  : return "HTTP-502-ServerErr-Bad Gateway";
            case 503  : return "HTTP-503-ServerErr-Service Unavailable";
            case 504  : return "HTTP-504-ServerErr-Gateway Timeout";
            case 505  : return "HTTP-505-ServerErr-HTTP Version Not Supported";
            // Heritrix internal codes (all negative numbers
            case S_BLOCKED_BY_USER:
                return "Heritrix(" + S_BLOCKED_BY_USER + ")-Blocked by user";
            case S_BLOCKED_BY_CUSTOM_PROCESSOR:
                return "Heritrix(" + S_BLOCKED_BY_CUSTOM_PROCESSOR +
                ")-Blocked by custom prefetch processor";
            case S_DELETED_BY_USER:
                return "Heritrix(" + S_DELETED_BY_USER + ")-Deleted by user";
            case S_CONNECT_FAILED:
                return "Heritrix(" + S_CONNECT_FAILED + ")-Connection failed";
            case S_CONNECT_LOST:
                return "Heritrix(" + S_CONNECT_LOST + ")-Connection lost";
            case S_DEEMED_CHAFF:
                return "Heritrix(" + S_DEEMED_CHAFF + ")-Deemed chaff";
            case S_DEFERRED:
                return "Heritrix(" + S_DEFERRED + ")-Deferred";
            case S_DOMAIN_UNRESOLVABLE:
                return "Heritrix(" + S_DOMAIN_UNRESOLVABLE
                        + ")-Domain unresolvable";
            case S_OUT_OF_SCOPE:
                return "Heritrix(" + S_OUT_OF_SCOPE + ")-Out of scope";
            case S_DOMAIN_PREREQUISITE_FAILURE:
                return "Heritrix(" + S_DOMAIN_PREREQUISITE_FAILURE
                        + ")-Domain prerequisite failure";
            case S_ROBOTS_PREREQUISITE_FAILURE:
                return "Heritrix(" + S_ROBOTS_PREREQUISITE_FAILURE
                        + ")-Robots prerequisite failure";
            case S_OTHER_PREREQUISITE_FAILURE:
                return "Heritrix(" + S_OTHER_PREREQUISITE_FAILURE
                        + ")-Other prerequisite failure";
            case S_PREREQUISITE_UNSCHEDULABLE_FAILURE:
                return "Heritrix(" + S_PREREQUISITE_UNSCHEDULABLE_FAILURE
                        + ")-Prerequisite unschedulable failure";
            case S_ROBOTS_PRECLUDED:
                return "Heritrix(" + S_ROBOTS_PRECLUDED + ")-Robots precluded";
            case S_RUNTIME_EXCEPTION:
                return "Heritrix(" + S_RUNTIME_EXCEPTION
                        + ")-Runtime exception";
            case S_SERIOUS_ERROR:
                return "Heritrix(" + S_SERIOUS_ERROR + ")-Serious error";
            case S_TIMEOUT:
                return "Heritrix(" + S_TIMEOUT + ")-Timeout";
            case S_TOO_MANY_EMBED_HOPS:
                return "Heritrix(" + S_TOO_MANY_EMBED_HOPS
                        + ")-Too many embed hops";
            case S_TOO_MANY_LINK_HOPS:
                return "Heritrix(" + S_TOO_MANY_LINK_HOPS
                        + ")-Too many link hops";
            case S_TOO_MANY_RETRIES:
                return "Heritrix(" + S_TOO_MANY_RETRIES + ")-Too many retries";
            case S_UNATTEMPTED:
                return "Heritrix(" + S_UNATTEMPTED + ")-Unattempted";
            case S_UNFETCHABLE_URI:
                return "Heritrix(" + S_UNFETCHABLE_URI + ")-Unfetchable URI";
            case S_PROCESSING_THREAD_KILLED:
                return "Heritrix(" + S_PROCESSING_THREAD_KILLED + ")-" +
                    "Processing thread killed";
            // Unknown return code
            default : return Integer.toString(code);
        }
    }


    /**
     * Return the overall/fetch status of this CrawlURI for its
     * current trip through the processing loop.
     *
     * @return a value from FetchStatusCodes
     */
    public int getFetchStatus(){
        return fetchStatus;
    }

    /**
     * Set the overall/fetch status of this CrawlURI for
     * its current trip through the processing loop.
     *
     * @param newstatus a value from FetchStatusCodes
     */
    public void setFetchStatus(int newstatus){
        fetchStatus = newstatus;
    }

    /**
     * Get the number of attempts at getting the document referenced by this
     * URI.
     *
     * @return the number of attempts at getting the document referenced by this
     *         URI.
     */
    public int getFetchAttempts() {
        return fetchAttempts;
    }

    /**
     * Increment the number of attempts at getting the document referenced by
     * this URI.
     *
     * @return the number of attempts at getting the document referenced by this
     *         URI.
     */
    public int incrementFetchAttempts() {
        // TODO: rename, this is actually processing-loop-attempts
        return fetchAttempts++;
    }

    /**
     * Reset fetchAttempts counter.
     */
    public void resetFetchAttempts() {
        this.fetchAttempts = 0;
    }

    /**
     * Reset deferrals counter.
     */
    public void resetDeferrals() {
        this.deferrals = 0;
    }

    /**
     * Get the next processor to process this URI.
     *
     * @return the processor that should process this URI next.
     */
    public Processor nextProcessor() {
        return nextProcessor;
    }

    /**
     * Get the processor chain that should be processing this URI after the
     * current chain is finished with it.
     *
     * @return the next processor chain to process this URI.
     */
    public ProcessorChain nextProcessorChain() {
        return nextProcessorChain;
    }

    /**
     * Set the next processor to process this URI.
     *
     * @param processor the next processor to process this URI.
     */
    public void setNextProcessor(Processor processor) {
        nextProcessor = processor;
    }

    /**
     * Set the next processor chain to process this URI.
     *
     * @param nextProcessorChain the next processor chain to process this URI.
     */
    public void setNextProcessorChain(ProcessorChain nextProcessorChain) {
        this.nextProcessorChain = nextProcessorChain;
    }

    /**
     * Do all actions associated with setting a <code>CrawlURI</code> as
     * requiring a prerequisite.
     *
     * @param lastProcessorChain Last processor chain reference.  This chain is
     * where this <code>CrawlURI</code> goes next.
     * @param preq Object to set a prerequisite.
     * @throws URIException
     */
    public void markPrerequisite(String preq,
            ProcessorChain lastProcessorChain) throws URIException {
        Link link = createLink(preq,Link.PREREQ_MISC,Link.PREREQ_HOP);
        setPrerequisiteUri(link);
        incrementDeferrals();
        setFetchStatus(S_DEFERRED);
        skipToProcessorChain(lastProcessorChain);
    }

    /**
     * Set a prerequisite for this URI.
     * <p>
     * A prerequisite is a URI that must be crawled before this URI can be
     * crawled.
     *
     * @param link Link to set as prereq.
     */
    public void setPrerequisiteUri(Object link) {
        putObject(A_PREREQUISITE_URI, link);
    }

    /**
     * Get the prerequisite for this URI.
     * <p>
     * A prerequisite is a URI that must be crawled before this URI can be
     * crawled.
     *
     * @return the prerequisite for this URI or null if no prerequisite.
     */
    public Object getPrerequisiteUri() {
        return getObject(A_PREREQUISITE_URI);
    }
    
    /**
     * @return True if this CrawlURI has a prerequisite.
     */
    public boolean hasPrerequisiteUri() {
        return containsKey(A_PREREQUISITE_URI);
    }

    /**
     * Returns true if this CrawlURI is a prerequisite.
     *
     * @return true if this CrawlURI is a prerequisite.
     */
    public boolean isPrerequisite() {
        return this.prerequisite;
    }

    /**
     * Set if this CrawlURI is itself a prerequisite URI.
     *
     * @param prerequisite True if this CrawlURI is itself a prerequiste uri.
     */
    public void setPrerequisite(boolean prerequisite) {
        this.prerequisite = prerequisite;
    }

    /**
     * @return This crawl URI as a string wrapped with 'CrawlURI(' +
     * ')'.
     */
    public String getCrawlURIString() {
        if (this.cachedCrawlURIString == null) {
            synchronized (this) {
                if (this.cachedCrawlURIString == null) {
                    this.cachedCrawlURIString =
                        "CrawlURI(" + toString() + ")";
                }
            }
        }
        return this.cachedCrawlURIString;
    }

    /**
     * Get the content type of this URI.
     *
     * @return Fetched URIs content type.  May be null.
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Set a fetched uri's content type.
     *
     * @param ct Contenttype.  May be null.
     */
    public void setContentType(String ct) {
        this.contentType = ct;
    }

    /**
     * Set the number of the ToeThread responsible for processing this uri.
     *
     * @param i the ToeThread number.
     */
    public void setThreadNumber(int i) {
        threadNumber = i;
    }

    /**
     * Get the number of the ToeThread responsible for processing this uri.
     *
     * @return the ToeThread number.
     */
    public int getThreadNumber() {
        return threadNumber;
    }

    /**
     * Increment the deferral count.
     *
     */
    public void incrementDeferrals() {
        deferrals++;
    }

    /**
     * Get the deferral count.
     *
     * @return the deferral count.
     */
    public int getDeferrals() {
        return deferrals;
    }

    /**
     * Remove all attributes set on this uri.
     * <p>
     * This methods removes the attribute list.
     */
    public void stripToMinimal() {
        clearAList();
    }

    /** 
     * Get the size in bytes of this URI's recorded content, inclusive
     * of things like protocol headers. It is the responsibility of the 
     * classes which fetch the URI to set this value accordingly -- it is 
     * not calculated/verified within CrawlURI. 
     * 
     * This value is consulted in reporting/logging/writing-decisions.
     * 
     * @see #setContentSize()
     * @return contentSize
     */
    public long getContentSize(){
        return contentSize;
    }

    /**
     * Make note of a non-fatal error, local to a particular Processor,
     * which should be logged somewhere, but allows processing to continue.
     *
     * This is how you add to the local-error log (the 'localized' in
     * the below is making an error local rather than global, not
     * making a swiss-french version of the error.).
     * 
     * @param processorName Name of processor the exception was thrown
     * in.
     * @param ex Throwable to log.
     * @param message Extra message to log beyond exception message.
     */
    public void addLocalizedError(final String processorName,
            final Throwable ex, final String message) {
        List<LocalizedError> localizedErrors;
        if (containsKey(A_LOCALIZED_ERRORS)) {
            @SuppressWarnings("unchecked")
            List<LocalizedError> temp // to prevent warning on cast
             = (List<LocalizedError>) getObject(A_LOCALIZED_ERRORS);
            localizedErrors = temp;
        } else {
            localizedErrors = new ArrayList<LocalizedError>();
            putObject(A_LOCALIZED_ERRORS, localizedErrors);
        }

        localizedErrors.add(new LocalizedError(processorName, ex, message));
        addAnnotation("le:" + getClassSimpleName(ex.getClass()) + "@" +
            processorName);
    }
    
    // TODO: Move to utils.
    protected String getClassSimpleName(final Class c) {
        String classname = c.getName();
        int index = classname.lastIndexOf('.');
        return ((index > 0 && (index + 1) < classname.length())?
            classname.substring(index + 1): classname);
    }

    /**
     * Add an annotation: an abbrieviated indication of something special
     * about this URI that need not be present in every crawl.log line,
     * but should be noted for future reference. 
     *
     * @param annotation the annotation to add; should not contain 
     * whitespace or a comma
     */
    public void addAnnotation(String annotation) {
        String annotations;
        if(containsKey(A_ANNOTATIONS)) {
            annotations = getString(A_ANNOTATIONS);
            annotations += ","+annotation;
        } else {
            annotations = annotation;
        }

        putString(A_ANNOTATIONS,annotations);
    }
    
    /**
     * TODO: Implement truncation using booleans rather than as this
     * ugly String parse.
     * @return True if fetch was truncated.
     */
    public boolean isTruncatedFetch() {
        return annotationContains(TRUNC_SUFFIX);
    }
    
    public boolean isLengthTruncatedFetch() {
        return annotationContains(LENGTH_TRUNC);
    }
    
    public boolean isTimeTruncatedFetch() {
        return annotationContains(TIMER_TRUNC);
    }
    
    public boolean isHeaderTruncatedFetch() {
        return annotationContains(HEADER_TRUNC);
    }
    
    protected boolean annotationContains(final String str2Find) {
        boolean result = false;
        if (!containsKey(A_ANNOTATIONS)) {
            return result;
        }
        String annotations = getString(A_ANNOTATIONS);
        if (annotations != null && annotations.length() > 0) {
            result = annotations.indexOf(str2Find) >= 0;
        }
        return result;
    }

    /**
     * Get the annotations set for this uri.
     *
     * @return the annotations set for this uri.
     */
    public String getAnnotations() {
        return (containsKey(A_ANNOTATIONS))?
            getString(A_ANNOTATIONS): null;
    }

    /**
     * Get the embeded hop count.
     *
     * @return the embeded hop count.
     * @deprecated 
     */
    public int getEmbedHopCount() {
        return embedHopCount;
    }

    /**
     * Get the link hop count.
     *
     * @return the link hop count.
     * @deprecated 
     */
    public int getLinkHopCount() {
        return linkHopCount;
    }

    /**
     * Mark this uri as being a seed.
     *
     *
     * @deprecated 
     */
    public void markAsSeed() {
        linkHopCount = 0;
        embedHopCount = 0;
    }

    /**
     * Get the user agent to use for crawling this URI.
     *
     * If null the global setting should be used.
     *
     * @return user agent or null
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Set the user agent to use when crawling this URI.
     *
     * If not set the global settings should be used.
     *
     * @param string user agent to use
     */
    public void setUserAgent(String string) {
        userAgent = string;
    }

    /**
     * Set which processor should be the next processor to process this uri
     * instead of using the default next processor.
     *
     * @param processorChain the processor chain to skip to.
     * @param processor the processor in the processor chain to skip to.
     */
    public void skipToProcessor(ProcessorChain processorChain,
            Processor processor) {
        setNextProcessorChain(processorChain);
        setNextProcessor(processor);
    }

    /**
     * Set which processor chain should be processing this uri next.
     *
     * @param processorChain the processor chain to skip to.
     */
    public void skipToProcessorChain(ProcessorChain processorChain) {
        setNextProcessorChain(processorChain);
        setNextProcessor(null);
    }

    /**
     * For completed HTTP transactions, the length of the content-body.
     *
     * @return For completed HTTP transactions, the length of the content-body.
     */
    public long getContentLength() {
        if (this.contentLength < 0) {
            this.contentLength = (getHttpRecorder() != null)?
                getHttpRecorder().getResponseContentLength(): 0;
        }
        return this.contentLength;
    }
    
    /**
     * Get size of data recorded (transferred)
     *
     * @return recorded data size
     */
    public long getRecordedSize() {
        return (getHttpRecorder() != null)
                    ?  getHttpRecorder().getRecordedInput().getSize()
                    // if unavailable fall back on content-size
                    : getContentSize(); 
    }

    /**
     * Sets the 'content size' for the URI, which is considered inclusive
     * of all recorded material (such as protocol headers) or even material
     * 'virtually' considered (as in material from a previous fetch 
     * confirmed unchanged with a server). (In contrast, content-length 
     * matches the HTTP definition, that of the enclosed content-body.)
     * 
     * Should be set by a fetcher or other processor as soon as the final 
     * size of recorded content is known. Setting to an artificial/incorrect
     * value may affect other reporting/processing. 
     * 
     * @param l Content size.
     */
    public void setContentSize(long l) {
        contentSize = l;
    }

    /**
     * If true then a link extractor has already claimed this CrawlURI and
     * performed link extraction on the document content. This does not
     * preclude other link extractors that may have an interest in this
     * CrawlURI from also doing link extraction but default behavior should
     * be to not run if link extraction has already been done.
     * 
     * <p>There is an onus on link extractors to set this flag if they have
     * run.
     * 
     * <p>The only extractor of the default Heritrix set that does not
     * respect this flag is
     * {@link org.archive.crawler.extractor.ExtractorHTTP}.
     * It runs against HTTP headers, not the document content.
     * 
     * @return True if a processor has performed link extraction on this
     * CrawlURI
     *
     * @see #linkExtractorFinished()
     */
    public boolean hasBeenLinkExtracted(){
        return linkExtractorFinished;
    }

    /**
     * Note that link extraction has been performed on this CrawlURI. A processor
     * doing link extraction should invoke this method once it has finished it's
     * work. It should invoke it even if no links are extracted. It should only
     * invoke this method if the link extraction was performed on the document
     * body (not the HTTP headers etc.).
     *
     * @see #hasBeenLinkExtracted()
     */
    public void linkExtractorFinished() {
        linkExtractorFinished = true;
        if(discardedOutlinks>0) {
            addAnnotation("dol:"+discardedOutlinks);
        }
    }

    /**
     * Notify CrawlURI it is about to be logged; opportunity
     * for self-annotation
     */
    public void aboutToLog() {
        if (fetchAttempts>1) {
            addAnnotation(fetchAttempts+"t");
        }
    }

    /**
     * Get the http recorder associated with this uri.
     *
     * @return Returns the httpRecorder.  May be null but its set early in
     * FetchHttp so there is an issue if its null.
     */
    public HttpRecorder getHttpRecorder() {
        return httpRecorder;
    }

    /**
     * Set the http recorder to be associated with this uri.
     *
     * @param httpRecorder The httpRecorder to set.
     */
    public void setHttpRecorder(HttpRecorder httpRecorder) {
        this.httpRecorder = httpRecorder;
    }

    /**
     * Return true if this is a http transaction.
     *
     * TODO: Compound this and {@link #isPost()} method so that there is one
     * place to go to find out if get http, post http, ftp, dns.
     *
     * @return True if this is a http transaction.
     */
    public boolean isHttpTransaction() {
        return containsKey(A_HTTP_TRANSACTION);
    }

    /**
     * Clean up after a run through the processing chain.
     *
     * Called on the end of processing chain by Frontier#finish.  Null out any
     * state gathered during processing.
     */
    public void processingCleanup() {
        this.httpRecorder = null;
        this.fetchStatus = S_UNATTEMPTED;
        this.setPrerequisite(false);
        this.contentSize = UNCALCULATED;
        this.contentLength = UNCALCULATED;
        // Clear 'links extracted' flag.
        this.linkExtractorFinished = false;
        // Clean the alist of all but registered permanent members.
        setAList(getPersistentAList());
    }
    
    public AList getPersistentAList() {
        AList newAList = new HashtableAList();
        // copy declared persistent keys
        if(alistPersistentMember!=null && alistPersistentMember.size() > 0) {
            newAList.copyKeysFrom(alistPersistentMember.iterator(), getAList());
        } 
        // also copy declared 'heritable' keys
        List heritableKeys = (List) getObject(A_HERITABLE_KEYS);
        if(heritableKeys!=null) {
            newAList.copyKeysFrom(heritableKeys.iterator(), getAList());
        }
        return newAList;
    }

    /**
     * Make a <code>CrawlURI</code> from the passed <code>CandidateURI</code>.
     *
     * Its safe to pass a CrawlURI instance.  In this case we just return it
     * as a result. Otherwise, we create new CrawlURI instance.
     *
     * @param caUri Candidate URI.
     * @param ordinal
     * @return A crawlURI made from the passed CandidateURI.
     */
    public static CrawlURI from(CandidateURI caUri, long ordinal) {
        return (caUri instanceof CrawlURI)?
            (CrawlURI)caUri: new CrawlURI(caUri, ordinal);
    }

    /**
     * @param avatars Credential avatars to save off.
     */
    private void setCredentialAvatars(Set avatars) {
        putObject(A_CREDENTIAL_AVATARS_KEY, avatars);
    }

    /**
     * @return Credential avatars.  Null if none set.
     */
    @SuppressWarnings("unchecked")
    public Set<CredentialAvatar> getCredentialAvatars() {
        return (Set)getObject(A_CREDENTIAL_AVATARS_KEY);
    }

    /**
     * @return True if there are avatars attached to this instance.
     */
    public boolean hasCredentialAvatars() {
        return getCredentialAvatars() != null &&
            getCredentialAvatars().size() > 0;
    }

    /**
     * Add an avatar.
     *
     * We do lazy instantiation.
     *
     * @param ca Credential avatar to add to set of avatars.
     */
    public void addCredentialAvatar(CredentialAvatar ca) {
        Set<CredentialAvatar> avatars = getCredentialAvatars();
        if (avatars == null) {
            avatars = new HashSet<CredentialAvatar>();
            setCredentialAvatars(avatars);
        }
        avatars.add(ca);
    }

    /**
     * Remove all credential avatars from this crawl uri.
     */
    public void removeCredentialAvatars() {
        if (hasCredentialAvatars()) {
            remove(A_CREDENTIAL_AVATARS_KEY);
        }
    }

    /**
     * Remove all credential avatars from this crawl uri.
     * @param ca Avatar to remove.
     * @return True if we removed passed parameter.  False if no operation
     * performed.
     */
    public boolean removeCredentialAvatar(CredentialAvatar ca) {
        boolean result = false;
        Set avatars = getCredentialAvatars();
        if (avatars != null && avatars.size() > 0) {
            result = avatars.remove(ca);
        }
        return result;
    }

    /**
     * Ask this URI if it was a success or not.
     *
     * Only makes sense to call this method after execution of
     * HttpMethod#execute. Regard any status larger then 0 as success
     * except for below caveat regarding 401s.  Use {@link #is2XXSuccess()} if
     * looking for a status code in the 200 range.
     *
     * <p>401s caveat: If any rfc2617 credential data present and we got a 401
     * assume it got loaded in FetchHTTP on expectation that we're to go around
     * the processing chain again. Report this condition as a failure so we
     * get another crack at the processing chain only this time we'll be making
     * use of the loaded credential data.
     *
     * @return True if ths URI has been successfully processed.
     * @see #is2XXSuccess()
     */
    public boolean isSuccess() {
        boolean result = false;
        int statusCode = this.fetchStatus;
        if (statusCode == HttpStatus.SC_UNAUTHORIZED &&
            hasRfc2617CredentialAvatar()) {
            result = false;
        } else {
            result = (statusCode > 0);
        }
        return result;
    }
    
    /**
     * @return True if status code is in the 2xx range.
     * @see #isSuccess()
     */
    public boolean is2XXSuccess() {
    	return this.fetchStatus >= 200 && this.fetchStatus < 300;
    }

    /**
	 * @return True if we have an rfc2617 payload.
	 */
	public boolean hasRfc2617CredentialAvatar() {
	    boolean result = false;
	    Set avatars = getCredentialAvatars();
	    if (avatars != null && avatars.size() > 0) {
	        for (Iterator i = avatars.iterator(); i.hasNext();) {
	            if (((CredentialAvatar)i.next()).
	                match(Rfc2617Credential.class)) {
	                result = true;
	                break;
	            }
	        }
	    }
        return result;
	}

    /**
     * Set whether this URI should be fetched by sending a HTTP POST request.
     * Else a HTTP GET request will be used.
     *
     * @param b Set whether this curi is to be POST'd.  Else its to be GET'd.
     */
    public void setPost(boolean b) {
        this.post = b;
    }

    /**
     * Returns true if this URI should be fetched by sending a HTTP POST request.
     *
     *
     * TODO: Compound this and {@link #isHttpTransaction()} method so that there
     * is one place to go to find out if get http, post http, ftp, dns.
     *
     * @return Returns is this CrawlURI instance is to be posted.
     */
    public boolean isPost() {
        return this.post;
    }

    /**
     * Set the retained content-digest value (usu. SHA1). 
     * 
     * @param digestValue
     * @deprecated Use {@link #setContentDigest(String scheme, byte[])}
     */
    public void setContentDigest(byte[] digestValue) {
        setContentDigest("SHA1", digestValue);
    }
    
    public void setContentDigest(final String scheme,
            final byte [] digestValue) {
        this.contentDigest = digestValue;
        this.contentDigestScheme = scheme;
    }
    
    public String getContentDigestSchemeString() {
        if(this.contentDigest==null) {
            return null;
        }
        return this.contentDigestScheme + ":" + getContentDigestString();
    }

    /**
     * Return the retained content-digest value, if any.
     * 
     * @return Digest value.
     */
    public Object getContentDigest() {
        return contentDigest;
    }
    
    public String getContentDigestString() {
        if(this.contentDigest==null) {
            return null;
        }
        return Base32.encode(this.contentDigest);
    }

    transient Object holder;
    transient Object holderKey;

    /**
     * Remember a 'holder' to which some enclosing/queueing
     * facility has assigned this CrawlURI
     * .
     * @param obj
     */
    public void setHolder(Object obj) {
        holder=obj;
    }

    /**
     * Return the 'holder' for the convenience of 
     * an external facility.
     *
     * @return holder
     */
    public Object getHolder() {
        return holder;
    }

    /**
     * Remember a 'holderKey' which some enclosing/queueing
     * facility has assigned this CrawlURI
     * .
     * @param obj
     */
    public void setHolderKey(Object obj) {
        holderKey=obj;
    }
    /**
     * Return the 'holderKey' for convenience of 
     * an external facility (Frontier).
     * 
     * @return holderKey 
     */
    public Object getHolderKey() {
        return holderKey;
    }

    /**
     * Get the ordinal (serial number) assigned at creation.
     * 
     * @return ordinal
     */
    public long getOrdinal() {
        return ordinal;
    }

    /** spot for an integer cost to be placed by external facility (frontier).
     *  cost is truncated to 8 bits at times, so should not exceed 255 */
    int holderCost = UNCALCULATED;
    /**
     * Return the 'holderCost' for convenience of external facility (frontier)
     * @return value of holderCost
     */
    public int getHolderCost() {
        return holderCost;
    }

    /**
     * Remember a 'holderCost' which some enclosing/queueing
     * facility has assigned this CrawlURI
     * @param cost value to remember
     */
    public void setHolderCost(int cost) {
        holderCost = cost;
    }

    /** 
     * All discovered outbound Links (navlinks, embeds, etc.) 
     * Can either contain Link instances or CandidateURI instances, or both.
     * The LinksScoper processor converts Link instances in this collection
     * to CandidateURI instances. 
     */
    transient Collection<Object> outLinks = new ArrayList<Object>();
    
    /**
     * Returns discovered links.  The returned collection might be empty if
     * no links were discovered, or if something like LinksScoper promoted
     * the links to CandidateURIs.
     * 
     * Elements can be removed from the returned collection, but not added.
     * To add a discovered link, use one of the createAndAdd methods or
     * {@link #getOutObjects()}.
     * 
     * @return Collection of all discovered outbound Links
     */
    public Collection<Link> getOutLinks() {
        return Transform.subclasses(outLinks, Link.class);
    }
    
    /**
     * Returns discovered candidate URIs.  The returned collection will be
     * emtpy until something like LinksScoper promotes discovered Links
     * into CandidateURIs.
     * 
     * Elements can be removed from the returned collection, but not added.
     * To add a candidate URI, use {@link #replaceOutlinks(Collection)} or
     * {@link #getOutObjects}.
     * 
     * @return  Collection of candidate URIs
     */
    public Collection<CandidateURI> getOutCandidates() {
        return Transform.subclasses(outLinks, CandidateURI.class);
    }
    
    
    /**
     * Returns all of the outbound objects.  The returned Collection will
     * contain Link instances, or CandidateURI instances, or both.  
     * 
     * @return  the collection of Links and/or CandidateURIs
     */
    public Collection<Object> getOutObjects() {
        return outLinks;
    }
    
    /**
     * Add a discovered Link, unless it would exceed the max number
     * to accept. (If so, increment discarded link counter.) 
     * 
     * @param link the Link to add
     */
    public void addOutLink(Link link) {
        if (outLinks.size() < MAX_OUTLINKS) {
            outLinks.add(link);
        } else {
            // note & discard
            discardedOutlinks++;
        }
    }
    
    public void clearOutlinks() {
        this.outLinks.clear();
    }
    
    /**
     * Replace current collection of links w/ passed list.
     * Used by Scopers adjusting the list of links (removing those
     * not in scope and promoting Links to CandidateURIs).
     * 
     * @param a collection of CandidateURIs replacing any previously
     *   existing outLinks or outCandidates
     */
    public void replaceOutlinks(Collection<CandidateURI> links) {
        clearOutlinks();
        this.outLinks.addAll(links);
    }
    
    
    /**
     * @return Count of outlinks.
     */
    public int outlinksSize() {
        return this.outLinks.size();
    }

    /**
     * Convenience method for creating a Link discovered at this URI
     * with the given string and context
     * 
     * @param url
     *            String to use to create Link
     * @param context
     *            CharSequence context to use
     * @param hopType
     * @return Link.
     * @throws URIException
     *             if Link UURI cannot be constructed
     */
    public Link createLink(String url, CharSequence context,
            char hopType) throws URIException {
        return new Link(getUURI(), UURIFactory.getInstance(getUURI(),
                url), context, hopType);
    }
    
    /**
     * Convenience method for creating a Link with the given string and
     * context
     * 
     * @param url
     *            String to use to create Link
     * @param context
     *            CharSequence context to use
     * @param hopType
     * @throws URIException
     *             if Link UURI cannot be constructed
     */
    public void createAndAddLink(String url, CharSequence context,
            char hopType) throws URIException {
        addOutLink(createLink(url, context, hopType));
    }

    /**
     * Convenience method for creating a Link with the given string and
     * context, relative to a previously set base HREF if available (or
     * relative to the current CrawlURI if no other base has been set)
     * 
     * @param url String URL to add as destination of link
     * @param context String context where link was discovered
     * @param hopType char hop-type indicator
     * @throws URIException
     */
    public void createAndAddLinkRelativeToBase(String url,
            CharSequence context, char hopType) throws URIException {
        addOutLink(new Link(getUURI(), UURIFactory.getInstance(
                getBaseURI(), url), context, hopType));
    }
    
    /**
     * Convenience method for creating a Link with the given string and
     * context, relative to this CrawlURI's via UURI if available. (If
     * a via is not available, falls back to using 
     * #createAndAddLinkRelativeToBase.)
     * 
     * @param url String URL to add as destination of link
     * @param context String context where link was discovered
     * @param hopType char hop-type indicator
     * @throws URIException
     */
    public void createAndAddLinkRelativeToVia(String url,
            CharSequence context, char hopType) throws URIException {
        if(getVia()!=null) {
            addOutLink(new Link(getUURI(), UURIFactory.getInstance(
                getVia(), url), context, hopType));
        } else {
            // if no 'via', fall back to base/self
            createAndAddLinkRelativeToBase(url,context,hopType);
        }
    }
    
    /**
     * Set the (HTML) Base URI used for derelativizing internal URIs. 
     * 
     * @param baseHref String base href to use
     * @throws URIException if supplied string cannot be interpreted as URI
     */
    public void setBaseURI(String baseHref) throws URIException {
        putObject(A_HTML_BASE, UURIFactory.getInstance(baseHref));
    }
      
    /**
     * Get the (HTML) Base URI used for derelativizing internal URIs. 
     *
     * @return UURI base URI previously set 
     */  
    public UURI getBaseURI() {
        if (!containsKey(A_HTML_BASE)) {
            return getUURI();
        }
        return (UURI)getObject(A_HTML_BASE);
    }
    
    /**
     * Add the key of alist items you want to persist across
     * processings.
     * @param key Key to add.
     */
    public static void addAlistPersistentMember(Object key) {
        alistPersistentMember.add(key);
    }
    
    /**
     * @param key Key to remove.
     * @return True if list contained the element.
     */
    public static boolean removeAlistPersistentMember(Object key) {
        return alistPersistentMember.remove(key);
    }

    /**
     * Custom serialization writing an empty 'outLinks' as null. Estimated
     * to save ~20 bytes in serialized form. 
     * 
     * @param stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject((outLinks.isEmpty()) ? null : outLinks);
    }

    /**
     * Custom deserialization recreating empty HashSet from null in 'outLinks'
     * slot. 
     * 
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        stream.defaultReadObject();
        @SuppressWarnings("unchecked")
        Collection<Object> ol = (Collection<Object>) stream.readObject();
        outLinks = (ol == null) ? new ArrayList<Object>() : ol;
    }

    public long getFetchDuration() {
        if(! containsKey(A_FETCH_COMPLETED_TIME)) {
            return -1;
        }
        
        long completedTime = getLong(A_FETCH_COMPLETED_TIME);
        long beganTime = getLong(A_FETCH_BEGAN_TIME);
        return completedTime - beganTime;
    }


}
