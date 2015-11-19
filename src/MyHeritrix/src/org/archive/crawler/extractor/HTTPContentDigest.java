/* HTTPContentDigest
 * 
 * $Id: HTTPContentDigest.java 4654 2006-09-25 20:19:54Z paul_jack $
 * 
 * Created on 5.1.2005
 *
 * Copyright (C) 2005 Kristinn Sigur?sson
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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.io.ReplayCharSequence;
import org.archive.util.Base32;
import org.archive.util.TextUtils;

/**
 * A processor for calculating custum HTTP content digests in place of the 
 * default (if any) computed by the HTTP fetcher processors.
 * <p>
 * This processor allows the user to specify a regular expression called 
 * <i>strip-reg-expr<i>. Any segment of a document (text only, binary files will
 * be skipped) that matches this regular expression will by rewritten with 
 * the blank character (character 32 in the ANSI character set) <b> for the 
 * purpose of the digest</b> this has no effect on the document for subsequent 
 * processing or archiving.
 * <p>
 * NOTE: Content digest only accounts for the document body, not headers.
 * <p>
 * The operator will also be able to specify a maximum length for documents 
 * being evaluated by this processors. Documents exceeding that length will be 
 * ignored.
 * <p>
 * To further discriminate by file type or URL, an operator should use the 
 * override and refinement options. 
 * <p>
 * It is generally recommended that this recalculation only be performed when 
 * absolutely needed (because of stripping data that changes automatically each 
 * time the URL is fetched) as this is an expensive operation.
 *
 * @author Kristinn Sigurdsson
 */
public class HTTPContentDigest extends Processor {

    private static final long serialVersionUID = 8055532198737384358L;

    private static Logger logger =
        Logger.getLogger(HTTPContentDigest.class.getName());

    /** A regular expression detailing elements to strip before making digest */
    public final static String ATTR_STRIP_REG_EXPR = "strip-reg-expr";
    protected final static String DEFAULT_STRIP_REG_EXPR = "";
    /** Maximum file size for - longer files will be ignored. -1 = unlimited*/
    public final static String ATTR_MAX_SIZE_BYTES = "max-size-bytes";
    protected final static Long DEFAULT_MAX_SIZE_BYTES = new Long(1048576); // 1 Megabyte
    
    private static final String SHA1 = "SHA1";

    
    /**
     * Constructor
     * @param name Processor name
     */
    public HTTPContentDigest(String name) {
        super(name, "Calculate custom - stripped - content digests. " +
                "A processor for calculating custom HTTP content digests " +
                "in place of the default (if any) computed by the HTTP " +
                "fetcher processors. " +
                "This processor enables you to specify a regular expression " +
                "called strip-reg-expr. Any segment of a document (text " +
                "only, binary files will be skipped) that matches this " +
                "regular expression will be rewritten with the blank " +
                "character (character 32 in the ANSI character set) FOR THE " +
                "PURPOSE OF THE DIGEST, this has no effect on the document " +
                "for subsequent processing or archiving. You can also " +
                "specify a maximum length for documents being evaluated by " +
                "this processor. Documents exceeding that length will be " +
                "ignored. " +
                "To further discriminate by file type or URL, you should use " +
                "the override and refinement options (the processor can be " +
                "disabled by default and only enabled as needed in overrides " +
                "and refinements. " +
                "It is generally recommended that this recalculation only be " +
                "performed when absolutely needed (because of stripping data " +
                "that changes automatically each time the URL is fetched) as " +
                "this is an expensive operation.");

        addElementToDefinition(new SimpleType(ATTR_STRIP_REG_EXPR,
                "A regular expression that matches those portions of " +
                "downloaded documents that need to be ignored when " +
                "calculating the content digest. " +
                "Segments matching this expression will be rewritten with " +
                "the blank character for the content digest.",
                DEFAULT_STRIP_REG_EXPR));
        addElementToDefinition(new SimpleType(ATTR_MAX_SIZE_BYTES,
                "Maximum size of of documents to recalculate the digest for." +
                " Documents that exceed this value (bytes) will be ignored." +
                " Defaults to 1048576 bytes, or 1 MB. " +
                "-1 denotes unlimited size. A setting of 0 will effectively " +
                "disable the processor.",
                DEFAULT_MAX_SIZE_BYTES));
    }

    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        if (!curi.isHttpTransaction()){
            // Only handles HTTP docsuments.
            return;
        }
        if(!TextUtils.matches("^text.*$", curi.getContentType())){
            // Only handles text based documents.
            return;
        }
        long maxsize = DEFAULT_MAX_SIZE_BYTES.longValue(); 
        try {
            maxsize = ((Long)getAttribute(curi,ATTR_MAX_SIZE_BYTES)).longValue();
        } catch (AttributeNotFoundException e) {
            logger.severe("Missing max-size-bytes attribute when processing " + 
                    curi.toString());
        }
        if(maxsize < curi.getContentSize() && maxsize > -1){
            // Document too big.
            return;
        }
        
        // Ok, if we got this far we need to calculate the content digest. 
        // Get the regexpr
        String regexpr = "";
        try {
            regexpr = (String)getAttribute(curi,ATTR_STRIP_REG_EXPR);
        } catch (AttributeNotFoundException e2) {
            logger.severe("Missing strip-reg-exp when processing " +
                    curi.toString());
            return; // Can't do anything without it.
        }
        
        // Get a replay of the document character seq.
        ReplayCharSequence cs = null;
        
        try {
           cs = curi.getHttpRecorder().getReplayCharSequence();
        } catch(Exception e) {
            curi.addLocalizedError(this.getName(), e,
                "Failed get of replay char sequence " + curi.toString() +
                    " " + e.getMessage());
            logger.warning("Failed get of replay char sequence " +
                curi.toString() + " " + e.getMessage() + " " +
                Thread.currentThread().getName());
            return; // Can't proceed if this happens.
        }
        
        // Create a MessageDigest 
        MessageDigest digest = null;
        
        // We have a ReplayCharSequence open.  Wrap all in finally so we
        // for sure close it before we leave.
        try {
            try {
                digest = MessageDigest.getInstance(SHA1);
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
                return;
            }

            digest.reset();

            String s = null;

            if (regexpr.length() == 0) {
                s = cs.toString();
            } else {
                // Process the document
                Matcher m = TextUtils.getMatcher(regexpr, cs);
                s = m.replaceAll(" ");
                TextUtils.recycleMatcher(m);
            }
            digest.update(s.getBytes());

            // Get the new digest value
            byte[] newDigestValue = digest.digest();

            // Log if needed.
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Recalculated content digest for "
                        + curi.toString() + " old: "
                        + Base32.encode((byte[]) curi.getContentDigest())
                        + ", new: " + Base32.encode(newDigestValue));
            }
            // Save new digest value
            curi.setContentDigest(SHA1, newDigestValue);
        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (IOException ioe) {
                    logger.warning(TextUtils.exceptionToString(
                            "Failed close of ReplayCharSequence.", ioe));
                }
            }
        }
    }
}