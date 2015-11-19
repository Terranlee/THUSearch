/*
 * ExtractorURITest
 *
 * $Id: ExtractorImpliedURITest.java 4667 2006-09-26 20:38:48Z paul_jack $
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


import junit.framework.TestCase;

/**
 * Test ExtractorImpliedURI
 * 
 * @author gojomo
 */
public class ExtractorImpliedURITest extends TestCase {
    
    public void testYouTubeExample() {
        String startUri = 
            "http://youtube.com/player2.swf?video_id=pv5zWaTEVkI&l=184&t=OEgsToPDskJrxamAv3Xm6ykQPSaw_f-Q&nc=16763904";
        String expectedUri = 
            "http://youtube.com/get_video?video_id=pv5zWaTEVkI&l=184&t=OEgsToPDskJrxamAv3Xm6ykQPSaw_f-Q&nc=16763904";
        // without escaping: ^(http://[\w\.:@]*)/player2.swf\?(.*)$
        String triggerPattern = "^(http://[\\w\\.:@]*)/player2.swf\\?(.*)$";
        String buildPattern = "$1/get_video?$2";
        
        String implied = ExtractorImpliedURI.extractImplied(
                startUri,triggerPattern,buildPattern);
        assertEquals(expectedUri,implied);
    }
}
