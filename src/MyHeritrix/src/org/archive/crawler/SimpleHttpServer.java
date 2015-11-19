/*
 * SimpleHttpServer
 *
 * $Id: SimpleHttpServer.java 4666 2006-09-26 17:53:28Z paul_jack $
 *
 * Created on Jul 11, 2003
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
package org.archive.crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.mortbay.http.HashUserRealm;
import org.mortbay.http.HttpListener;
import org.mortbay.http.HttpServer;
import org.mortbay.http.NCSARequestLog;
import org.mortbay.http.RequestLog;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.util.InetAddrPort;


/**
 * Wrapper for embedded Jetty server.
 *
 * Loads up all webapps under webapp directory.
 *
 */
public class SimpleHttpServer
{
    private int port;
    private Server server = null;

    /**
     * Default web port.
     */
    public static final int DEFAULT_PORT = 8080;

    /**
     * Webapp contexts returned out of a server start.
     */
    private List<WebApplicationContext> contexts
     = new ArrayList<WebApplicationContext>();

    /**
     * Name of the root webapp.
     */
    private static final String ROOT_WEBAPP = "root";

    /**
     * Name of the admin webapp.
     */
    private static final String ADMIN_WEBAPP = "admin";

    /**
     * List of webapps to deploy.
     */
    private static final List webapps =
        Arrays.asList(new String [] {ROOT_WEBAPP, ADMIN_WEBAPP});


    public SimpleHttpServer() throws Exception {
        this(DEFAULT_PORT, true);
    }

    public SimpleHttpServer(int port, boolean expandWebapps)
    throws Exception {
        this(SimpleHttpServer.webapps, port, expandWebapps);
    }
    
    /**
     * @param name Name of webapp to load.
     * @param context Where to mount the webapp.  If passed context is
     * null or empty string, we'll use '/' + <code>name</code> else if
     * passed '/' then we'll add the webapp as the root webapp.
     * @param port Port to run on.
     * @param expandWebapps True if we're to expand the webapp passed.
     * @throws Exception
     * @deprecated  Use SimpleHttpServer(name,context,hosts,port,expandWebapps)
     */
    public SimpleHttpServer(boolean localhostOnly, String name, String context,
        int port, boolean expandWebapps)
    throws Exception {
        this(name, context, determineHosts(localhostOnly), port, expandWebapps);
    }
    
    
    /**
     * Constructor.
     * 
     * @param name     Name of webapp to load
     * @param context  Where to mount the webap.  If null or empty string,
     *                  we'll use '/' + <code>name</code>; if passed '/'
     *                  then we'll add the webapp as the root webapp
     * @param hosts    list of hosts to bind to
     * @param port     port to listen on
     * @param expandWebapps   true to expand webapp passed
     * @throws Exception
     */
    public SimpleHttpServer(String name, String context,
        Collection<String> hosts, int port, boolean expandWebapps)
    throws Exception {
        initialize(hosts, port);
        addWebapp(name, context, expandWebapps);
        this.server.setRequestLog(getServerLogging());
    }


    /**
     * @param webapps List of webapps to load.
     * @param port Port to run on.
     * @param expandWebapps True if we're to expand the webapps found.
     * @throws Exception
     */
    public SimpleHttpServer(List webapps, int port, boolean expandWebapps)
    throws Exception {
        initialize(null, port);
        
        // Add each of the webapps in turn. If we're passed the root webapp,
        // give it special handling -- assume its meant to be server root and
        // its meant to be mounted on '/'.  The below also favors the war file
        // if its present.
        for (Iterator i = webapps.iterator(); i.hasNext();) {
            addWebapp((String)i.next(), null, expandWebapps);
        }
        this.server.setRequestLog(getServerLogging());
    }
    
