/* XmlUtils
 *
 * Created on Sep 19, 2007
 *
 * Copyright (C) 2007 Internet Archive.
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

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 * XML utilities for document/xpath actions. 
 *
 * @author gojomo
 * @version $Revision: 4644 $ $Date: 2006-09-20 22:40:21 +0000 (Wed, 20 Sep 2006) $
 */
public class XmlUtils {
    public static Logger logger =
        Logger.getLogger(XmlUtils.class.getName());

    /**
     * Parse a DOM Document from the given XML file. 
     * 
     * @param f File to parse as Document
     * @return Document
     * @throws IOException
     */
    public static Document getDocument(File f) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // never forget this!
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(f);
        } catch (ParserConfigurationException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        } catch (SAXException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }
    
    /**
     * Evaluate an XPath against a Document, returning a String.
     * 
     * @param doc Document
     * @param xp XPath to evaluate against Document
     * @return String found at path or null
     */
    public static String xpathOrNull(Document doc, String xp) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            XPathExpression expr = xpath.compile(xp);
            return expr.evaluate(doc);
        } catch (XPathExpressionException e) {
            return null;
        }
    }
}
