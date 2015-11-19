/* DomainSensitiveFrontier
*
* $Id: DomainSensitiveFrontier.java 4656 2006-09-25 21:34:50Z paul_jack $
*
* Created on 2004-may-06
*
* Copyright (C) 2004 Royal Library of Sweden.
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
package org.archive.crawler.frontier;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.filter.URIRegExpFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.prefetch.QuotaEnforcer;
import org.archive.crawler.scope.ClassicScope;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/** 
 * Behaves like {@link BdbFrontier} (i.e., a basic mostly breadth-first
 * frontier), but with the addition that you can set the number of documents
 * to download on a per site basis. 
 *
 * Useful for case of frequent revisits of a site of frequent changes.
 * 
 * <p>Choose the number of docs you want to download and specify
 * the count in <code>max-docs</code>.  If <code>count-per-host</code> is
 * true, the default, then the crawler will download <code>max-docs</code> 
 * per host.  If you create an override,  the overridden <code>max-docs</code>
 * count will be downloaded instead, whether it is higher or lower.
 * <p>If <code>count-per-host</code> is false, then <code>max-docs</code>
 * acts like the the crawl order <code>max-docs</code> and the crawler will
 * download this total amount of docs only.  Overrides will  
 * download <code>max-docs</code> total in the overridden domain. 
 *
 * @author Oskar Grenholm <oskar dot grenholm at kb dot se>
 * @deprecated As of release 1.10.0.  Replaced by {@link BdbFrontier} and
 * {@link QuotaEnforcer}.
 */
