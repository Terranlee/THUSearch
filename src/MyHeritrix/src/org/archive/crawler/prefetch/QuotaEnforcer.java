/* QuotaEnforcer
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
package org.archive.crawler.prefetch;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlSubstats;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;

/**
 * A simple quota enforcer. If the host, server, or frontier group
 * associated with the current CrawlURI is already over its quotas, 
 * blocks the current URI's processing with S_BLOCKED_BY_QUOTA.
 * 
 * @author gojomo
 * @version $Date: 2007-04-06 00:40:50 +0000 (Fri, 06 Apr 2007) $, $Revision: 5040 $
 */
public class QuotaEnforcer extends Processor implements FetchStatusCodes {

    private static final long serialVersionUID = 6091720623469404595L;

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    
    // indexed table of reused string categorical names/keys
    protected static final int SERVER = 0;
    protected static final int HOST = 1;
    protected static final int GROUP = 2;
    protected static final int NAME = 0;
    protected static final int SUCCESSES = 1;
    protected static final int SUCCESS_KB = 2;
    protected static final int RESPONSES = 3;
    protected static final int RESPONSE_KB = 4;
    protected static final String[][] keys = new String[][] {
            {
                "server",
                "server-max-fetch-successes",
                "server-max-success-kb",
                "server-max-fetch-responses",
                "server-max-all-kb"
            },
            {
                "host",
                "host-max-fetch-successes",
                "host-max-success-kb",
                "host-max-fetch-responses",
                "host-max-all-kb"
            },
            {
                "group",
                "group-max-fetch-successes",
                "group-max-success-kb",
                "group-max-fetch-responses",
                "group-max-all-kb"
            }
    };
    
   // server quotas
   // successes
   /** server max successful fetches */
   protected static final String ATTR_SERVER_MAX_FETCH_SUCCESSES = 
       keys[SERVER][SUCCESSES];
   protected static final Long DEFAULT_SERVER_MAX_FETCH_SUCCESSES =
       new Long(-1);
   /** server max successful fetch bytes */
   protected static final String ATTR_SERVER_MAX_SUCCESS_KB = 
       keys[SERVER][SUCCESS_KB];;
   protected static final Long DEFAULT_SERVER_MAX_SUCCESS_KB =
       new Long(-1);
   // all-responses
   /** server max fetch responses (including error codes) */
   protected static final String ATTR_SERVER_MAX_FETCH_RESPONSES = 
       keys[SERVER][RESPONSES];
   protected static final Long DEFAULT_SERVER_MAX_FETCH_RESPONSES =
       new Long(-1);
   /** server max all fetch bytes (including error responses) */
   protected static final String ATTR_SERVER_MAX_ALL_KB = 
       keys[SERVER][RESPONSE_KB];
   protected static final Long DEFAULT_SERVER_MAX_ALL_KB =
       new Long(-1);
   
   // host quotas
   // successes
   /** host max successful fetches */
   protected static final String ATTR_HOST_MAX_FETCH_SUCCESSES = 
       keys[HOST][SUCCESSES];;
   protected static final Long DEFAULT_HOST_MAX_FETCH_SUCCESSES =
       new Long(-1);
   /** host max successful fetch bytes */
   protected static final String ATTR_HOST_MAX_SUCCESS_KB = 
       keys[HOST][SUCCESS_KB];;
   protected static final Long DEFAULT_HOST_MAX_SUCCESS_KB =
       new Long(-1);
   // all-responses
   /** host max fetch responses (including error codes) */
   protected static final String ATTR_HOST_MAX_FETCH_RESPONSES = 
       keys[HOST][RESPONSES];
   protected static final Long DEFAULT_HOST_MAX_FETCH_RESPONSES =
       new Long(-1);
   /** host max all fetch bytes (including error responses) */
   protected static final String ATTR_HOST_MAX_ALL_KB = 
       keys[HOST][RESPONSE_KB];
   protected static final Long DEFAULT_HOST_MAX_ALL_KB =
       new Long(-1);
   