    /**
     * Add a webapp.
     * @param name Name of webapp to add.
     * @param context Context to add the webapp on.
     * @param expand True if we should expand the webapps.
     * @throws IOException
     */
    protected void addWebapp(String name, String context, boolean expand)
    throws IOException {
        File ptr = new File(getWARSPath(), name + ".war");
        if (!ptr.exists()) {
            ptr = new File(getWARSPath(), name);
            if (!ptr.exists()) {
                throw new FileNotFoundException(ptr.getAbsolutePath());
            }
        }
        // If webapp name is for root, mount it on '/', else '/WEBAPP_NAME'.
        if (context == null || context.length() <= 0) {
            context = "/" + ((name.equals(ROOT_WEBAPP))? "": name);
        }
        WebApplicationContext c =
            this.server. addWebApplication(context, ptr.getAbsolutePath());
        if (context.equals("/")) {
            // If we've just mounted the root webapp, make it the root.
            this.server.setRootWebApp(name);
        }
        // Selftest depends on finding the extracted WARs. TODO: Fix.
        c.setExtractWAR(expand);
        // let login sessions last 24 hours
        c.getServletHandler().getSessionManager().setMaxInactiveInterval(86400);
        this.contexts.add(c);
    }
    
    /**
     * Initialize the server.
     * Called from constructors.
     * @param port Port to start the server on.
     * @deprecated  Use initialize(Collection<String>, port) instead
     */
    protected void initialize(int port, boolean localhostOnly) {
        Collection<String> hosts = determineHosts(localhostOnly);        
        initialize(hosts, port);
    }
    
    
    /**
     * Initialize the server.  Called from constructors.
     * 
     * @param hosts   the hostnames to bind to; if empty or null, will bind
     *                  to all interfaces
     * @param port    the port to listen on
     */
    protected void initialize(Collection<String> hosts, int port) {
        this.server = new Server();
        this.port = port;
        if (hosts.isEmpty()) {
            SocketListener listener = new SocketListener();
            listener.setPort(port);
            this.server.addListener(listener);
            return;
        }
        
        for (String host: hosts) try {
            InetAddrPort addr = new InetAddrPort(host, port);
            SocketListener listener = new SocketListener(addr);
            this.server.addListener(listener);
        } catch (UnknownHostException e) { 
            e.printStackTrace();
        }
    }
    
    
    private static Collection<String> determineHosts(boolean lho) {
        Collection<String> hosts = new ArrayList<String>();
        if (lho) {
            hosts.add("127.0.0.1");
        }
        return hosts;
    }


    /**
     * Setup log files.
     * @return RequestLog instance to add to a server. 
     * @throws Exception
     */
    protected RequestLog getServerLogging() throws Exception {
        // Have accesses go into the stdout/stderr log for now.  Later, if
        // demand, we'll have accesses go into their own file.
        NCSARequestLog a = new NCSARequestLog(Heritrix.getHeritrixOut());
        a.setRetainDays(90);
        a.setAppend(true);
        a.setExtended(false);
        a.setBuffered(false);
        a.setLogTimeZone("GMT");
        a.start();
        return a;
    }

    /**
     * Return the directory that holds the WARs we're to deploy.
     *
     * @return Return webapp path (Path returned has a trailing '/').
     * @throws IOException
     */
    private static String getWARSPath() throws IOException {
        String webappsPath = Heritrix.getWarsdir().getAbsolutePath();
        if (!webappsPath.endsWith(File.separator))
        {
            webappsPath = webappsPath + File.separator;
        }
        return webappsPath;
    }

    /**
     * Start the server.
     *
     * @throws Exception if problem starting server or if server already
     * started.
     */
    public synchronized void startServer()
        throws Exception {

        this.server.start();
    }

