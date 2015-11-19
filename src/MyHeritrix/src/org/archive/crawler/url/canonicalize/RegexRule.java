/* RegexRule
 * 
 * Created on Oct 6, 2004
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

import org.archive.crawler.settings.SimpleType;
import org.archive.util.TextUtils;

/**
 * General conversion rule.
 * @author stack
 * @version $Date: 2006-09-25 20:27:35 +0000 (Mon, 25 Sep 2006) $, $Revision: 4655 $
 */
public class RegexRule
extends BaseRule {

    private static final long serialVersionUID = -2658094415450237847L;

    protected static Logger logger =
        Logger.getLogger(BaseRule.class.getName());
    private static final String DESCRIPTION = "General regex rule. " +
        "Specify a matching regex and a format string used outputting" +
        " result if a match was found.  If problem compiling regex or" +
        " interpreting format, problem is logged, and this rule does" +
        " nothing.  See User Manual for example usage.";
    private static final String ATTR_REGEX = "matching-regex";
    private static final String ATTR_FORMAT = "format";
    private static final String ATTR_COMMENT = "comment";
    
    public RegexRule(String name) {
        this(name, "(.*)", "${1}");
    }
    
    protected RegexRule(String name, String defaultRegex,
            String defaultFormat) {
        super(name, DESCRIPTION);
        addElementToDefinition(new SimpleType(ATTR_REGEX,
            "Java regular expression. If the regex matches, we'll rewrite" +
            " the passed url using the specified format pattern.",
            defaultRegex));
        addElementToDefinition(
            new SimpleType(ATTR_FORMAT, "Pattern to use rewriting matched" +
                "url. Use '${1}' to match first regex group, '${2}' for" +
                "next group, etc.", defaultFormat));
        addElementToDefinition(new SimpleType(ATTR_COMMENT,
            "Free-text comment on why this rule was added.", ""));
    }

    public String canonicalize(String url, Object context) {
        String regex = getNullOrAttribute(ATTR_REGEX, context);
        if (regex == null) {
            return url;
        }
        String format = getNullOrAttribute(ATTR_FORMAT, context);
        if (format == null) {
            return url;
        }
        Matcher matcher = TextUtils.getMatcher(regex, url);
        String retVal; 
        if (matcher == null || !matcher.matches()) {
            retVal = url;
        } else {
            StringBuffer buffer = new StringBuffer(url.length() * 2);
            format(matcher, format, buffer);
            retVal = buffer.toString();
        }
        TextUtils.recycleMatcher(matcher);
        return retVal;
    }
    
    /**
     * @param matcher Matched matcher.
     * @param format Output format specifier.
     * @param buffer Buffer to append output to.
     */
    protected void format(Matcher matcher, String format,
            StringBuffer buffer) {
        for (int i = 0; i < format.length(); i++) {
            switch(format.charAt(i)) {
                case '\\':
                    if ((i + 1) < format.length() &&
                            format.charAt(i + 1) == '$') {
                        // Don't write the escape character in output.
                        continue;
                    }
                    
                case '$':
                    // Check to see if its not been escaped.
                    if (i == 0 || (i > 0 && (format.charAt(i - 1) != '\\'))) {
                        // Looks like we have a matching group specifier in
                        // our format string, something like '$2' or '${2}'.
                        int start = i + 1;
                        boolean curlyBraceStart = false;
                        if (format.charAt(start) == '{') {
                            start++;
                            curlyBraceStart = true;
                        }
                        int j = start;
                        for (; j < format.length() &&
                                Character.isDigit(format.charAt(j)); j++) {
                            // While a digit, increment.
                        }
                        if (j > start) {
                            int groupIndex = Integer.
                                parseInt(format.substring(start, j));
                            if (groupIndex >= 0 && groupIndex < 256) {
                                String g = null;
                                try {
                                    g = matcher.group(groupIndex);
                                } catch (IndexOutOfBoundsException e) {
                                    logger.warning("IndexOutOfBoundsException" +
                                        " getting group " + groupIndex +
                                        " from " + matcher.group(0) +
                                        " with format of " + format);
                                }
                                if (g != null) {
                                    buffer.append(g);
                                }
                                // Skip closing curly bracket if one.
                                if (curlyBraceStart &&
                                        format.charAt(j) == '}') {
                                    j++;
                                }
                                // Update the loop index so that we skip over
                                // the ${x} group item.
                                i = (j - 1);
                                // Don't fall through to the default.
                                continue;
                            }
                        }
                        
                    }
                    // Let fall through to default rule.  The '$' was escaped.
                    
                default:
                    buffer.append(format.charAt(i));
            }
        }
    }

    protected String getNullOrAttribute(String name, Object context) {
        try {
            return (String)getAttribute(context, name);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return null;
        }
    }
}
