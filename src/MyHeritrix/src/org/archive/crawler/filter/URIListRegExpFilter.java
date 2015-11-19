/* URIListRegExpFilter
 * 
 * $Id: URIListRegExpFilter.java 4652 2006-09-25 18:41:10Z paul_jack $
 * 
 * Created on 30.5.2005
 *
 * Copyright (C) 2004 Kristinn Sigurdsson.
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
package org.archive.crawler.filter;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.deciderules.DecideRule;
import org.archive.crawler.deciderules.DecidingFilter;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.util.TextUtils;


/**
* Compares passed object -- a CrawlURI, UURI, or String --
* against regular expressions, accepting matches.
* <p>
* Can be configured to logically OR or AND the regular expressions.
*
* @author Kristinn Sigurdsson
* 
* @see org.archive.crawler.filter.URIRegExpFilter
* @deprecated As of release 1.10.0.  Replaced by {@link DecidingFilter} and
* equivalent {@link DecideRule}.
*/
public class URIListRegExpFilter
extends Filter {

    private static final long serialVersionUID = -2587977969340783677L;
    
    private static final Logger logger =
       Logger.getLogger(URIListRegExpFilter.class.getName());
   public static final String ATTR_REGEXP_LIST = "regexp-list";
   public static final String ATTR_LIST_LOGIC= "list-logic";
   public static final String ATTR_MATCH_RETURN_VALUE = "if-match-return";
   
   public static final String DEFAULT_LIST_LOGIC = "OR";
   public static final String[] LEGAL_LIST_LOGIC = {"OR","AND"};
   public static final Boolean DEFAULT_MATCH_RETURN_VALUE = new Boolean(true);

   /**
    * @param name Filter name.
    */
   public URIListRegExpFilter(String name) {
       super(name, "A filter that uses a list of regular expressions " +
       		"*Deprecated* Use DecidingFilter and equivalent DecideRule " +
       		"instead. Can be " +
             "optionally either OR or AND based in its evaluation.");
       addElementToDefinition(
               new SimpleType(ATTR_MATCH_RETURN_VALUE, "What to return when" +
                   " regular expression matches. \n", 
                   DEFAULT_MATCH_RETURN_VALUE));
       addElementToDefinition(
               new SimpleType(ATTR_LIST_LOGIC, "Should the list of regular " +
                   "expressions be considered as logically AND or OR when " +
                   "matching.", 
                   DEFAULT_LIST_LOGIC, LEGAL_LIST_LOGIC));
       addElementToDefinition(new StringList(ATTR_REGEXP_LIST,"The list of " +
            "regular expressions to evalute against the URI."));
   }

   protected boolean innerAccepts(Object o) {
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
       
       result = getMatchReturnValue(o) ? result : !result;
       
       if (logger.isLoggable(Level.FINE) && result){
           logger.fine("Matched: " + str);
       }
       
       return result;
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
   
   protected boolean getMatchReturnValue(Object o){
       try {
           return ((Boolean) getAttribute(o, ATTR_MATCH_RETURN_VALUE)).booleanValue();
       } catch (AttributeNotFoundException e) {
           logger.severe(e.getMessage());
           return DEFAULT_MATCH_RETURN_VALUE.booleanValue();  
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
