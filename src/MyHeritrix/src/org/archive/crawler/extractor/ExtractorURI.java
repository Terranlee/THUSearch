/*
 * ExtractorURI
 *
 * $Id: ExtractorURI.java 4671 2006-09-26 23:47:15Z paul_jack $
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.net.LaxURLCodec;
import org.archive.net.UURI;
import org.archive.util.TextUtils;

/**
 * An extractor for finding URIs inside other URIs. Unlike most other
 * extractors, this works on URIs discovered by previous extractors. Thus 
 * it should appear near the end of any set of extractors.
 *
 * Initially, only finds absolute HTTP(S) URIs in query-string or its 
 * parameters.
 *
 * TODO: extend to find URIs in path-info
 *
 * @author Gordon Mohr
 *
 **/

public class ExtractorURI extends Extractor implements CoreAttributeConstants {

    private static final long serialVersionUID = -6273897743240970822L;

    private static Logger LOGGER =
        Logger.getLogger(ExtractorURI.class.getName());

    static final String ABS_HTTP_URI_PATTERN = "^https?://[^\\s<>]*$";
    
    // FIXME: these counters are not incremented atomically; totals may not
    // be correct
    private long numberOfCURIsHandled = 0;
    private long numberOfLinksExtracted = 0;

    /**
     * Constructor
     * 
     * @param name
     */
    public ExtractorURI(String name) {
        super(name, "URI Extractor. Extracts links inside other " +
                "discovered URIs. Should appear last among extractors.");
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
            extractLink(curi,wref);
        }
    }

    /**
     * Consider a single Link for internal URIs
     * 
     * @param curi CrawlURI to add discoveries to 
     * @param wref Link to examine for internal URIs
     */
    protected void extractLink(CrawlURI curi, Link wref) {
        UURI source = UURI.from(wref.getDestination());
        if(source == null) {
            // shouldn't happen
            return; 
        }
        List<String> found = extractQueryStringLinks(source);
        for (String uri : found) {
            try {
                curi.createAndAddLink(
                        uri, 
                        Link.SPECULATIVE_MISC,
                        Link.SPECULATIVE_HOP);
                numberOfLinksExtracted++;
            } catch (URIException e) {
                LOGGER.log(Level.FINE, "bad URI", e);
            }
        }
        // TODO: consider path URIs too
        
    }

    /**
     * Look for URIs inside the supplied UURI.
     * 
     * Static for ease of testing or outside use. 
     * 
     * @param source UURI to example
     * @return List of discovered String URIs.
     */
    protected static List<String> extractQueryStringLinks(UURI source) {
        List<String> results = new ArrayList<String>(); 
        String decodedQuery;
        try {
            decodedQuery = source.getQuery();
        } catch (URIException e1) {
            // shouldn't happen
            return results;
        }
        if(decodedQuery==null) {
            return results;
        }
        // check if full query-string appears to be http(s) URI
        Matcher m = TextUtils.getMatcher(ABS_HTTP_URI_PATTERN,decodedQuery);
        if(m.matches()) {
            TextUtils.recycleMatcher(m);
            results.add(decodedQuery);
        }
        // split into params, see if any param value is http(s) URI
        String rawQuery = new String(source.getRawQuery());
        String[] params = rawQuery.split("&");
        for (String param : params) {
            String[] keyVal = param.split("=");
            if(keyVal.length==2) {
                String candidate;
                try {
                    candidate = LaxURLCodec.DEFAULT.decode(keyVal[1]);
                } catch (DecoderException e) {
                    continue;
                }
                // TODO: use other non-UTF8 codecs when appropriate
                m.reset(candidate);
                if(m.matches()) {
                    results.add(candidate);
                }
            }
        }
        return results;
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: "+ExtractorURI.class.getName()+"\n");
        ret.append("  Function:          Extracts links inside other URIs\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