   // group quotas
   // successes
   /** group max successful fetches */
   protected static final String ATTR_GROUP_MAX_FETCH_SUCCESSES = 
       keys[GROUP][SUCCESSES];
   protected static final Long DEFAULT_GROUP_MAX_FETCH_SUCCESSES =
       new Long(-1);
   /** group max successful fetch bytes */
   protected static final String ATTR_GROUP_MAX_SUCCESS_KB = 
       keys[GROUP][SUCCESS_KB];
   protected static final Long DEFAULT_GROUP_MAX_SUCCESS_KB =
       new Long(-1);
   // all-responses
   /** group max fetch responses (including error codes) */
   protected static final String ATTR_GROUP_MAX_FETCH_RESPONSES = 
       keys[GROUP][RESPONSES];
   protected static final Long DEFAULT_GROUP_MAX_FETCH_RESPONSES =
       new Long(-1);
   /** group max all fetch bytes (including error responses) */
   protected static final String ATTR_GROUP_MAX_ALL_KB = 
       keys[GROUP][RESPONSE_KB];
   protected static final Long DEFAULT_GROUP_MAX_ALL_KB =
       new Long(-1);
   
   /** whether to force-retire when over-quote detected */
   protected static final String ATTR_FORCE_RETIRE = 
       "force-retire";
   protected static final Boolean DEFAULT_FORCE_RETIRE = true;
   
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public QuotaEnforcer(String name) {
        super(name, "QuotaEnforcer.");
        
        addElementToDefinition(new SimpleType(ATTR_FORCE_RETIRE,
                "Whether an over-quota situation should result in the " +
                "containing queue being force-retired (if the Frontier " +
                "supports this). Note that if your queues combine URIs " +
                "that are different with regard to the quota category, " +
                "the retirement may hold back URIs not in the same " +
                "quota category. " +
                "Default is false.",
                DEFAULT_FORCE_RETIRE)); 
        
        String maxFetchSuccessesDesc = "Maximum number of fetch successes " +
            "(e.g. 200 responses) to collect from one CATEGORY. " +
            "Default is -1, meaning no limit.";
        String maxSuccessKbDesc = "Maximum amount of fetch success content " +
            "(e.g. 200 responses) in KB to collect from one CATEGORY. " +
            "Default is -1, meaning no limit.";
        String maxFetchResponsesDesc = "Maximum number of fetch responses " +
            "(incl. error responses) to collect from one CATEGORY. " +
            "Default is -1, meaning no limit.";
        String maxAllKbDesc = "Maximum amount of response content " +
            "(incl. error responses) in KB to collect from one CATEGORY. " +
            "Default is -1, meaning no limit.";
        // server successes
        addElementToDefinition(new SimpleType(ATTR_SERVER_MAX_FETCH_SUCCESSES,
            maxFetchSuccessesDesc.replaceAll("CATEGORY","server"),
            DEFAULT_SERVER_MAX_FETCH_SUCCESSES));
        addElementToDefinition(new SimpleType(ATTR_SERVER_MAX_SUCCESS_KB,
            maxSuccessKbDesc.replaceAll("CATEGORY","server"),
            DEFAULT_SERVER_MAX_SUCCESS_KB));
        // server all-responses
        addElementToDefinition(new SimpleType(ATTR_SERVER_MAX_FETCH_RESPONSES,
            maxFetchResponsesDesc.replaceAll("CATEGORY","server"),
            DEFAULT_SERVER_MAX_FETCH_RESPONSES));
        addElementToDefinition(new SimpleType(ATTR_SERVER_MAX_ALL_KB,
            maxAllKbDesc.replaceAll("CATEGORY","server"),
            DEFAULT_SERVER_MAX_ALL_KB));
        // host successes
        addElementToDefinition(new SimpleType(ATTR_HOST_MAX_FETCH_SUCCESSES,
            maxFetchSuccessesDesc.replaceAll("CATEGORY","host"),
            DEFAULT_HOST_MAX_FETCH_SUCCESSES));
        addElementToDefinition(new SimpleType(ATTR_HOST_MAX_SUCCESS_KB,
            maxSuccessKbDesc.replaceAll("CATEGORY","host"),
            DEFAULT_HOST_MAX_SUCCESS_KB));
        // host all-responses
        addElementToDefinition(new SimpleType(ATTR_HOST_MAX_FETCH_RESPONSES,
            maxFetchResponsesDesc.replaceAll("CATEGORY","host"),
            DEFAULT_HOST_MAX_FETCH_RESPONSES));
        addElementToDefinition(new SimpleType(ATTR_HOST_MAX_ALL_KB,
            maxAllKbDesc.replaceAll("CATEGORY","host"),
            DEFAULT_HOST_MAX_ALL_KB));        
        // group successes
        addElementToDefinition(new SimpleType(ATTR_GROUP_MAX_FETCH_SUCCESSES,
            maxFetchSuccessesDesc.replaceAll("CATEGORY","group (queue)"),
            DEFAULT_GROUP_MAX_FETCH_SUCCESSES));
        addElementToDefinition(new SimpleType(ATTR_GROUP_MAX_SUCCESS_KB,
            maxSuccessKbDesc.replaceAll("CATEGORY","group (queue)"),
            DEFAULT_GROUP_MAX_SUCCESS_KB));
        // group all-responses
        addElementToDefinition(new SimpleType(ATTR_GROUP_MAX_FETCH_RESPONSES,
            maxFetchResponsesDesc.replaceAll("CATEGORY","group (queue)"),
            DEFAULT_GROUP_MAX_FETCH_RESPONSES));
        addElementToDefinition(new SimpleType(ATTR_GROUP_MAX_ALL_KB,
            maxAllKbDesc.replaceAll("CATEGORY","group (queue)"),
            DEFAULT_GROUP_MAX_ALL_KB));  
       
    }
    
