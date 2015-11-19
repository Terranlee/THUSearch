/* AddRedirectFromRootServerToScope
 * 
 * Created on May 25, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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

import java.util.logging.Logger;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.net.UURI;


public class AddRedirectFromRootServerToScope extends PredicatedDecideRule {

    private static final long serialVersionUID = 2644131585813079064L;

    private static final Logger LOGGER =
	        Logger.getLogger(AddRedirectFromRootServerToScope.class.getName());
	private static final String SLASH = "/";
	public AddRedirectFromRootServerToScope(String name) {
		super(name);
		setDescription("Allow URI only if it is a redirect and via URI is a " +
				"root server (host's slash page) that is within the " +
				"scope. Also mark the URI as a seed."); 
	}

	protected boolean evaluate(Object object) {
		UURI via = getVia(object);
		if (via == null) {
			return false;
		}
		CandidateURI curi = (CandidateURI) object;
		if ( curi == null) {
			return false;
		}
		try {
			// Mark URI as seed if via is from different host, URI is not a seed
			// already, URI is redirect and via is root server
			if (curi.getUURI().getHostBasename() != null &&
					via.getHostBasename() != null &&
					!curi.getUURI().getHostBasename().equals(via.getHostBasename())
				    && curi.isLocation()
					&& via.getPath().equals(SLASH)) {
				curi.setIsSeed(true);
				LOGGER.info("Adding " + object.toString() + " to seeds via "
						+ getVia(object).toString());
				return true;
			}
		} catch (URIException e) {
			e.printStackTrace();
		} catch (Exception e) {
            e.printStackTrace();
			// Return false since we could not get hostname or something else 
			// went wrong
		}		
		return false;
	}

    private UURI getVia(Object o){
        return (o instanceof CandidateURI)? ((CandidateURI)o).getVia(): null;
    }    
}