public class DomainSensitiveFrontier extends BdbFrontier
implements CrawlURIDispositionListener {

    private static final long serialVersionUID = -3330190056282726202L;

    private static final Logger logger =
        Logger.getLogger(DomainSensitiveFrontier.class.getName());
    
    public static final String ATTR_MAX_DOCS = "max-docs";
    public static final String ATTR_COUNTER_MODE = "counter-mode";
    public static final String COUNT_OVERRIDE = "count-per-override";
    public static final String COUNT_HOST = "count-per-host";
    public static final String COUNT_DOMAIN = "count-per-domain";
    public static final String[] ATTR_AVAILABLE_MODES = new String[] {
        COUNT_OVERRIDE, COUNT_HOST, COUNT_DOMAIN };      
    public static final String DEFAULT_MODE = COUNT_OVERRIDE;
        
    // TODO: Make this a BigMap.
    private Hashtable<String,Long> hostCounters = new Hashtable<String,Long>();
    private boolean countPerOverride = true;
    private String counterMode;

    public DomainSensitiveFrontier(String name) {
        super(ATTR_NAME, "DomainSensitiveFrontier. *Deprecated* Use " +
        	"BdbFrontier+QuotaEnforcer instead. " +
            "Overrides BdbFrontier to add specification of number of " +
            "documents to download (Expects 'exclude-filter' " +
            "to be part of CrawlScope).");
        Type e = addElementToDefinition(new SimpleType(ATTR_MAX_DOCS,
            "Maximum number of documents to download for host or domain" +
            " (Zero means no limit).", new Long(0)));
        e.setOverrideable(true);
        e = addElementToDefinition(new SimpleType(ATTR_COUNTER_MODE,
               "If " + COUNT_OVERRIDE + ", acts like the crawl " +
               "order maximum download count and the crawler will download " +
               "this total amount of docs only. Override to change the max " +
               "count for the overridden domain or host. " +
               "Else if " + COUNT_HOST + " the crawler will download " +
               ATTR_MAX_DOCS + " per host. Add an override to change " +
               "max count on a per-domain or a per-host basis.For " +
               "example, if you set " + ATTR_MAX_DOCS + " to 30 in " +
               "this mode, the crawler will download 30 docs from " +
               "each host in scope. If you  override for kb.se setting " +
               ATTR_MAX_DOCS +
               " to 20, it will instead download only 20 docs from each " +
               "host of kb.se. (It can be a larger as well as a smaller " +
               "value here.). " +
               "Finally " + COUNT_DOMAIN + " behaves similar to " +
               COUNT_HOST +
               ", but instead sets max on a per-domain basis." +
               "Here you can do overrides on the domain-level, but " +
               "not on the host-level. So if you here set " +
               ATTR_MAX_DOCS + 
               " to 30 the crawler will download 30 docs from each " +
               "domain in scope. If you  override for kb.se setting " +
               ATTR_MAX_DOCS + " to 20, it will instead download only " +
               "20 docs in total from the whole kb.se domain. (It can be " +
               "a larger as well as a smaller value here.)", 
               DEFAULT_MODE, ATTR_AVAILABLE_MODES));
         e.setOverrideable(false);         
    }

    public void initialize(CrawlController c)
    throws FatalConfigurationException, IOException {
        super.initialize(c);
        this.controller.addCrawlURIDispositionListener(this);
        try {
            counterMode = ((String)getAttribute(ATTR_COUNTER_MODE));
            if(counterMode.equalsIgnoreCase(COUNT_DOMAIN) ||
                    counterMode.equalsIgnoreCase(COUNT_HOST))
                countPerOverride = false;
            else
                countPerOverride = true;
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Check if the max document download limit for this host or domain has
     * been reached.
     * 
     * If so, delete the rest of the URIs for this host or domain waiting in
     * the queue. Then add an URIRegExpFilter for this host or domain, so
     * we won't get any more URIs from this one later on.
     * @param curi CrawlURI.
     * @return True if discarded queue.
     */
    private synchronized boolean checkDownloadLimits(CrawlURI curi) {
        long thisMaxDocs = 0;
        long thisCounter = 0;
        boolean discarded = false;
        boolean retVal = false;
        if (curi.getUURI().getScheme().equals("dns")) {
            return false;
        }
        try {
            String host = curi.getUURI().getHost();
            CrawlerSettings cs = controller.getSettingsHandler().
                getSettings(host);
            do {
                String scope;
                if(counterMode.equalsIgnoreCase(COUNT_OVERRIDE))
                    scope = cs.getScope() != null ? cs.getScope() : "root";
                else if(counterMode.equalsIgnoreCase(COUNT_HOST))    
                    scope = host;
                else{ //Get domain part of host
                    int i = host.lastIndexOf(".");
                    i = host.lastIndexOf(".", i-1);
                    scope = host.substring(i+1, host.length());
                }
                thisMaxDocs =
                    ((Long) getAttribute(cs, ATTR_MAX_DOCS)).longValue();
                thisCounter = this.hostCounters.get(scope) != null ?
                    ((Long) this.hostCounters.get(scope)).longValue(): 0;
                // Have we hit the max document download limit for this host
                // or domain?
                if ((thisMaxDocs > 0 && thisCounter >= thisMaxDocs)) {
                    logger.fine("Discarding Queue: " + host + " ");
                    curi.addAnnotation("dsfLimit");
                   if (!discarded) {
                        long count = 0;
                        WorkQueue wq = getQueueFor(curi);
                        wq.unpeek();
                        count += wq.deleteMatching(this, ".*");
                        decrementQueuedCount(count);
                        discarded = true;
                        // I tried adding annotation but we're past log time
                        // for Curi so it doesn't work.
                        // curi.addAnnotation("maxDocsForHost");
                    }
                    // Adding an exclude filter for this host or domain
                    OrFilter or = (OrFilter) this.controller.getScope()
                            .getAttribute(ClassicScope.ATTR_EXCLUDE_FILTER);
                    // If we have hit max for root, block everything. Else
                    // just the scope.
                    String filter = scope.equalsIgnoreCase("root") ?
                        ".*" : "^((https?://)?[a-zA-Z0-9\\.]*)" + scope +
                            "($|/.*)";
                    logger.fine("Adding filter: [" + filter + "].");
                    URIRegExpFilter urf =
                        new URIRegExpFilter(curi.toString(), filter);
                    or.addFilter(this.controller.getSettingsHandler().
                        getSettings(null), urf);
                    thisMaxDocs = 0;
                    thisCounter = 0;
                    retVal = true;
                }
            } while ((cs = cs.getParent()) != null && countPerOverride);
        } catch (Exception e) {
            logger.severe("ERROR: checkDownloadLimits(), "
                    + "while processing {" + curi.toString() + "}"
                    + e.getClass()
                    + "message: " + e.getMessage() + ".  Stack trace:");
            e.printStackTrace();
        }
        return retVal;
    }
    
    protected synchronized void incrementHostCounters(CrawlURI curi) {
        if (!curi.getUURI().toString().startsWith("dns:")) {
            try {
                String host = curi.getUURI().getHost();
                CrawlerSettings cs =
                    controller.getSettingsHandler().getSettings(host);
                do {
                    String scope;
                    if(counterMode.equalsIgnoreCase(COUNT_OVERRIDE))
                        scope = cs.getScope() != null? cs.getScope() : "root";
                    else if(counterMode.equalsIgnoreCase(COUNT_HOST))    
                        scope = host;
                    else{ //Get only domain part of host
                        int i = host.lastIndexOf(".");
                        i = host.lastIndexOf(".", i-1);
                        scope = host.substring(i+1, host.length());
                    }
                    long counter = this.hostCounters.get(scope) != null ?
                        ((Long)this.hostCounters.get(scope)).longValue(): 0;
                    this.hostCounters.put(scope, new Long(++counter));
                } while ((cs = cs.getParent()) != null && countPerOverride);
            } catch (Exception e) {
                logger.severe("ERROR: incrementHostCounters() " +
                    e.getMessage());
            }
        }
    }
    
    public void crawledURISuccessful(CrawlURI curi) {
        incrementHostCounters(curi);
        checkDownloadLimits(curi);
    }

    public void crawledURINeedRetry(CrawlURI curi) {
    }

    public void crawledURIDisregard(CrawlURI curi) {
    }

    public void crawledURIFailure(CrawlURI curi) {
    }
}
