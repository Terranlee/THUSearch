/* CrawlStateUpdater
 *
 * Created on Jun 5, 2003
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
package org.archive.crawler.postprocessor;


import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;


/**
 * A step, late in the processing of a CrawlURI, for updating the per-host
 * information that may have been affected by the fetch. This will initially
 * be robots and ip address info; it could include other per-host stats that
 * would affect the crawl (like total pages visited at the site) as well.
 *
 * @author gojomo
 * @version $Date: 2010-04-02 01:03:46 +0000 (Fri, 02 Apr 2010) $, $Revision: 6803 $
 */
public class CrawlStateUpdater extends Processor implements
        CoreAttributeConstants, FetchStatusCodes {

    private static final long serialVersionUID = -1072728147960180091L;

    private static final Logger logger =
        Logger.getLogger(CrawlStateUpdater.class.getName());

    public CrawlStateUpdater(String name) {
        super(name, "Crawl state updater");
    }

    protected void innerProcess(CrawlURI curi) {
        CrawlServer server =
            getController().getServerCache().getServerFor(curi);
        
        String scheme = curi.getUURI().getScheme().toLowerCase();
        if (scheme.equals("http") || scheme.equals("https") &&
                server != null) {
            // Update connection problems counter
            if( curi.getFetchStatus() == S_CONNECT_FAILED || curi.getFetchStatus() == S_CONNECT_LOST) {
                server.incrementConsecutiveConnectionErrors();
            } else if (curi.getFetchStatus() > 0){
                server.resetConsecutiveConnectionErrors();
            }

            // Update robots info
            try {
                if ("/robots.txt".equals(curi.getUURI().getPath())) {
                    // Update server with robots info
                    // NOTE, this *can* change the curi's fetchStatus from a connection
                    // problem to S_DEEMED_NOT_FOUND to prevent further retries
                    server.updateRobots(curi);
                }
            }
            catch (URIException e) {
                logger.severe("Failed get path on " + curi.getUURI());
            }
        }
    }
}
