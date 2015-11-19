/* SURTPrefixSet
*
* $Id: SurtPrefixSet.java 6704 2009-11-25 01:38:55Z gojomo $
*
* Created on Jul 23, 2004
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
package org.archive.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;

import org.apache.commons.httpclient.URIException;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.iterator.LineReadingIterator;
import org.archive.util.iterator.RegexpLineIterator;

/**
 * Specialized TreeSet for keeping a set of String prefixes. 
 * 
 * Redundant prefixes (those that are themselves prefixed
 * by other set entries) are eliminated.
 * 
 * @author gojomo
 */
public class SurtPrefixSet extends PrefixSet {

    private static final long serialVersionUID = 2598365040524933110L;

    private static final String SURT_PREFIX_DIRECTIVE = "+";

    /**
     * Read a set of SURT prefixes from a reader source; keep sorted and 
     * with redundant entries removed.
     * 
     * @param r reader over file of SURT_format strings
     * @throws IOException
     */
    public void importFrom(Reader r) {
        BufferedReader reader = new BufferedReader(r);
        String s;
        
        Iterator iter = 
            new RegexpLineIterator(
                    new LineReadingIterator(reader),
                    RegexpLineIterator.COMMENT_LINE,
                    RegexpLineIterator.NONWHITESPACE_ENTRY_TRAILING_COMMENT,
                    RegexpLineIterator.ENTRY);

        while (iter.hasNext()) {
            s = (String) iter.next();
            add(s.toLowerCase());
        }
    }

    /**
     * @param r Where to read from.
     */
    public void importFromUris(Reader r) {
        BufferedReader reader = new BufferedReader(r);
        String s;
        
        Iterator iter = 
            new RegexpLineIterator(
                    new LineReadingIterator(reader),
                    RegexpLineIterator.COMMENT_LINE,
                    RegexpLineIterator.NONWHITESPACE_ENTRY_TRAILING_COMMENT,
                    RegexpLineIterator.ENTRY);

        while (iter.hasNext()) {
            s = (String) iter.next();
            // s is a URI (or even fragmentary hostname), not a SURT
            addFromPlain(s);
        }
    }

    /**
     * Import SURT prefixes from a reader with mixed URI and SURT prefix
     * format. 
     * 
     * @param r  the reader to import the prefixes from
     * @param deduceFromSeeds   true to also import SURT prefixes implied
     *                          from normal URIs/hostname seeds
     */
    public void importFromMixed(Reader r, boolean deduceFromSeeds) {
        BufferedReader reader = new BufferedReader(r);
        String s;
        
        Iterator iter = 
            new RegexpLineIterator(
                    new LineReadingIterator(reader),
                    RegexpLineIterator.COMMENT_LINE,
                    RegexpLineIterator.NONWHITESPACE_ENTRY_TRAILING_COMMENT,
                    RegexpLineIterator.ENTRY);

        while (iter.hasNext()) {
            s = (String) iter.next();
            if(s.startsWith(SURT_PREFIX_DIRECTIVE)) {
                // it's specifically a SURT prefix line
                String u = s.substring(SURT_PREFIX_DIRECTIVE.length()).trim();
                if(u.indexOf("(")>0) {
                    // formal SURT prefix; toLowerCase just in case
                    add(u.toLowerCase());
                } else {
                    // hostname/normal form URI from which 
                    // to deduce SURT prefix
                    addFromPlain(u);
                }
                
                continue; 
            } else {
                if(deduceFromSeeds) {
                    // also deducing 'implied' SURT prefixes 
                    // from normal URIs/hostname seeds
                    addFromPlain(s);
                }
            }
        }
    }
    
    /**
     * Given a plain URI or hostname, deduce an implied SURT prefix from
     * it and add to active prefixes. 
     * 
     * @param u String of URI or hostname
     */
    private void addFromPlain(String u) {
        u = prefixFromPlain(u);
        add(u);
    }

    /**
     * Given a plain URI or hostname/hostname+path, deduce an implied SURT 
     * prefix from it. Results may be unpredictable on strings that cannot
     * be interpreted as URIs. 
     * 
     * UURI 'fixup' is applied to the URI that is built. 
     *
     * @param u URI or almost-URI to consider
     * @return implied SURT prefix form
     */
    public static String prefixFromPlain(String u) {
        u = ArchiveUtils.addImpliedHttpIfNecessary(u);
        u = coerceFromHttpsForComparison(u);
        boolean trailingSlash = u.endsWith("/");
        // ensure all typical UURI cleanup (incl. IDN-punycoding) is done
        try {
            u = UURIFactory.getInstance(u).toString();
        } catch (URIException e) {
            e.printStackTrace();
            // allow to continue with original string uri
        }
        // except: don't let UURI-fixup add a trailing slash
        // if it wasn't already there (presence or absence of
        // such slash has special meaning specifying implied
        // SURT prefixes)
        if(!trailingSlash && u.endsWith("/")) {
            u = u.substring(0,u.length()-1);
        }
        // convert to full SURT
        u = SURT.fromURI(u);
        // truncate to implied prefix
        u = SurtPrefixSet.asPrefix(u);
        return u;
    }

