/*
 * ExtractorXML
 *
 * $Id: ExtractorXML.java 4653 2006-09-25 18:58:50Z paul_jack $
 *
 * Created on Sep 27, 2005
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

package org.archive.crawler.extractor;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.io.ReplayCharSequence;
import org.archive.util.TextUtils;

/**
 * A simple extractor which finds HTTP URIs inside XML/RSS files,
 * inside attribute values and simple elements (those with only
 * whitespace + HTTP URI + whitespace as contents)
 *
 * @author gojomo
 *
 **/

public class ExtractorXML extends Extractor implements CoreAttributeConstants {

    private static final long serialVersionUID = 3101230586822401584L;

    private static Logger logger =
        Logger.getLogger(ExtractorXML.class.getName());

    private static String ESCAPED_AMP = "&amp";

    static final String XML_URI_EXTRACTOR =    
    "(?i)[\"\'>]\\s*(http:[^\\s\"\'<>]+)\\s*[\"\'<]"; 
    // GROUPS:
    // (G1) URI
    
    private long numberOfCURIsHandled = 0;
    private long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorXML(String name) {
        super(name, "XML Extractor. Extracts links from XML/RSS.");
    }

    /**
     * @param curi Crawl URI to process.
     */
    public void extract(CrawlURI curi) {
        if (!isHttpTransactionContentToProcess(curi)) {
            return;
        }
        String mimeType = curi.getContentType();
        if (mimeType == null) {
            return;
        }
        if ((mimeType.toLowerCase().indexOf("xml") < 0) 
                && (!curi.toString().toLowerCase().endsWith(".rss"))
                && (!curi.toString().toLowerCase().endsWith(".xml"))) {
            return;
        }
        this.numberOfCURIsHandled++;

        ReplayCharSequence cs = null;
        try {
            cs = curi.getHttpRecorder().getReplayCharSequence();
        } catch (IOException e) {
            logger.severe("Failed getting ReplayCharSequence: " + e.getMessage());
        }
        if (cs == null) {
            logger.severe("Failed getting ReplayCharSequence: " +
                curi.toString());
            return;
        }
        try {
            this.numberOfLinksExtracted += processXml(curi, cs,
                getController());
            // Set flag to indicate that link extraction is completed.
            curi.linkExtractorFinished();
        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (IOException ioe) {
                    logger.warning(TextUtils.exceptionToString(
                            "Failed close of ReplayCharSequence.", ioe));
                }
            }
        }
    }

    public static long processXml(CrawlURI curi, CharSequence cs,
            CrawlController controller) {
        long foundLinks = 0;
        Matcher uris = null;
        String xmlUri;
        uris = TextUtils.getMatcher(XML_URI_EXTRACTOR, cs);
        while (uris.find()) {
            xmlUri = uris.group(1);
            // TODO: Escape more HTML Entities.
            xmlUri = TextUtils.replaceAll(ESCAPED_AMP, xmlUri, "&");
            foundLinks++;
            try {
                // treat as speculative, as whether context really 
                // intends to create a followable/fetchable URI is
                // unknown
                curi.createAndAddLink(xmlUri,Link.SPECULATIVE_MISC,
                        Link.SPECULATIVE_HOP);
            } catch (URIException e) {
                // There may not be a controller (e.g. If we're being run
                // by the extractor tool).
                if (controller != null) {
                    controller.logUriError(e, curi.getUURI(), xmlUri);
                } else {
                    logger.info(curi + ", " + xmlUri + ": " +
                        e.getMessage());
                }
            }
        }
        TextUtils.recycleMatcher(uris);
        return foundLinks;
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorXML\n");
        ret.append("  Function:          Link extraction on XML/RSS\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
