/* Rfc2617Credential
 *
 * Created on Apr 7, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.datamodel.credential;


import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;



/**
 * A Basic/Digest auth RFC2617 credential.
 *
 * @author stack
 * @version $Revision: 4668 $, $Date: 2006-09-26 21:49:01 +0000 (Tue, 26 Sep 2006) $
 */
public class Rfc2617Credential extends Credential {

    private static final long serialVersionUID = -1909614285968756188L;

    private static Logger logger =
        Logger.getLogger(Rfc2617Credential.class.getName());

    private static final String ATTR_REALM = "realm";
    private static final String ATTR_LOGIN = "login";
    private static final String ATTR_PASSWORD = "password";


    /**
     * Constructor.
     *
     * A constructor that takes name of the credential is required by settings
     * framework.
     *
     * @param name Name of this credential.
     */
    public Rfc2617Credential(String name) {
        super(name, "Basic/Digest Auth type credential.");

        Type t = addElementToDefinition(new SimpleType(ATTR_REALM,
            "Basic/Digest Auth realm.", "Realm"));
        t.setOverrideable(false);
        t.setExpertSetting(true);

        t = addElementToDefinition(new SimpleType(ATTR_LOGIN, "Login.",
            "login"));
        t.setOverrideable(false);
        t.setExpertSetting(true);

        t = addElementToDefinition(new SimpleType(ATTR_PASSWORD, "Password.",
            "password"));
        t.setOverrideable(false);
        t.setExpertSetting(true);
    }

    /**
     * @param context Context to use when searching the realm.
     * @return Realm using set context.
     * @throws AttributeNotFoundException
     */
    public String getRealm(CrawlURI context)
            throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_REALM, context);
    }

    /**
     * @param context CrawlURI ontext to use.
     * @return login to use doing credential.
     * @throws AttributeNotFoundException
     */
    public String getLogin(CrawlURI context)
            throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_LOGIN, context);
    }

    /**
     * @param context CrawlURI ontext to use.
     * @return Password to use doing credential.
     * @throws AttributeNotFoundException
     */
    public String getPassword(CrawlURI context)
            throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_PASSWORD, context);
    }

    public boolean isPrerequisite(CrawlURI curi) {
        // Return false.  Later when we implement preemptive
        // rfc2617, this will change.
        return false;
    }

    public boolean hasPrerequisite(CrawlURI curi) {
        // Return false.  Later when we implement preemptive
        // rfc2617, this will change.
        return false;
    }

    public String getPrerequisite(CrawlURI curi) {
        // Return null.  Later when we implement preemptive
        // rfc2617, this will change.
        return null;
    }

    public String getKey(CrawlURI context) throws AttributeNotFoundException {
        return getRealm(context);
    }

    public boolean isEveryTime() {
        return true;
    }

    public boolean populate(CrawlURI curi, HttpClient http, HttpMethod method,
            String payload) {
        boolean result = false;
        String authRealm = payload;
        if (authRealm == null) {
            logger.severe("No authscheme though creds: " + curi);
            return result;
        }

        // Always add the credential to HttpState. Doing this because no way of
        // removing the credential once added AND there is a bug in the
        // credentials management system in that it always sets URI root to
        // null: it means the key used to find a credential is NOT realm + root
        // URI but just the realm. Unless I set it everytime, there is
        // possibility that as this thread progresses, it might come across a
        // realm already loaded but the login and password are from another
        // server. We'll get a failed authentication that'd be difficult to
        // explain.
        //
        // Have to make a UsernamePasswordCredentials. The httpclient auth code
        // does an instanceof down in its guts.
        UsernamePasswordCredentials upc = null;
        try {
        	upc = new UsernamePasswordCredentials(getLogin(curi),
        	    getPassword(curi));
        	http.getState().setCredentials(new AuthScope(curi.getUURI().getHost(),
        	    curi.getUURI().getPort(), authRealm), upc);
        	logger.fine("Credentials for realm " + authRealm +
        	    " for CrawlURI " + curi.toString() + " added to request: " +
				result);
        	result = true;
        } catch (AttributeNotFoundException e1) {
        	logger.severe("Failed to get login and password for " +
        			curi + " and " + authRealm);
        } catch (URIException e) {
        	logger.severe("Failed to parse host from " + curi + ": " +
        			e.getMessage());
        }
        
        return result;
    }

    public boolean isPost(CrawlURI curi) {
        // Return false.  This credential type doesn't care whether posted or
        // get'd.
        return false;
    }

    /**
     * Convenience method that does look up on passed set using realm for key.
     *
     * @param rfc2617Credentials Set of Rfc2617 credentials.  If passed set is
     * not pure Rfc2617Credentials then will be ClassCastExceptions.
     * @param realm Realm to find in passed set.
     * @param context Context to use when searching the realm.
     * @return Credential of passed realm name else null.  If more than one
     * credential w/ passed realm name, and there shouldn't be, we return first
     * found.
     */
    public static Rfc2617Credential getByRealm(Set rfc2617Credentials,
            String realm, CrawlURI context) {

        Rfc2617Credential result = null;
        if (rfc2617Credentials == null || rfc2617Credentials.size() <= 0) {
            return result;
        }
        if (rfc2617Credentials != null && rfc2617Credentials.size() > 0) {
            for (Iterator i = rfc2617Credentials.iterator(); i.hasNext();) {
                Rfc2617Credential c = (Rfc2617Credential)i.next();
                try {
                    if (c.getRealm(context).equals(realm)) {
                        result = c;
                        break;
                    }
                } catch (AttributeNotFoundException e) {
                    logger.severe("Failed look up by realm " + realm + " " + e);
                }
            }
        }
        return result;
    }
}
