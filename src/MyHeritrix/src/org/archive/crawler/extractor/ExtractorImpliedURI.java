/*
 * ExtractorURI
 *
 * $Id: ExtractorImpliedURI.java 4943 2007-02-27 02:54:54Z ia_igor $
 *
 * Created on July 20, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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

package org.archive.crawler.extractor;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.TextUtils;

/**
 * An extractor for finding 'implied' URIs inside other URIs.  If the 
 * 'trigger' regex is matched, a new URI will be constructed from the
 * 'build' replacement pattern. 
 * 
 * Unlike most other extractors, this works on URIs discovered by 
 * previous extractors. Thus it should appear near the end of any 
 * set of extractors.
 *
 * Initially, only finds absolute HTTP(S) URIs in query-string or its 
 * parameters.
 *
 * TODO: extend to find URIs in path-info
 *
 * @author Gordon Mohr
 *
 **/

public class ExtractorImpliedURI extends Extractor implements CoreAttributeConstants {

    private static final long serialVersionUID = 8579045413127769497L;

    private static Logger LOGGER =
        Logger.getLogger(ExtractorImpliedURI.class.getName());
   
    /** regex which when matched triggers addition of 'implied' URI */
    public static final String ATTR_TRIGGER_REGEXP = "trigger-regexp";
    /** replacement pattern used to build 'implied' URI */
    public static final String ATTR_BUILD_PATTERN = "build-pattern";
    
    /** whether to remove URIs that trigger addition of 'implied' URI;
     * default false 
     */
    public static final String ATTR_REMOVE_TRIGGER_URIS = "remove-trigger-uris";
    
    // FIXME: these counters are not incremented atomically; totals may not
    // be correct
    private long numberOfCURIsHandled = 0;
    private long numberOfLinksExtracted = 0;

    /**
     * Constructor
     * 
     * @param name
     */
    public ExtractorImpliedURI(String name) {
        super(name, "Implied URI Extractor. Finds URIs implied by other " +
                "URIs according to regex/replacement patterns. Should " +
                "appear after most other extractors.");

        addElementToDefinition(
            new SimpleType(ATTR_TRIGGER_REGEXP, 
                    "Triggering regular expression. When a discovered URI " +
                    "matches this pattern, the 'implied' URI will be " +
                    "built. The capturing groups of this expression are " +
                    "available for the build replacement pattern.", ""));
        addElementToDefinition(
                new SimpleType(ATTR_BUILD_PATTERN, 
                    "Replacement pattern to build 'implied' URI, using " +
                    "captured groups of trigger expression.", ""));
        addElementToDefinition(
                new SimpleType(ATTR_REMOVE_TRIGGER_URIS, 
                    "If true, all URIs that match trigger regular expression " +
                    "are removed from the list of extracted URIs. " +
                    "Default is false.", Boolean.FALSE));
    }

    /**
     * Perform usual extraction on a CrawlURI
     * 
     * @param curi Crawl URI to process.
     */
    public void extract(CrawlURI curi) {

        this.numberOfCURIsHandled++;
        // use array copy because discoveriess will add to outlinks
        Collection<Link> links = curi.getOutLinks();
        Link[] sourceLinks = links.toArray(new Link[links.size()]);
        for (Link wref: sourceLinks) {
            String implied = extractImplied(
                    wref.getDestination(),
                    (String)getUncheckedAttribute(curi,ATTR_TRIGGER_REGEXP),
                    (String)getUncheckedAttribute(curi,ATTR_BUILD_PATTERN));
            if (implied!=null) {
                try {
                    curi.createAndAddLink(
                            implied, 
                            Link.SPECULATIVE_MISC,
                            Link.SPECULATIVE_HOP);
                	
                    numberOfLinksExtracted++;
                	
                    final boolean removeTriggerURI = 
                    	((Boolean)getUncheckedAttribute(curi,
                    			ATTR_REMOVE_TRIGGER_URIS)).booleanValue();

                    // remove trigger URI from the outlinks if configured so.
                    if (removeTriggerURI) {
                    	if (curi.getOutLinks().remove(wref)) {
                    		LOGGER.log(Level.FINE, wref.getDestination() + 
                    				" has been removed from " + 
                    				wref.getSource() + " outlinks list.");                    	
                    		numberOfLinksExtracted--;

                    	} else {
                        	LOGGER.log(Level.FINE, "Failed to remove " + 
                        			wref.getDestination() + " from " + 
                        			wref.getSource()+ " outlinks list.");             		
                    	}
                    }
                    
                } catch (URIException e) {
                    LOGGER.log(Level.FINE, "bad URI", e);
                }
            }
        }
    }
    
    /**
     * Utility method for extracting 'implied' URI given a source uri, 
     * trigger pattern, and build pattern. 
     * 
     * @param uri source to check for implied URI
     * @param trigger regex pattern which if matched implies another URI
     * @param build replacement pattern to build the implied URI
     * @return implied URI, or null if none
     */
    protected static String extractImplied(CharSequence uri, String trigger, String build) {
        if(trigger.length()==0) {
            // short-circuit empty-string trigger
            return null; 
        }
        Matcher m = TextUtils.getMatcher(trigger, uri);
        if(m.matches()) {
            String result = m.replaceFirst(build);
            TextUtils.recycleMatcher(m);
            return result; 
        }
        return null; 
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: "+ExtractorImpliedURI.class.getName()+"\n");
        ret.append("  Function:          Extracts links inside other URIs\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
