/* CredentialAvatar
 *
 * Created on Apr 23, 2004
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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.CredentialStore;
import org.archive.crawler.settings.SettingsHandler;


/**
 * A credential representation.
 *
 * Added to the CrawlServer upon successful authentication.  Used as a marker
 * of successful authentication event and for carrying credential
 * payload to be used subsequently doing preemptive authentications (e.g.
 * For case of RFC2617, needs to be offered everytime we're accessing inside
 * a protected area).  Also carried by the CrawlURI when cycling through
 * processing chain trying a credential to see if it will authenticate.
 *
 * <p>This class exists because its not safe to keep references
 * to the settings derived Credential classes so instead of keeping references
 * to credential classes, we carry around this avatar.
 *
 * <p>Scope for avatars is crawlserver.  Only used within a CrawlServer
 * scope.
 *
 * <p>Immutable.
 *
 * @author stack
 * @version $Revision: 4668 $, $Date: 2006-09-26 21:49:01 +0000 (Tue, 26 Sep 2006) $
 */
public class CredentialAvatar
implements Serializable {

    private static final long serialVersionUID = 4489542750898404807L;

    private static final Logger logger =
        Logger.getLogger(CredentialAvatar.class.getName());

    /**
     * Key for this credential avatar.
     */
    private final String key;

    /**
     * Type represented by this avatar.
     */
    private final Class type;

    /**
     * Data.
     *
     * May be null.
     * 
     * <p>This used to be an Object and I used to store in here
     * the httpclient AuthScheme but AuthScheme is not serializable
     * and so there'd be trouble getting this payload to lie down
     * in a bdb database.  Changed it to String.  That should be
     * generic enough for credential purposes.
     */
    private final String payload;


    /**
     * Constructor.
     * @param type Type for this credential avatar.
     * @param key Key for this credential avatar.
     */
    public CredentialAvatar(Class type, String key) {
        this(type, key, null);
    }

    /**
     * Constructor.
     * @param type Type for this credential avatar.
     * @param key Key for this credential avatar.
     * @param payload Data credential needs rerunning or preempting.  May be
     * null and then just the presence is used as signifier of successful
     * auth.
     */
    public CredentialAvatar(Class type, String key, String payload) {
        if (!checkType(type)) {
            throw new IllegalArgumentException("Type is unrecognized: " +
                type);
        }
        this.key = key;
        this.type = type;
        this.payload = payload;
    }

    /**
     * Shutdown default constructor.
     */
    private CredentialAvatar() {
        super();
        this.key = null;
        this.type = null;
        this.payload = null;
    }

    /**
     * @param candidateType Type to check.
     * @return True if this is a known credential type.
     */
    protected boolean checkType(Class candidateType) {
        boolean result = false;
        List types = CredentialStore.getCredentialTypes();
        for (Iterator i = types.iterator(); i.hasNext();) {
            if (((Class)i.next()).equals(candidateType)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * @return Returns the payload. May be null.
     */
    public String getPayload() {
        return this.payload;
    }

    /**
     * @return Returns the key.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * @return Type represented by this avatar.
     */
    public Class getType() {
        return this.type;
    }

	/**
	 * @param otherType Class to match.
	 * @return True if this credential avatar is of same type.
	 */
	public boolean match(Class otherType) {
		return this.type.equals(otherType);
	}

    /**
     * @param otherType Credential to match.
     * @param otherKey Key to test.
     * @return True if this is avatar for passed credential.
     */
    public boolean match(Class otherType, String otherKey) {
        return match(otherType) &&
            (otherKey != null && this.key != null &&
            		this.key.equals(otherKey));
    }

    public String toString() {
        return getType() + "." + this.getKey();
    }

    /**
     * @param handler Settings handler.
     * @param curi CrawlURI to use for context.
     * @return The credential this avatar represents.
     */
    public Credential getCredential(SettingsHandler handler, CrawlURI curi) {
        Credential result = null;

        CredentialStore cs = CredentialStore.getCredentialStore(handler);
        if (cs == null) {
            logger.severe("No credential store for " + curi);
            return result;
        }

        Iterator i = cs.iterator(curi);
        if (i == null) {
            logger.severe("Have CredentialAvatar " + toString() +
                " but no iterator: " + curi);
            return result;
        }

        while (i.hasNext()) {
            Credential c = (Credential)i.next();
            if (!this.type.isInstance(c)) {
                continue;
            }
            String credKey = null;
            try {
                credKey = c.getKey(curi);
            }
            catch (AttributeNotFoundException e) {
                logger.severe("Failed to get key for " + c + " from " + curi);
            }
            if (credKey != null && credKey.equals(getKey())) {
                result = c;
                break;
            }
        }

        if (result == null) {
            logger.severe("Have CredentialAvatar " + toString() +
                " but no corresponding credential: " + curi);
        }

        return result;
    }
}