    /**
     * For SURT comparisons -- prefixes or candidates being checked against
     * those prefixes -- we treat https URIs as if they were http.
     * 
     * @param u string to coerce if it has https scheme
     * @return string converted to http scheme, or original if not necessary
     */
    private static String coerceFromHttpsForComparison(String u) {
        if (u.startsWith("https://")) {
            u = "http" + u.substring("https".length());
        }
        return u;
    }

    /**
     * Utility method for truncating a SURT that came from a 
     * full URI (as a seed, for example) into a prefix
     * for determining inclusion.
     * 
     * This involves: 
     * <pre>
     *    (1) removing the last path component, if any
     *        (anything after the last '/', if there are
     *        at least 3 '/'s)
     *    (2) removing a trailing ')', if present, opening
     *        the possibility of proper subdomains. (This
     *        means that the presence or absence of a
     *        trailing '/' after a hostname in a seed list
     *        is significant for the how the SURT prefix is 
     *        created, even though it is not signficant for 
     *        the URI's treatment as a seed.)
     * </pre>
     *
     * @param s String to work on.
     * @return As prefix.
     */
    private static String asPrefix(String s) {
        // Strip last path-segment, if more than 3 slashes
        s = s.replaceAll("^(.*//.*/)[^/]*","$1");
        // Strip trailing ")", if present and NO path (no 3rd slash).
        if (!s.endsWith("/")) {
            s = s.replaceAll("^(.*)\\)","$1");
        }
        return s;
    }

    /**
     * Calculate the SURT form URI to use as a candidate against prefixes
     * from the given Object (CandidateURI or UURI)
     * 
     * @param object CandidateURI or UURI
     * @return SURT form of URI for evaluation, or null if unavailable
     */
    public static String getCandidateSurt(Object object) {
        UURI u = UURI.from(object);
        if (u == null) {
            return null;
        }
        String candidateSurt = u.getSurtForm();
        // also want to treat https as http
        candidateSurt = coerceFromHttpsForComparison(candidateSurt);
        return candidateSurt;
    }
    /**
     * @param fw
     * @throws IOException
     */
    public void exportTo(Writer fw) throws IOException {
        Iterator iter = this.iterator();
        while(iter.hasNext()) {
            fw.write((String)iter.next() + "\n");
        }
    }

    /**
     * Changes all prefixes so that they enforce an exact host. For
     * prefixes that already include a ')', this means discarding 
     * anything after ')' (path info). For prefixes that don't include
     * a ')' -- domain prefixes open to subdomains -- add the closing
     * ')' (or ",)").  
     */
    public void convertAllPrefixesToHosts() {
        SurtPrefixSet iterCopy = (SurtPrefixSet) this.clone();
        Iterator iter = iterCopy.iterator();
        while (iter.hasNext()) {
            String prefix = (String) iter.next();
            String convPrefix = convertPrefixToHost(prefix);
            if(prefix!=convPrefix) {
            	// if returned value not unchanged, update set
            	this.remove(prefix);
            	this.add(convPrefix);
            }
        }
    }
    
    public static String convertPrefixToHost(String prefix) {
        if(prefix.endsWith(")")) {
            return prefix; // no change necessary
        }
        if(prefix.indexOf(')')<0) {
            // open-ended domain prefix
            if(!prefix.endsWith(",")) {
                prefix += ",";
            }
            prefix += ")";
        } else {
            // prefix with excess path-info
            prefix = prefix.substring(0,prefix.indexOf(')')+1);
        }
        return prefix;
    }

    /**
     * Changes all prefixes so that they only enforce a general
     * domain (allowing subdomains).For prefixes that don't include
     * a ')', no change is necessary. For others, truncate everything
     * from the ')' onward. Additionally, truncate off "www," if it
     * appears.
     */
    public void convertAllPrefixesToDomains() {
        SurtPrefixSet iterCopy = (SurtPrefixSet) this.clone();
        Iterator iter = iterCopy.iterator();
        while (iter.hasNext()) {
            String prefix = (String) iter.next();
            String convPrefix = convertPrefixToDomain(prefix);
            if(prefix!=convPrefix) {
            	// if returned value not unchanged, update set
            	this.remove(prefix);
            	this.add(convPrefix);
            }
        } 
    }
    
    public static String convertPrefixToDomain(String prefix) {
        if(prefix.indexOf(')')>=0) {
            prefix = prefix.substring(0,prefix.indexOf(')'));
        }
        // strip 'www,' when present
        if(prefix.endsWith("www,")) {
            prefix = prefix.substring(0,prefix.length()-4);
        }
        return prefix;
    }
    
    /**
     * Allow class to be used as a command-line tool for converting 
     * URL lists (or naked host or host/path fragments implied
     * to be HTTP URLs) to implied SURT prefix form. 
     * 
     * Read from stdin or first file argument. Writes to stdout. 
     *
     * @param args cmd-line arguments: may include input file
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        InputStream in = args.length > 0 ? new BufferedInputStream(
                new FileInputStream(args[0])) : System.in;
        PrintStream out = args.length > 1 ? new PrintStream(
                new BufferedOutputStream(new FileOutputStream(args[1])))
                : System.out;
        BufferedReader br =
            new BufferedReader(new InputStreamReader(in));
        String line;
        while((line = br.readLine())!=null) {
            if(line.indexOf("#")>0) line=line.substring(0,line.indexOf("#"));
            line = line.trim();
            if(line.length()==0) continue;
            out.println(prefixFromPlain(line));
        }
        br.close();
        out.close();
    }
}
