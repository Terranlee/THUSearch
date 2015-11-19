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
 * SimpleHTTPExtractor.java
 * Created on Jul 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.extractor;

import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * Extracts URIs from HTTP response headers.
 * @author gojomo
 */
public class ExtractorHTTP extends Processor
implements CoreAttributeConstants {

    private static final long serialVersionUID = 8499072198570554647L;

    private static final Logger LOGGER =
        Logger.getLogger(ExtractorHTTP.class.getName());
    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;

    public ExtractorHTTP(String name) {
        super(name,
            "HTTP extractor. Extracts URIs from HTTP response headers.");
    }

    public void innerProcess(CrawlURI curi) {
        if (!curi.isHttpTransaction() || curi.getFetchStatus() <= 0) {
            // If not http or if an error status code, skip.
            return;
        }
        numberOfCURIsHandled++;
        HttpMethod method = (HttpMethod)curi.getObject(A_HTTP_TRANSACTION);
        addHeaderLink(curi, method.getResponseHeader("Location"));
        addHeaderLink(curi, method.getResponseHeader("Content-Location"));
    }

    protected void addHeaderLink(CrawlURI curi, Header loc) {
        if (loc == null) {
            // If null, return without adding anything.
            return;
        }
        // TODO: consider possibility of multiple headers
        try {
            curi.createAndAddLink(loc.getValue(), loc.getName() + ":",
                Link.REFER_HOP);
            numberOfLinksExtracted++;
        } catch (URIException e) {
            // There may not be a controller (e.g. If we're being run
            // by the extractor tool).
            if (getController() != null) {
                getController().logUriError(e, curi.getUURI(), loc.getValue());
            } else {
                LOGGER.info(curi + ", " + loc.getValue() + ": " +
                    e.getMessage());
            }
        }

    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorHTTP\n");
        ret.append("  Function:          " +
            "Extracts URIs from HTTP response headers\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");
        return ret.toString();
    }
}
