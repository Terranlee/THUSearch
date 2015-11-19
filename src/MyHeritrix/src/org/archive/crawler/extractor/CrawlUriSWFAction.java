/*
 * CrawlUriSWFAction
 *
 * $Id: CrawlUriSWFAction.java 6513 2009-09-23 18:18:56Z szznax $
 *
 * Created on March 15, 2004
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
 */

package org.archive.crawler.extractor;

import java.io.IOException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;

import com.anotherbigidea.flash.writers.SWFActionsImpl;

/**
 * SWF action that handles discovered URIs.
 *
 * @author Igor Ranitovic
 */
public class CrawlUriSWFAction
extends SWFActionsImpl {
    CrawlURI curi;
    CrawlController controller; // for error reporting
    
    private long linkCount;
    static final String JSSTRING = "javascript:";

    /**
     *
     * @param curi
     */
    public CrawlUriSWFAction(CrawlURI curi, CrawlController controller) {
        assert (curi != null) : "CrawlURI should not be null";
        this.curi = curi;
        this.controller = controller; 
        this.linkCount = 0;
    }

    /**
     * Overwrite handling of discovered URIs.
     *
     * @param url Discovered URL.
     * @param target Discovered target (currently not being used.)
     * @throws IOException
     */
    public void getURL(String url, String target)
    throws IOException {
        // I have done tests on a few tens of swf files and have not seen a need
        // to use 'target.' Most of the time 'target' is not set, or it is set
        // to '_self' or '_blank'.
        if (url.startsWith(JSSTRING)) {
            linkCount += ExtractorJS.considerStrings(curi, url, controller, false);
        } else {
            curi.createAndAddLinkRelativeToVia(url,Link.EMBED_MISC,Link.EMBED_HOP);
            linkCount++;
        }
    }
    
    /**
     * @return Total number of links extracted from a swf file.
     */
    public long getLinkCount() {
        return linkCount;
    }
}
