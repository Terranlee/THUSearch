/*
 * ExtractorURITest
 *
 * $Id: ExtractorURITest.java 4595 2006-09-02 00:43:59Z gojomo $
 *
 * Created on August 30, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.crawler.extractor;

import java.util.List;

import org.archive.net.UURI;

import junit.framework.TestCase;

/**
 * Test ExtractorURI
 * 
 * @author gojomo
 */
public class ExtractorURITest extends TestCase {
    
    public void testFullQuery() {
        String queryStringUri = "http://www.example2.com";
        innerTestQueryString(queryStringUri,queryStringUri);
    }

    public void testFullQueryEncoded() {
        String queryStringUri = "http%3A//www.example2.com/";
        String expectedUri = "http://www.example2.com/";
        innerTestQueryString(queryStringUri,expectedUri);
    }
    
    public void testFullQueryEncodedComplex() {
        String queryStringUri = "http%3A//www.example2.com/foo%3Fbar%3Dbz%26red%3Dblue";
        String expectedUri = "http://www.example2.com/foo?bar=bz&red=blue";
        innerTestQueryString(queryStringUri,expectedUri);
    }
    
    private void innerTestQueryString(String queryStringUri, String expectedUri) {
        UURI uuri = UURI.from(
                "http://www.example.com/foo?"+queryStringUri);
        innerTestForPresence(uuri, expectedUri);
    }

    private void innerTestForPresence(UURI uuri, String expectedUri) {
        List<String> results = ExtractorURI.extractQueryStringLinks(uuri);
        assertTrue(
                "URI not found: "+expectedUri,
                results.contains(expectedUri));
    }
    
    public void testParameterComplex() {
        String parameterUri = "http%3A//www.example2.com/foo%3Fbar%3Dbz%26red%3Dblue";
        String expectedUri = "http://www.example2.com/foo?bar=bz&red=blue";
        UURI uuri = UURI.from(
                "http://www.example.com/foo?uri="+parameterUri+"&foo=bar");
        innerTestForPresence(uuri,expectedUri);
    }
}
