/* SeedRecord
 * 
 * $Id: SeedRecord.java 6600 2009-10-16 01:31:38Z gojomo $
 *
 * Created on June 12, 2005
 * 
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.admin;

import java.io.Serializable;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;

/**
 * Record of all interesting info about the most-recent
 * processing of a specific seed.
 * 
 * @author gojomo
 */
public class SeedRecord implements CoreAttributeConstants, Serializable {
    private static final long serialVersionUID = -8455358640509744478L;
    private static Logger logger =
        Logger.getLogger(SeedRecord.class.getName());
    private final String uri;
    private int statusCode;
    private String disposition;
    private String redirectUri;
    
    /**
     * Create a record from the given CrawlURI and disposition string
     * 
     * @param curi CrawlURI, already processed as reported to StatisticsTracker
     * @param disposition descriptive disposition string
     * 
     */
    public SeedRecord(CrawlURI curi, String disposition) {
        super();
        this.uri = curi.toString();
        this.statusCode = curi.getFetchStatus();
        this.disposition = disposition;
        if (statusCode==301 || statusCode == 302) {
            for (CandidateURI cauri: curi.getOutCandidates()) {
                if("location:".equalsIgnoreCase(cauri.getViaContext().
                		toString())) {
                    redirectUri = cauri.toString();
                }
            }
        }
    }
    
    /**
     * Constructor for when a CrawlURI is unavailable; such
     * as when considering seeds not yet passed through as
     * CrawlURIs. 
     * 
     * @param uri
     * @param disposition
     */
    public SeedRecord(String uri, String disposition) {
    	this(uri, disposition, -1, null);
    }

    /**
     * Create a record from the given URI, disposition, HTTP status code,
     * and redirect URI.
     * @param uri
     * @param disposition
     * @param statusCode
     * @param redirectUri
     */
    public SeedRecord(String uri, String disposition, int statusCode,
    		String redirectUri) {
        super();
        this.uri = uri;
        this.statusCode = statusCode;
        this.disposition = disposition;
        this.redirectUri = redirectUri;        
    }

    
    /**
     * A later/repeat report of the same seed has arrived; update with
     * latest. Should be rare/never?
     * 
     * @param curi
     */
    public void updateWith(CrawlURI curi,String disposition) {
        if(!this.uri.equals(curi.toString())) {
            logger.warning("SeedRecord URI changed: "+uri+"->"+curi.toString());
        }
        this.statusCode = curi.getFetchStatus();
        this.disposition = disposition;
        if (statusCode==301 || statusCode == 302) {
            for (CandidateURI cauri: curi.getOutCandidates()) {
                if("location:".equalsIgnoreCase(cauri.getViaContext().
                        toString())) {
                    redirectUri = cauri.toString();
                }
            }
        } else {
            redirectUri = null; 
        }
    }
    
    /**
     * @return Returns the disposition.
     */
    public String getDisposition() {
        return disposition;
    }
    /**
     * @return Returns the redirectUri.
     */
    public String getRedirectUri() {
        return redirectUri;
    }
    /**
     * @return Returns the statusCode.
     */
    public int getStatusCode() {
        return statusCode;
    }
    /**
     * @return Returns the uri.
     */
    public String getUri() {
        return uri;
    }
}