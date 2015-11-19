/* ScopePlusOneDecideRule
*
* Created on Aug 22, 2005
*
* Copyright 2005 Regents of the University of California, All rights reserved
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
package org.archive.crawler.deciderules;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.net.UURI;
import org.archive.util.SurtPrefixSet;

/**
 * Rule allows one level of discovery beyond configured scope
 * (e.g. Domain, plus the first otherwise out-of-scope link from an
 * in-scope page, but not further hops from that first page)
 *
 * @author Shifra Raffel
 * @version $Date: 2006-09-25 17:16:55 +0000 (Mon, 25 Sep 2006) $ $Revision: 4649 $
 */
public class ScopePlusOneDecideRule extends SurtPrefixedDecideRule {

    private static final long serialVersionUID = -6344162369024146340L;

    public static final String ATTR_SCOPE = "host-or-domain-scope";
    public static final String HOST = "Host";
    public static final String DOMAIN = "Domain";
    
    private static final Logger logger =
        Logger.getLogger(ScopePlusOneDecideRule.class.getName());
    
    /**
     * Constructor.
     * @param name
     */
    public ScopePlusOneDecideRule(String name) {
        super(name);
        setDescription(
            "ScopePlusOneDecideRule. Rule allows one level of discovery " +
            "beyond configured scope (e.g. Domain, plus the first " +
            "otherwise out-of-scope link from an in-scope page, but " +
            "no further hops from that first otherwise-out-of-scope page). " +
            "surts-source-file is optional. Use surts-dump-file option " +
            "when testing.");
        addElementToDefinition(new SimpleType(ATTR_SCOPE,
            "Restrict to host, e.g. archive.org excludes audio.archive.org, " +
            "or expand to domain as well, e.g. archive.org includes all " +
            "*.archive.org", DOMAIN, new String[] {HOST, DOMAIN}));
    }

    /**
     * Evaluate whether given object comes from a URI which is in scope
     *
     * @param object to evaluate
     * @return true if URI is either in scope or its via is
     */
    protected boolean evaluate(Object object) {
        boolean result = false;
        if (!(object instanceof CandidateURI)) {
            // Can't evaluate if not a candidate URI
            return false; 
        }
        SurtPrefixSet set = getPrefixes(object);
        UURI u = UURI.from(object);
        // First, is the URI itself in scope?
        boolean firstResult = isInScope(u, set);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Tested scope of UURI itself '" + u +
                        " and result was " + firstResult);
        }                        
        if (firstResult == true) {
            result = true;
        } else {
            // This object is not itself within scope, but
            // see whether its via might be
            UURI via = getVia(object);
            if (via == null) {
                // If there is no via and the URL doesn't match scope,reject it
                return false;
            }
            // If the via is within scope, accept it
            result = isInScope (via, set);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Tested via UURI '" + via +
                        " and result was " + result);
            }            
        }
        return result;
    }
    
    /**
     * Synchronized get of prefix set to use
     * 
     * @return SurtPrefixSet to use for check
     *@see org.archive.crawler.deciderules.SurtPrefixedDecideRule#getPrefixes()
     */
    protected synchronized SurtPrefixSet getPrefixes() {
        return getPrefixes(null);
    } 
    
    /**
     * Synchronized get of prefix set to use.
     * @param o Context object.
     * 
     * @return SurtPrefixSet to use for check
     * @see org.archive.crawler.deciderules.SurtPrefixedDecideRule#getPrefixes()
     */
    protected synchronized SurtPrefixSet getPrefixes(Object o) {
        if (surtPrefixes == null) {
            readPrefixes(o);
        }
        return surtPrefixes;
    }    
    
    /**
     * Patch the SURT prefix set so that it only includes the appropriate
     * prefixes.
     * @param o Context object.
     * @see org.archive.crawler.deciderules.SurtPrefixedDecideRule#readPrefixes()
     */
    protected void readPrefixes(Object o) {
        buildSurtPrefixSet();
        // See whether Host or Domain was chosen
        String scope = this.getScope(o);
        if (scope.equals(HOST)){
            surtPrefixes.convertAllPrefixesToHosts();            
        } else if (scope.equals(DOMAIN)) {
            surtPrefixes.convertAllPrefixesToDomains();            
        }
        dumpSurtPrefixSet();
    }        
    
    private UURI getVia(Object o){
        return (o instanceof CandidateURI)? ((CandidateURI)o).getVia(): null;
    }    

    /**
     * Decide whether using host or domain scope
     * @param o Context
     * @return String Host or domain
     * 
     */
    protected String getScope(Object o) {
        try {
            String scope = (String)getAttribute(o, ATTR_SCOPE);
            if (scope.equals(HOST)) {
                return HOST;
            } else if (scope.equals(DOMAIN)) {
                return DOMAIN;
            } else {
                assert false : "Unrecognized scope " + scope
                        + ". Should never happen!";
            }
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return null; // Basically the rule is inactive if this occurs.
    }
    
    //check that the URI is in scope
    private boolean isInScope (Object o, SurtPrefixSet set) {
        boolean iResult = false;
        UURI u = (UURI)o;
        if (u == null) {
            return false;
        }
        String candidateSurt = u.getSurtForm();
        // also want to treat https as http
        if (candidateSurt.startsWith("https:")) {
            candidateSurt = "http:" + candidateSurt.substring(6);
        }
        if (set.containsPrefixOf(candidateSurt)){
            iResult = true;          
        }
        return iResult;
    }
}
