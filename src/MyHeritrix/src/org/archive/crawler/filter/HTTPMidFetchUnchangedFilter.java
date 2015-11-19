/* HTTPMidFetchUnhangedFilter
 * 
 * $Id: HTTPMidFetchUnchangedFilter.java 4652 2006-09-25 18:41:10Z paul_jack $
 * 
 * Created on 4.2.2005
 *
 * Copyright (C) 2005 Kristinn Sigur?sson
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
package org.archive.crawler.filter;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpMethod;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants;

/**
 * A mid fetch filter for HTTP fetcher processors. It will evaluate the HTTP
 * header to try and predict if the document has changed since it last passed
 * through this filter. It does this by comparing the last-modified and etag 
 * values with the same values stored during the last processing of the URI.
 * <p>
 * If both values are present, they must agree on predicting no change, 
 * otherwise a change is predicted (return true).
 * <p>
 * If only one of the values is present, it alone is used to predict if a 
 * change has occured.
 * <p>
 * If neither value is present the filter will return true (predict change) 
 *
 * @author Kristinn Sigurdsson
 */
public class HTTPMidFetchUnchangedFilter extends Filter 
implements AdaptiveRevisitAttributeConstants {

    private static final long serialVersionUID = -7416477243375196980L;

    private static final Logger logger =
        Logger.getLogger(HTTPMidFetchUnchangedFilter.class.getName());

    // Header predictor state constants
    public static final int HEADER_PREDICTS_MISSING = -1;
    public static final int HEADER_PREDICTS_UNCHANGED = 0;
    public static final int HEADER_PREDICTS_CHANGED = 1;
    
    /**
     * Constructor
     * 
     * @param name Module name
     */
    public HTTPMidFetchUnchangedFilter(String name){
        this(name, "Filters out unchanged documents. " +
                "Examines HTTP Header timestamp and etags. " +
                "This filter should" +
                "only be used in the 'midfetch-filters' on the FetchHTTP " +
                "processor. Earlier then that, the headers are not available " +
                "and later, the entire document is available and examining " +
                "this will usually give better results then relying on HTTP " +
                "headers. See documentation for further details.");

        // Register persistent CrawlURI items 
        CrawlURI.addAlistPersistentMember(A_LAST_DATESTAMP);
        CrawlURI.addAlistPersistentMember(A_LAST_ETAG);
    }
    
    /**
     * Constructor
     * 
     * @param name Module name
     * @param description A description of the modules functions
     */
    public HTTPMidFetchUnchangedFilter(String name, String description) {
        super(name, description);
    }

    protected boolean innerAccepts(Object o) {
        // Return FALSE when the document has NOT changed!
        // Return TRUE if the document has changed or we can't tell
        if(o instanceof CrawlURI == false){
            // Only handles CrawlURIs
            if (logger.isLoggable(Level.INFO)) {
                logger.info("Error: Object passed for evaluation was not a " +
                    "CrawlURI. " + o.toString());
            }
            return true;
        }
        
        CrawlURI curi = (CrawlURI)o;
        
        if (curi.isHttpTransaction() == false) {
            // Only handles HTTP
            if (logger.isLoggable(Level.INFO)) {
                logger.info("Error: Non HTTP CrawlURI was passed for evalution. "
                    + curi.toString());
            }
            return true;
        }
        
        if(curi.containsKey(A_HTTP_TRANSACTION) == false){
            // Missing header info, can't do anything.
            if (logger.isLoggable(Level.INFO)) {
                logger.info("Error: Missing HttpMethod object in CrawlURI. "
                        + curi.toString());
            }
            return true;
        }
        
        // Intially assume header info is missing
        int datestamp = HEADER_PREDICTS_MISSING;
        int etag = HEADER_PREDICTS_MISSING;
        HttpMethod method = (HttpMethod)curi.getObject(A_HTTP_TRANSACTION);

        // Compare datestamps (last-modified)
        String newDatestamp = null;
        if (method.getResponseHeader("last-modified") != null) {
            newDatestamp = method.getResponseHeader("last-modified").getValue();
        }
        
        if (newDatestamp != null && newDatestamp.length() > 0) {
            datestamp = HEADER_PREDICTS_CHANGED; // Not missing, assume change
            if (curi.containsKey(A_LAST_DATESTAMP)) {
                if (newDatestamp.equals(curi.getString(A_LAST_DATESTAMP))) {
                    // Both new and old are present and equal, datestamp
                    // predicts no change
                    datestamp = HEADER_PREDICTS_UNCHANGED;
                }
            }
            curi.putString(A_LAST_DATESTAMP, newDatestamp);
        }
        
        // Compare ETags
        String newETag = null;
        if(method.getResponseHeader("last-etag") != null){
            newETag = method.getResponseHeader("last-etag").getValue();
        }
        
        if(newETag != null && newETag.length() > 0){
            etag = HEADER_PREDICTS_CHANGED; // Not missing, assume change
            if(curi.containsKey(A_LAST_ETAG)){
                if(newETag.equals(curi.getString(A_LAST_ETAG))){
                    // Both new and old are present and equal, etag 
                    // predicts no change
                    etag = HEADER_PREDICTS_UNCHANGED;
                }
            }
            curi.putString(A_LAST_ETAG, newETag);
        }
        
        // If both are present, predict no change only if both agree
        if (datestamp == HEADER_PREDICTS_UNCHANGED
                && etag == HEADER_PREDICTS_UNCHANGED) {
            // Have both and they agree, no change
            curi.putInt(A_CONTENT_STATE_KEY, CONTENT_UNCHANGED);
            return false;
        }
        // If one or the other is missing, trust the one that is present
        if (datestamp == HEADER_PREDICTS_MISSING
                && etag == HEADER_PREDICTS_UNCHANGED) {
            // Only have etag, and it predicts no change
            curi.putInt(A_CONTENT_STATE_KEY, CONTENT_UNCHANGED);
            return false;
        }
        if (datestamp == HEADER_PREDICTS_UNCHANGED
                && etag == HEADER_PREDICTS_MISSING) {
            // Only have last-modified, and it predicts no change
            curi.putInt(A_CONTENT_STATE_KEY, CONTENT_UNCHANGED);
            return false;
        }
        return true; // Default, assume change. 
    }
}
