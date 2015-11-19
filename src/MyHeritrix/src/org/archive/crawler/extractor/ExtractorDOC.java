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
 * Created on Jul 7, 2003
 *
 */
package org.archive.crawler.extractor;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.io.ReplayInputStream;
import org.archive.io.SeekReader;
import org.archive.io.SeekReaderCharSequence;
import org.archive.util.ms.Doc;

/**
 *  This class allows the caller to extract href style links from word97-format word documents.
 *
 * @author Parker Thompson
 *
 */
public class ExtractorDOC extends Extractor implements CoreAttributeConstants {

    private static final long serialVersionUID = 1896822554981116303L;
    
    private static Pattern PATTERN = Pattern.compile("HYPERLINK.*?\"(.*?)\"");

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.extractor.ExtractorDOC");
    private long numberOfCURIsHandled = 0;
    private long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorDOC(String name) {
        super(name, "MS-Word document Extractor. Extracts links from MS-Word" +
                " '.doc' documents.");
    }

    /**
     *  Processes a word document and extracts any hyperlinks from it.
     *  This only extracts href style links, and does not examine the actual
     *  text for valid URIs.
     * @param curi CrawlURI to process.
     */
    protected void extract(CrawlURI curi){
        // Assumes docs will be coming in through http.
        // TODO make this more general (currently we're only fetching via http
        // so it doesn't matter)
        if (!isHttpTransactionContentToProcess(curi) ||
                !isExpectedMimeType(curi.getContentType(),
                    "application/msword")) {
            return;
        }

        int links = 0;
        ReplayInputStream documentStream = null;
        SeekReader docReader = null;
        
        numberOfCURIsHandled++;

        // Get the doc as a repositionable reader
        try
        {
            documentStream = curi.getHttpRecorder().getRecordedInput().
                getContentReplayInputStream();

            if (documentStream==null) {
                // TODO: note problem
                return;
            }
            
            docReader = Doc.getText(documentStream);
        }catch(Exception e){
            curi.addLocalizedError(getName(),e,"ExtractorDOC Exception");
            return;
        } finally {
            try {
                documentStream.close();
            } catch (IOException ignored) {

            }
        }

        CharSequence cs = new SeekReaderCharSequence(docReader, 0);
        Matcher m = PATTERN.matcher(cs);
        while (m.find()) {
            links++;
            addLink(curi, m.group(1));
        }
        
        curi.linkExtractorFinished(); // Set flag to indicate that link extraction is completed.
        logger.fine(curi + " has " + links + " links.");
    }
    
    
    private void addLink(CrawlURI curi, String hyperlink) {
        try {
            curi.createAndAddLink(hyperlink,Link.NAVLINK_MISC,Link.NAVLINK_HOP);
        } catch (URIException e1) {
            getController().logUriError(e1, curi.getUURI(), hyperlink);
            if (getController() != null) {
                // Controller can be null: e.g. when running
                // ExtractorTool.
                getController().logUriError(e1, curi.getUURI(), hyperlink);
            } else {
                logger.info(curi + ", " + hyperlink + ": "
                        + e1.getMessage());
            }
        }
        numberOfLinksExtracted++;        
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorDOC\n");
        ret.append("  Function:          Link extraction on MS Word documents (.doc)\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