    /**
     * Stop the running server.
     *
     * @throws InterruptedException
     */
    public synchronized void stopServer() throws InterruptedException {

        if (this.server != null)
        {
            this.server.stop();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    protected void finalize()
        throws Throwable {

        stopServer();
        super.finalize();
    }

    /**
     * @return Port server is running on.
     */
    public int getPort() {

        return this.port;
    }

    /**
     * @return Server reference.
     */
    public HttpServer getServer() {

        return this.server;
    }

    /**
     * @param contextName Name of context to look for.  Possible names would be
     * '/admin', '/', or '/selftest'.
     *
     * @return named context.
     */
    private WebApplicationContext getContext(String contextName) {

        WebApplicationContext context = null;

        if (this.contexts == null)
        {
            throw new NullPointerException("No contexts available.");
        }

        if (!contextName.startsWith("/")) {
            contextName = '/' + contextName;
        }
        for (Iterator i = this.contexts.iterator(); i.hasNext();)
        {
            WebApplicationContext c = (WebApplicationContext)i.next();
            if (c.getHttpContextName().equals(contextName))
            {
                context = c;
                break;
            }
        }

        if (context == null)
        {
            throw new NoSuchElementException("Unknown webapp: " + contextName);
        }

        return context;
    }

    /**
     * Setup a realm on the server named for the webapp and add to the
     * passed webapp's context.
     *
     * Used by the selftest to check digest authentication is working.
     * For this all to work, the <code>web.xml</code> needs to set with
     * a security constraint that points to a realm named for the passed
     * webapp, <code>webappName</code>.
     *
     * @param realmName Name of realm to configure.
     * @param contextName Name of context we're using with this realm.
     * If null, we'll use the realm name as context name.
     * @param authProperties Path to file that holds the auth login and
     * password.
     * @return Hash of user realms.
     *
     * @throws IOException
     */
    public HashUserRealm setAuthentication(String realmName,
        String contextName, String authProperties)
    throws IOException {
        HashUserRealm realm =
            (authProperties != null && authProperties.length() > 0)?
                new HashUserRealm(realmName, authProperties):
                new HashUserRealm(realmName);
        this.server.addRealm(realm);
        if (contextName == null || contextName.length() <= 0) {
            contextName = realmName;
        }
        WebApplicationContext context = getContext(contextName);
        context.setRealmName(realmName);
        return realm;
    }
    
    public void setAuthentication(String realmName, String contextName,
            String username, String password, String role)
    throws IOException {
        HashUserRealm realm = setAuthentication(realmName, contextName,
            null);
        realm.put(username, password);
        realm.addUserToRole(username, role);
    }
    

    /**
     * Reset the administrator login info. 
     * 
     * @param realmAndRoleName for our use, always 'admin'
     * @param oldUsername previous username to replace/disable
     * @param newUsername new username (may be same as old)
     * @param newPassword new password
     */
    public void resetAuthentication(String realmAndRoleName,
        String oldUsername, String newUsername, String newPassword) {
        HashUserRealm realm = (HashUserRealm)this.server.
            getRealm(realmAndRoleName);
        realm.remove(oldUsername);
        realm.put(newUsername,newPassword);
        realm.addUserToRole(newUsername, realmAndRoleName);
    }

    /**
     * Get path to named webapp.
     *
     * @param name Name of webpp.  Possible names are 'admin' or 'selftest'.
     *
     * @return Path to deployed webapp.
     */
    public File getWebappPath(String name) {

        if (this.server == null) {
            throw new NullPointerException("Server does not exist");
        }
        String contextName =
            (name.equals(this.server.getRootWebApp()))? "/": "/" + name;
        return new
            File(getContext(contextName).getServletHandler().getRealPath("/"));
    }

    /**
     * @return Returns the root webapp name.
     */
    public static String getRootWebappName()
    {
        return ROOT_WEBAPP;
    }
    
    
    /**
     * Returns the hosts that the server is listening on.
     * 
     * @return  the hosts that the server is listening on.
     */
    public Collection<String> getHosts() {
        ArrayList<String> result = new ArrayList<String>();
        for (HttpListener listener: server.getListeners()) {
            result.add(listener.getHost());
        }
        return result;
    }
}
