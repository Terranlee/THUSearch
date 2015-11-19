/* MatchesListRegExpDecideRule
 * 
 * $Id: MatchesListRegExpDecideRule.java 4649 2006-09-25 17:16:55Z paul_jack $
 * 
 * Created on 30.5.2005
 *
 * Copyright (C) 2005 Kristinn Sigurdsson
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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.util.TextUtils;


/**
 * Rule applies configured decision to any CrawlURIs whose String URI
 * matches the supplied regexps.
 * <p>
 * The list of regular expressions can be considered logically AND or OR.
 *
 * @author Kristinn Sigurdsson
 * 
 * @see MatchesRegExpDecideRule
 */
public class MatchesListRegExpDecideRule extends PredicatedDecideRule {

    private static final long serialVersionUID = 3011579758573454930L;

    private static final Logger logger =
        Logger.getLogger(MatchesListRegExpDecideRule.class.getName());
    
    public static final String ATTR_REGEXP_LIST = "regexp-list";
    public static final String ATTR_LIST_LOGIC= "list-logic";
    
    public static final String DEFAULT_LIST_LOGIC = "OR";
    public static final String[] LEGAL_LIST_LOGIC = {"OR","AND"};

    /**
     * Usual constructor. 
     * @param name
     */
    public MatchesListRegExpDecideRule(String name) {
        super(name);
        setDescription("MatchesListRegExpDecideRule. Applies the configured " +
            "decision to URIs matching the supplied regular expressions.\n" +
            "The list of regular expressions can be considered logically AND " +
            "or OR.");
        addElementToDefinition(
                new SimpleType(ATTR_LIST_LOGIC, "Should the list of regular " +
                    "expressions be considered as logically AND or OR when " +
                    "matching.", 
                    DEFAULT_LIST_LOGIC, LEGAL_LIST_LOGIC));
        addElementToDefinition(new StringList(ATTR_REGEXP_LIST,"The list of " +
             "regular expressions to evalute against the URI."));
    }

    /**
     * Evaluate whether given object's string version
     * matches configured regexps
     * 
     * @param o
     * @return true if regexps are matched
     */
    protected boolean evaluate(Object o) {
        try {
            List regexps = getRegexp(o);
            if(regexps.size()==0){
                return false;
            }
            String str = o.toString();
            Iterator it = regexps.iterator();
            
            boolean listLogicOR = isListLogicOR(o);
            // Result is initialized so that if OR based the default assumption is
            // false (find no matches) but if AND based the default assumption is
            // true (finds no non-matches)
            boolean result = listLogicOR == false;
            
            while(it.hasNext()){
                String regexp = (String)it.next();
                boolean matches = TextUtils.matches(regexp, str);

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Tested '" + str + "' match with regex '" +
                        regexp + " and result was " + matches);
                }
                
                if(matches){
                    if(listLogicOR){
                        // OR based and we just got a match, done!
                        result = true;
                        break;
                    }
                } else {
                    if(listLogicOR == false){
                        // AND based and we just found a non-match, done!
                        result = false;
                        break;
                    }
                }
            }
            
            if (logger.isLoggable(Level.FINE) && result){
                logger.fine("Matched: " + str);
            }
            
            return result;
        } catch (ClassCastException e) {
            // if not CrawlURI, always disregard
            return false; 
        }
    }
    
    /** 
     * Get the regular expressions list to match the URI against.
     *
     * @param o the object for which the regular expression should be
     *          matched against.
     * @return the regular expression to match against.
     */
    protected List getRegexp(Object o) {
        try {
            return (StringList) getAttribute(o, ATTR_REGEXP_LIST);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            // Basically the filter is inactive if this occurs
            // (The caller should be returning false when regexp is null).
            return null;  
        }
    }
    
    protected boolean isListLogicOR(Object o){
        String logic = DEFAULT_LIST_LOGIC;
        try {
            logic = (String) getAttribute(o, ATTR_LIST_LOGIC);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return logic.equals("OR") ? true : false;
    }
}