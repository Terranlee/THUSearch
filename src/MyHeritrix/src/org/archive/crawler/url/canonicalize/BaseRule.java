/* BaseRule
 * 
 * Created on Oct 5, 2004
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
package org.archive.crawler.url.canonicalize;

import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.url.CanonicalizationRule;

/**
 * Base of all rules applied canonicalizing a URL that are configurable
 * via the Heritrix settings system.
 * 
 * This base class is abstact.  Subclasses must implement the
 * {@link CanonicalizationRule#canonicalize(String, Object)} method.
 * 
 * @author stack
 * @version $Date: 2005-11-04 23:00:23 +0000 (Fri, 04 Nov 2005) $, $Revision: 3932 $
 */
public abstract class BaseRule
extends ModuleType
implements CanonicalizationRule {
    private static Logger logger =
        Logger.getLogger(BaseRule.class.getName());
    public static final String ATTR_ENABLED = "enabled";
    
    /**
     * Constructor.
     * @param name Name of this canonicalization rule.
     * @param description Description of what this rule does.
     */
    public BaseRule(String name, String description) {
        super(name, description);
        setExpertSetting(true);
        setOverrideable(true);
        Object [] possibleValues = {Boolean.TRUE, Boolean.FALSE};
        addElementToDefinition(new SimpleType(ATTR_ENABLED,
            "Rule is enabled.", new Boolean(true), possibleValues));
    }
    
    public boolean isEnabled(Object context) {
        boolean result = true;
        try {
            Boolean b = (Boolean)getAttribute(context, ATTR_ENABLED);
            if (b != null) {
                result = b.booleanValue();
            }
        } catch (AttributeNotFoundException e) {
            logger.warning("Failed get of 'enabled' attribute.");
        }

        return result;
    }
    
    /**
     * Run a regex that strips elements of a string.
     * 
     * Assumes the regex has a form that wants to strip elements of the passed
     * string.  Assumes that if a match, appending group 1
     * and group 2 yields desired result.
     * @param url Url to search in.
     * @param matcher Matcher whose form yields a group 1 and group 2 if a
     * match (non-null.
     * @return Original <code>url</code> else concatenization of group 1
     * and group 2.
     */
    protected String doStripRegexMatch(String url, Matcher matcher) {
        return (matcher != null && matcher.matches())?
            checkForNull(matcher.group(1)) + checkForNull(matcher.group(2)):
            url;
    }

    /**
     * @param string String to check.
     * @return <code>string</code> if non-null, else empty string ("").
     */
    private String checkForNull(String string) {
        return (string != null)? string: "";
    }
}
