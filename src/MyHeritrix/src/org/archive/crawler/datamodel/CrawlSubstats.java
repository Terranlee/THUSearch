/* CrawlSubstats
*
* $Id: CrawlSubstats.java 6534 2009-10-01 02:54:34Z nlevitt $
*
* Created on Nov 4, 2005
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
*/ 
package org.archive.crawler.datamodel;

import java.io.Serializable;

import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.deciderules.recrawl.IdenticalDigestDecideRule;

/**
 * Collector of statistics for a 'subset' of a crawl,
 * such as a server (host:port), host, or frontier group 
 * (eg queue). 
 * 
 * @author gojomo
 */
public class CrawlSubstats implements Serializable, FetchStatusCodes {
    private static final long serialVersionUID = 8624425657056569036L;

    public enum Stage {SCHEDULED, SUCCEEDED, RETRIED, DISREGARDED, FAILED};
    
    public interface HasCrawlSubstats {
        public CrawlSubstats getSubstats();
    }
    
    long totalScheduled;   // anything initially scheduled
                           // (totalScheduled - (fetchSuccesses + fetchFailures)
    long fetchSuccesses;   // anything disposed-success 
                           // (HTTP 2XX response codes, other non-errors)
    long fetchFailures;    // anything disposed-failure
    long fetchDisregards;  // anything disposed-disregard
    long fetchResponses;   // all positive responses (incl. 3XX, 4XX, 5XX)
    long robotsDenials;    // all robots-precluded failures
    long successBytes;     // total size of all success responses
    long totalBytes;       // total size of all responses
    long fetchNonResponses; // processing attempts resulting in no response
                           // (both failures and temp deferrals)
    long novelBytes;
    long novelUrls;
    long notModifiedBytes;
    long notModifiedUrls;
    long dupByHashBytes;
    long dupByHashUrls;
    
    /**
     * Examing the CrawlURI and based on its status and internal values,
     * update tallies. 
     * 
     * @param curi
     */
    public synchronized void tally(CrawlURI curi, Stage stage) {
        switch(stage) {
            case SCHEDULED:
                totalScheduled++;
                break;
            case RETRIED:
                if(curi.getFetchStatus()<=0) {
                    fetchNonResponses++;
                }
                break;
            case SUCCEEDED:
                fetchSuccesses++;
                fetchResponses++;
                totalBytes += curi.getContentSize();
                successBytes += curi.getContentSize();
                
                if (curi.getFetchStatus() == HttpStatus.SC_NOT_MODIFIED) {
                    notModifiedBytes += curi.getContentSize();
                    notModifiedUrls++;
                } else if (IdenticalDigestDecideRule.hasIdenticalDigest(curi)) {
                    dupByHashBytes += curi.getContentSize();
                    dupByHashUrls++;
                } else {
                    novelBytes += curi.getContentSize();
                    novelUrls++;
                }
                
                break;
            case DISREGARDED:
                fetchDisregards++;
                if(curi.getFetchStatus()==S_ROBOTS_PRECLUDED) {
                    robotsDenials++;
                }
                break;
            case FAILED:
                if(curi.getFetchStatus()<=0) {
                    fetchNonResponses++;
                } else {
                    fetchResponses++;
                    totalBytes += curi.getContentSize();
                    if (curi.getFetchStatus() == HttpStatus.SC_NOT_MODIFIED) {
                        notModifiedBytes += curi.getContentSize();
                        notModifiedUrls++;
                    } else if (IdenticalDigestDecideRule.hasIdenticalDigest(curi)) {
                        dupByHashBytes += curi.getContentSize();
                        dupByHashUrls++;
                    } else {
                        novelBytes += curi.getContentSize();
                        novelUrls++;
                    }
                }
                fetchFailures++;
                break;
        }
    }
    
    public long getFetchSuccesses() {
        return fetchSuccesses;
    }
    public long getFetchResponses() {
        return fetchResponses;
    }
    public long getSuccessBytes() {
        return successBytes;
    }
    public long getTotalBytes() {
        return totalBytes;
    }
    public long getFetchNonResponses() {
        return fetchNonResponses;
    }
    public long getTotalScheduled() {
        return totalScheduled;
    }
    public long getFetchDisregards() {
        return fetchDisregards;
    }
    public long getRobotsDenials() {
        return robotsDenials;
    }
    
    public long getRemaining() {
        return totalScheduled - (fetchSuccesses + fetchFailures + fetchDisregards);
    }

    public long getRecordedFinishes() {
        return fetchSuccesses + fetchFailures;
    }

    public long getNovelBytes() {
        return novelBytes;
    }

    public long getNovelUrls() {
        return novelUrls;
    }

    public long getNotModifiedBytes() {
        return notModifiedBytes;
    }

    public long getNotModifiedUrls() {
        return notModifiedUrls;
    }

    public long getDupByHashBytes() {
        return dupByHashBytes;
    }

    public long getDupByHashUrls() {
        return dupByHashUrls;
    }
}
