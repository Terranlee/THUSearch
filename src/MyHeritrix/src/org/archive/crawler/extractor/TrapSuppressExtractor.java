/* Copyright (C) 2007 Internet Archive.
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
 *
 * Created on Mar 5, 2007
 *
 */
package org.archive.crawler.extractor;

import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;


/** 
 * Pseudo-extractor that suppresses link-extraction of likely trap pages,
 * by noticing when content's digest is identical to that of its 'via'. 
 *
 * @author gojomo
 *
 */
public class TrapSuppressExtractor extends Extractor implements CoreAttributeConstants {
    private static final long serialVersionUID = -1028783453022579530L;

    private static final Logger LOGGER =
        Logger.getLogger(TrapSuppressExtractor.class.getName());

    /** ALIst attribute key for carrying-forward content-digest from 'via'*/
    public static String A_VIA_DIGEST = "via-digest";
    
    protected long numberOfCURIsHandled = 0;
    protected long numberOfCURIsSuppressed = 0;

    /**
     * Usual constructor. 
     * @param name
     */
    public TrapSuppressExtractor(String name) {
        super(name, "TrapSuppressExtractor. Prevent extraction of likely " +
                "trap content.");
    }

    @Override
    protected void initialTasks() {
        super.initialTasks();
    }

    protected void extract(CrawlURI curi){
        numberOfCURIsHandled++;

        String currentDigest = curi.getContentDigestSchemeString();
        String viaDigest = null;
        if(curi.containsKey(A_VIA_DIGEST)) {
            viaDigest = curi.getString(A_VIA_DIGEST);
        }
        
        if(currentDigest!=null) {
            if(currentDigest.equals(viaDigest)) {
                // mark as already-extracted -- suppressing further extraction
                curi.linkExtractorFinished();
                curi.addAnnotation("trapSuppressExtractor");
                numberOfCURIsSuppressed++;
            }
            // already consulted; so clobber with current value to be 
            // inherited
            curi.putString(A_VIA_DIGEST, currentDigest);
            curi.makeHeritable(A_VIA_DIGEST);
        }
    }

    /**
     * Provide a human-readable textual summary of this Processor's state.
     *
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.TrapSuppressExtractor\n");
        ret.append("  Function:             Suppress extraction on likely traps\n");
        ret.append("  CrawlURIs handled:    " + numberOfCURIsHandled + "\n");
        ret.append("  CrawlURIs suppressed: " + numberOfCURIsSuppressed + "\n\n");

        return ret.toString();
    }
}
