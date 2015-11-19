/* HtmlFormCredential
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;



/**
 * Credential that holds all needed to do a GET/POST to a HTML form.
 *
 * @author stack
 * @version $Revision: 5913 $, $Date: 2008-07-28 22:34:52 +0000 (Mon, 28 Jul 2008) $
 */
public class HtmlFormCredential extends Credential {

    private static final long serialVersionUID = -4732570804435453949L;

    private static final Logger logger =
        Logger.getLogger(HtmlFormCredential.class.getName());

    private static final String ATTR_LOGIN_URI = "login-uri";
    private static final String ATTR_FORM_ITEMS = "form-items";
    private static final String ATTR_FORM_METHOD = "http-method";
    private static final String [] METHODS = {"POST", "GET"};

    /**
     * Constructor.
     *
     * A constructor that takes name of the credential is required by settings
     * framework.
     *
     * @param name Name of this credential.
     */
    public HtmlFormCredential(final String name)
    {
        super(name, "Credential that has all necessary" +
            " for running a POST/GET to an HTML login form.");

        Type t = addElementToDefinition(new SimpleType("login-uri",
            "Full URI of page that contains the HTML login form we're to" +
            " apply these credentials too: E.g. http://www.archive.org", ""));
        t.setOverrideable(false);
        t.setExpertSetting(true);


        t = addElementToDefinition(new SimpleType(ATTR_FORM_METHOD,
            "GET or POST", METHODS[0], METHODS));
        t.setOverrideable(false);
        t.setExpertSetting(true);

        t = addElementToDefinition(new MapType(ATTR_FORM_ITEMS, "Form items.",
            String.class));
        t.setOverrideable(false);
        t.setExpertSetting(true);
    }

    /**
     * @param context CrawlURI context to use.
     * @return login-uri.
     * @throws AttributeNotFoundException
     */
    public String getLoginUri(final CrawlURI context)
            throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_LOGIN_URI, context);
    }

    /**
     * @param context CrawlURI context to use.
     * @return login-uri.
     * @throws AttributeNotFoundException
     */
    public String getHttpMethod(final CrawlURI context)
            throws AttributeNotFoundException {
        return (String)getAttribute(ATTR_FORM_METHOD, context);
    }

    /**
     * @param context CrawlURI context to use.
     * @return Form inputs as convenient map.  Returns null if no form items.
     * @throws AttributeNotFoundException
     */
    public Map<String,Object> getFormItems(final CrawlURI context)
            throws AttributeNotFoundException {
        Map<String,Object> result = null;
        MapType items = (MapType)getAttribute(ATTR_FORM_ITEMS, context);
        if (items != null) {
            for (Iterator i = items.iterator(context); i.hasNext();) {
                Attribute a = (Attribute)i.next();
                if (result == null) {
                    result = new HashMap<String,Object>();
                }
                result.put(a.getName(), a.getValue());
            }
        }
        return result;
    }

    public boolean isPrerequisite(final CrawlURI curi) {
        boolean result = false;
        String curiStr = curi.getUURI().toString();
        String loginUri = getPrerequisite(curi);
        if (loginUri != null) {
            try {
                UURI uuri = UURIFactory.getInstance(curi.getUURI(), loginUri);
                if (uuri != null && curiStr != null &&
                    uuri.toString().equals(curiStr)) {
                    result = true;
                    if (!curi.isPrerequisite()) {
                        curi.setPrerequisite(true);
                        logger.fine(curi + " is prereq.");
                    }
                }
            } catch (URIException e) {
                logger.severe("Failed to uuri: " + curi + ", " +
                    e.getMessage());
            }
        }
        return result;
    }

    public boolean hasPrerequisite(CrawlURI curi) {
        return getPrerequisite(curi) != null;
    }

    public String getPrerequisite(CrawlURI curi) {
        String loginUri = null;
        try {
            loginUri = getLoginUri(curi);
        } catch (AttributeNotFoundException e) {
            logger.severe("Failed to getLoginUri: " + this + ", " + curi + ","
                + e.getMessage());
            // Not much I can do here. What if I fail every time? Then
            // this prereq. will not ever be processed.  We'll never get on to
            // this server.
        }
        return loginUri;
    }

    public String getKey(CrawlURI curi) throws AttributeNotFoundException {
        return getLoginUri(curi);
    }

    public boolean isEveryTime() {
        // This authentication is one time only.
        return false;
    }

    public boolean populate(CrawlURI curi, HttpClient http, HttpMethod method,
            String payload) {
        // http is not used.
        // payload is not used.
        boolean result = false;
        Map formItems = null;
        try {
            formItems = getFormItems(curi);
        }
        catch (AttributeNotFoundException e1) {
            logger.severe("Failed get of form items for " + curi);
        }
        if (formItems == null || formItems.size() <= 0) {
            try {
                logger.severe("No form items for " + method.getURI());
            }
            catch (URIException e) {
                logger.severe("No form items and exception getting uri: " +
                    e.getMessage());
            }
            return result;
        }

        NameValuePair[] data = new NameValuePair[formItems.size()];
        int index = 0;
        String key = null;
        for (Iterator i = formItems.keySet().iterator(); i.hasNext();) {
            key = (String)i.next();
            data[index++] = new NameValuePair(key, (String)formItems.get(key));
        }
        if (method instanceof PostMethod) {
            ((PostMethod)method).setRequestBody(data);
            result = true;
        } else if (method instanceof GetMethod) {
            // Append these values to the query string.
            // Get current query string, then add data, then get it again
            // only this time its our data only... then append.
            HttpMethodBase hmb = (HttpMethodBase)method;
            String currentQuery = hmb.getQueryString();
            hmb.setQueryString(data);
            String newQuery = hmb.getQueryString();
            hmb.setQueryString(
                    ((StringUtils.isNotEmpty(currentQuery))
                            ? currentQuery + "&"
                            : "")
            		+ newQuery);
            result = true;
        } else {
            logger.severe("Unknown method type: " + method);
        }
        return result;
    }

    public boolean isPost(CrawlURI curi) {
        String method = null;
        try {
            method = getHttpMethod(curi);
        }
        catch (AttributeNotFoundException e) {
            logger.severe("Failed to get method for " + curi + ", " + this);
        }
        return method != null && method.equalsIgnoreCase("POST");
    }
}