    protected void innerProcess(CrawlURI curi) {
        CrawlSubstats.HasCrawlSubstats[] haveStats = 
            new CrawlSubstats.HasCrawlSubstats[] {
                getController().getServerCache().getServerFor(curi), // server
                getController().getServerCache().getHostFor(curi), // host
                getController().getFrontier().getGroup(curi) // group
            };
        
        for(int cat = SERVER; cat <= GROUP; cat++) {
            if (checkQuotas(curi, haveStats[cat], cat)) {
                return;
            }
        }
    }

    /**
     * Check all quotas for the given substats and category (server, host, or
     * group). 
     * 
     * @param curi CrawlURI to mark up with results
     * @param hasStats  holds CrawlSubstats with actual values to test
     * @param CAT category index (SERVER, HOST, GROUP) to quota settings keys
     * @return true if quota precludes fetching of CrawlURI
     */
    protected boolean checkQuotas(final CrawlURI curi,
            final CrawlSubstats.HasCrawlSubstats hasStats,
            final int CAT) {
        if (hasStats == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(curi.toString() + " null stats category: " + CAT);
            }
            return false;
        }
        CrawlSubstats substats = hasStats.getSubstats();
        long[] actuals = new long[] {
                -1, // dummy
                substats.getFetchSuccesses(),
                substats.getSuccessBytes()/1024,
                substats.getFetchResponses(),
                substats.getTotalBytes()/1024,
        };
        for(int q = SUCCESSES; q <= RESPONSE_KB; q++) {
            if(applyQuota(curi, keys[CAT][q], actuals[q])) {
                return true; 
            }
        }
        return false; 
    }

    /**
     * Apply the quota specified by the given key against the actual 
     * value provided. If the quota and actual values rule out processing the 
     * given CrawlURI,  mark up the CrawlURI appropriately. 
     * 
     * @param curi CrawlURI whose processing is subject to a potential quota
     * limitation
     * @param quotaKey settings key to get applicable quota
     * @param actual current value to compare to quota 
     * @return true is CrawlURI is blocked by a quota, false otherwise
     */
    protected boolean applyQuota(CrawlURI curi, String quotaKey, long actual) {
        long quota = ((Long)getUncheckedAttribute(curi, quotaKey)).longValue();
        if (quota >= 0 && actual >= quota) {
            curi.setFetchStatus(S_BLOCKED_BY_QUOTA);
            curi.addAnnotation("Q:"+quotaKey);
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            if((Boolean)getUncheckedAttribute(curi,ATTR_FORCE_RETIRE)) {
                curi.putObject(CoreAttributeConstants.A_FORCE_RETIRE, (Boolean) true);
            }
            return true;
        }
        return false; 
    }
}
