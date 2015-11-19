/* ServerCache 
 * 
 * Created on Nov 19, 2004
 *
 * Copyright (C) 2009 Internet Archive.
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
package org.archive.crawler.datamodel;

import java.util.logging.Logger;

import org.apache.commons.collections.Closure;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.util.ObjectIdentityCache;
import org.archive.util.ObjectIdentityMemCache;
import org.archive.util.Supplier;

/**
 * Server and Host cache.
 * @author stack
 * @version $Date: 2009-11-09 23:40:30 +0000 (Mon, 09 Nov 2009) $, $Revision: 6635 $
 */
public class ServerCache {
    private static Logger logger =
        Logger.getLogger(ServerCache.class.getName());
    
    protected SettingsHandler settingsHandler = null;
    
    /**
     * hostname[:port] -> CrawlServer.
     * Set in the initialization.
     */
    protected ObjectIdentityCache<String,CrawlServer> servers = null;
    
    /**
     * hostname -> CrawlHost.
     * Set in the initialization.
     */
    protected ObjectIdentityCache<String,CrawlHost> hosts = null;
    
    /**
     * Constructor.
     * Shutdown access to the default constructor by making it protected.
     */
    protected ServerCache() {
        super();
    }
    
    /**
     * This constructor creates a ServerCache that is all memory-based using
     * Hashtables.  Used for unit testing only
     * (Use {@link #ServerCache(CrawlController)} when crawling).
     * @param sh
     * @throws Exception
     */
    public ServerCache(final SettingsHandler sh)
    throws Exception {
        this.settingsHandler = sh;
        this.servers = new ObjectIdentityMemCache<CrawlServer>();
        this.hosts = new ObjectIdentityMemCache<CrawlHost>();
    }
    
    /**
     * Create a ServerCache that uses the given CrawlController to initialize the
     * maps of servers and hosts.
     * @param c 
     * @throws Exception
     */
    public ServerCache(final CrawlController c)
    throws Exception {
        this.settingsHandler = c.getSettingsHandler();
        this.servers = c.getBigMap("servers", CrawlServer.class);
        this.hosts = c.getBigMap("hosts", CrawlHost.class);
    }
    
    /**
     * Get the {@link CrawlServer} associated with <code>name</code>,
     * creating if necessary. 
     * @param serverKey Server name we're to return server for.
     * @return CrawlServer instance that matches the passed server name.
     */
    public CrawlServer getServerFor(final String serverKey) {
        CrawlServer cserver = servers.getOrUse(
                serverKey,
                new Supplier<CrawlServer>() {
                    public CrawlServer get() {
                        String skey = new String(serverKey); // ensure private minimal key
                        CrawlServer cs = new CrawlServer(skey);
                        cs.setSettingsHandler(settingsHandler);
                        return cs;
                    }});
        return cserver;
    }

    /**
     * Get the {@link CrawlServer} associated with <code>curi</code>.
     * @param cauri CandidateURI we're to get server from.
     * @return CrawlServer instance that matches the passed CandidateURI.
     */
    public CrawlServer getServerFor(CandidateURI cauri) {
        CrawlServer cs = null;
        try {
            String key = CrawlServer.getServerKey(cauri);
            // TODOSOMEDAY: make this robust against those rare cases
            // where authority is not a hostname.
            if (key != null) {
                cs = getServerFor(key);
            }
        } catch (URIException e) {
            logger.severe(e.getMessage() + ": " + cauri);
            e.printStackTrace();
        } catch (NullPointerException npe) {
            logger.severe(npe.getMessage() + ": " + cauri);
            npe.printStackTrace();
        }
        return cs;
    }
    
    /**
     * Get the {@link CrawlHost} associated with <code>name</code>.
     * @param hostname Host name we're to return Host for.
     * @return CrawlHost instance that matches the passed Host name.
     */
    public CrawlHost getHostFor(final String hostname) {
        if (hostname == null || hostname.length() == 0) {
            return null;
        }
        CrawlHost host = hosts.getOrUse(
                hostname,
                new Supplier<CrawlHost>() {
                    public CrawlHost get() {
                        String hkey = new String(hostname); // ensure private minimal key
                        return new CrawlHost(hkey);
                    }});
        return host;
    }
    
    /**
     * Get the {@link CrawlHost} associated with <code>curi</code>.
     * @param cauri CandidateURI we're to return Host for.
     * @return CandidateURI instance that matches the passed Host name.
     */
    public CrawlHost getHostFor(CandidateURI cauri) {
        CrawlHost h = null;
        try {
            String hostKey;
            if (cauri.getUURI().getScheme().equals("dns")) {
                hostKey = "dns:";
            } else {
                hostKey = cauri.getUURI().getReferencedHost();
            }
            h = getHostFor(hostKey);
        } catch (URIException e) {
            e.printStackTrace();
        }
        return h;
    }

    /**
     * @param serverKey Key to use doing lookup.
     * @return True if a server instance exists.
     */
    public boolean containsServer(String serverKey) {
        return (CrawlServer) servers.get(serverKey) != null; 
    }

    /**
     * @param hostKey Key to use doing lookup.
     * @return True if a host instance exists.
     */
    public boolean containsHost(String hostKey) {
        return (CrawlHost) hosts.get(hostKey) != null; 
    }

    /**
     * Called when shutting down the cache so we can do clean up.
     */
    public void cleanup() {
        if (this.hosts != null) {
            // If we're using a bdb bigmap, the call to clear will
            // close down the bdb database.
            this.hosts.close();
            this.hosts = null;
        }
        if (this.servers != null) { 
            this.servers.close();
            this.servers = null;
        }
    }

    public void forAllHostsDo(Closure c) {
        for(String host : hosts.keySet()) {
            c.execute(hosts.get(host));
        }
    }
}
