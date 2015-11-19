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
 * RobotsExclusionPolicy.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.settings.CrawlerSettings;

/**
 * RobotsExclusionPolicy represents the actual policy adopted with 
 * respect to a specific remote server, usually constructed from 
 * consulting the robots.txt, if any, the server provided. 
 * 
 * (The similarly named RobotsHonoringPolicy, on the other hand, 
 * describes the strategy used by the crawler to determine to what
 * extent it respects exclusion rules.)
 * 
 * The expiration of policies after a suitable amount of time has
 * elapsed since last fetch is handled outside this class, in 
 * CrawlServer itself. 
 * 
 * TODO: refactor RobotsHonoringPolicy to be a class-per-policy, and 
 * then see if a CrawlServer with a HonoringPolicy and a RobotsTxt
 * makes this mediating class unnecessary. 
 * 
 * @author gojomo
 *
 */
public class RobotsExclusionPolicy implements Serializable {

    private static final long serialVersionUID = 6323907991237383113L;

    private static final Logger logger =
        Logger.getLogger(RobotsExclusionPolicy.class.getName());

    private final static int NORMAL_TYPE = 0;
    private final static int ALLOWALL_TYPE = 1;
    private final static int DENYALL_TYPE = 2;
    private transient int type = NORMAL_TYPE;

    public static RobotsExclusionPolicy ALLOWALL =
        new RobotsExclusionPolicy(ALLOWALL_TYPE);
    public static RobotsExclusionPolicy DENYALL =
        new RobotsExclusionPolicy(DENYALL_TYPE);

    private Robotstxt robotstxt = null;
    // FIXME?: this 'transient' seems wrong -- likely to cause
    // all non-normal policies to break when CrawlServer
    // go through a serialization/deserialization cycle
    transient RobotsHonoringPolicy honoringPolicy = null;

    private String lastUsedUserAgent = null;
    private List<String> userAgentsToTest = null;

    /**
     * @param settings 
     * @param reader
     * @param honoringPolicy
     * @return Robot exclusion policy.
     * @throws IOException
     */
    public static RobotsExclusionPolicy policyFor(CrawlerSettings settings,
            BufferedReader reader, RobotsHonoringPolicy honoringPolicy)
    throws IOException {
        Robotstxt robots = new Robotstxt(reader);
        return (robots.allowsAll())?
            ALLOWALL:
            new RobotsExclusionPolicy(settings, robots, honoringPolicy);
    }



    /**
     * @param settings 
     * @param u
     * @param d
     * @param honoringPolicy
     */
    public RobotsExclusionPolicy(CrawlerSettings settings,
            Robotstxt robotstxt, 
            RobotsHonoringPolicy honoringPolicy) {
        this.robotstxt = robotstxt;
        this.honoringPolicy = honoringPolicy;

        if(honoringPolicy == null) return;

        // If honoring policy is most favored user agent, all rules should be checked
        if(honoringPolicy.isType(settings, RobotsHonoringPolicy.MOST_FAVORED)) {
            userAgentsToTest = robotstxt.getUserAgents();

        // IF honoring policy is most favored of set, then make a list with only the set as members
        } else if(honoringPolicy.isType(settings, RobotsHonoringPolicy.MOST_FAVORED_SET)) {
            userAgentsToTest = new ArrayList<String>();
            Iterator userAgentSet = honoringPolicy.getUserAgents(settings).iterator();
            while(userAgentSet.hasNext()) {
                String userAgent = (String) userAgentSet.next();

                Iterator iter = robotstxt.getUserAgents().iterator();
                while ( iter.hasNext() ) {
                    String ua = (String)iter.next();
                    if (userAgent.indexOf(ua)>-1) {
                        userAgentsToTest.add(ua);
                        break;
                    }
                }
            }
        }
    }

    public RobotsExclusionPolicy(int type) {
        this(null, null, null);
        this.type = type;
    }

    public boolean disallows(CrawlURI curi, String userAgent) {
        if (this == ALLOWALL)
            return false;
        if (this == DENYALL)
            return true;

        // In the common case with policy=Classic, the useragent is 
        // remembered from uri to uri on the same server
        if((honoringPolicy.isType(curi, RobotsHonoringPolicy.CLASSIC) 
                || honoringPolicy.isType(curi, RobotsHonoringPolicy.CUSTOM))
            && (lastUsedUserAgent == null
            || !lastUsedUserAgent.equals(userAgent))) {

            lastUsedUserAgent = userAgent;
            userAgentsToTest = new ArrayList<String>();
            Iterator iter = robotstxt.getUserAgents().iterator();
            String lowerCaseUserAgent = userAgent.toLowerCase();
            while ( iter.hasNext() ) {
                String ua = (String)iter.next();
                if (lowerCaseUserAgent.indexOf(ua)>-1) {
                    userAgentsToTest.add(ua);
                    break; // consider no more sections
                }
            }
        }

        boolean disallow = false;
        String ua = null;

        // Go thru list of all user agents we might act as
        Iterator uas = userAgentsToTest.iterator();
        while(uas.hasNext()) {
            ua = (String) uas.next();
            String path = null; 
            try {
                 path = curi.getUURI().getPathQuery();
            } catch (URIException e) {
                logger.log(Level.SEVERE,"Failed getPathQuery from " + curi, e);
                disallow = false;
                break;
            }
            if(robotstxt.getDirectivesFor(ua).allows(path)) {
                // at least one applicable set of rules allows
                disallow = false;
                break; 
            } else {
                // at least one applicable set of rules disallows
                // so disallow unless later test allows
                disallow = true; 
            }
        }

        // Are we supposed to masquerade as the user agent to which restrictions
        // we follow?
        if(honoringPolicy.shouldMasquerade(curi) && ua != null && !ua.equals("")) {
            curi.setUserAgent(ua);
        }
        return disallow;
    }

    // Methods for object serialization.

    /** If object is DENYALL or ALLOWALL, only the object identity and type
     * is written in the serialization stream.
     *
     * @param stream the serialization stream.
     * @throws IOException 
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeInt(type);
        if (type == NORMAL_TYPE) {
            stream.defaultWriteObject();
        }
    }

    /** If object is DENYALL or ALLOWALL, only the object identity and type
     * is read from the serialization stream.
     *
     * @param stream the serialization stream.
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        type = stream.readInt();
        if (type == NORMAL_TYPE) {
            stream.defaultReadObject();
        }
    }

    /** If object is DENYALL or ALLOWALL, the object is replaced by constants
     * so that check for object equality works.
     * @return Object.
     */
    private Object readResolve() {
        if (type == NORMAL_TYPE) {
            return this;
        } else if (type == ALLOWALL_TYPE) {
            return ALLOWALL;
        } else if (type == DENYALL_TYPE) {
            return DENYALL;
        }
        return null;
    }


    /**
     * Get the crawl-delay that applies to the given user-agent, or
     * -1 (indicating no crawl-delay known) if not internal RobotsTxt
     * instance. 
     * 
     * @param userAgent
     * @return int Crawl-Delay value, or -1 if non available
     */
    public float getCrawlDelay(String userAgent) {
        if (robotstxt==null) {
            return -1;
        }
        return robotstxt.getDirectivesFor(userAgent).getCrawlDelay();
    }

}
