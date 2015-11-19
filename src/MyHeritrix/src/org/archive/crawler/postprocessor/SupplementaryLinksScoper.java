/* SupplementaryLinksScoper
 * 
 * $Id: SupplementaryLinksScoper.java 4911 2007-02-18 19:55:55Z gojomo $
 *
 * Created on Oct 2, 2003
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
 *
 */
package org.archive.crawler.postprocessor;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecideRuleSequence;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.framework.Scoper;
import org.archive.crawler.settings.MapType;

/**
 * Run CandidateURI links carried in the passed CrawlURI through a filter
 * and 'handle' rejections.
 * Used to do supplementary processing of links after they've been scope
 * processed and ruled 'in-scope' by LinkScoper.  An example of
 * 'supplementary processing' would check that a Link is intended for
 * this host to crawl in a multimachine crawl setting. Configure filters to
 * rule on links.  Default handler writes rejected URLs to disk.  Subclass
 * to handle rejected URLs otherwise.
 * @author stack
 */
public class SupplementaryLinksScoper extends Scoper {

    private static final long serialVersionUID = -775819977752790418L;

    private static Logger LOGGER =
        Logger.getLogger(SupplementaryLinksScoper.class.getName());
    
    public static final String ATTR_LINKS_DECIDE_RULES = "link-rules";

    /**
     * @param name Name of this filter.
     */
    public SupplementaryLinksScoper(String name) {
        super(name, "SupplementaryLinksScoper. Use to do supplementary " +
            "processing of in-scope links.  Will run each link through " +
            "configured filters.  Must be run after LinkScoper and " +
            "before FrontierScheduler. " +
            "Optionally logs rejected links (Enable " +
            ATTR_OVERRIDE_LOGGER_ENABLED + " and set logger level " +
            "at INFO or above).");
        
        addElementToDefinition(
                new DecideRuleSequence(ATTR_LINKS_DECIDE_RULES,
                    "DecideRules which if their final decision on a link is " +
                    "REJECT, cause the link to be ruled out-of-scope, even " +
                    "if it had previously been accepted by the main scope."));
    }

    protected void innerProcess(final CrawlURI curi) {
        // If prerequisites or no links, nothing to be done in here.
        if (curi.hasPrerequisiteUri() || curi.outlinksSize() <= 0) {
            return;
        }
        
        Collection<CandidateURI> inScopeLinks = new HashSet<CandidateURI>();
        for (CandidateURI cauri: curi.getOutCandidates()) {
            if (isInScope(cauri)) {
                inScopeLinks.add(cauri);
            }
        }
        // Replace current links collection w/ inscopeLinks.  May be
        // an empty collection.
        curi.replaceOutlinks(inScopeLinks);
    }
    
    protected boolean isInScope(CandidateURI caUri) {
        // TODO: Fix filters so work on CandidateURI.
        CrawlURI curi = (caUri instanceof CrawlURI)?
            (CrawlURI)caUri:
            new CrawlURI(caUri.getUURI());
        boolean result = false;
        if (rulesAccept(getLinkRules(curi), curi)) {
            result = true;
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Accepted: " + caUri);
            }
        } else {
            outOfScope(caUri);
        }
        return result;
    }
    
    protected DecideRule getLinkRules(Object o) {
        try {
            return (DecideRule)getAttribute(o, ATTR_LINKS_DECIDE_RULES);
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Called when a CandidateUri is ruled out of scope.
     * @param caUri CandidateURI that is out of scope.
     */
    protected void outOfScope(CandidateURI caUri) {
        if (!LOGGER.isLoggable(Level.INFO)) {
            return;
        }
        LOGGER.info(caUri.getUURI().toString());
    }
}