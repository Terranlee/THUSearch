/*
 * FilePatternFilterTest
 *
 * $Id: FilePatternFilterTest.java 4652 2006-09-25 18:41:10Z paul_jack $
 *
 * Created on Mar 11, 2004
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

package org.archive.crawler.filter;

import junit.framework.TestCase;

/**
 * Tests FilePatternFilter default pattern (all default file extension) and
 * separate subgroups patterns such as images, audio, video, and
 * miscellaneous groups.
 *
 * @author Igor Ranitovic
 * @deprecated  The tested class is deprecated, so this test 
 * will eventually go away
 */
public class FilePatternFilterTest extends TestCase {
        FilePatternFilter filter = new FilePatternFilter("File Pattern Filter");

    /**
     * Tests FilePatternFilter default pattern (all default file extension) and
     * separate subgroups patterns such as images, audio, video, and
     * miscellaneous groups.
     *
     */
    public final void testPatterns() {

       String stringURI = "http://foo.boo/moo.avi";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));

       stringURI = "http://foo.boo/moo.bmp";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));

       stringURI = "http://foo.boo/moo.doc";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.MISC_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.MISC_PATTERNS));

       stringURI = "http://foo.boo/moo.gif";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));

       stringURI = "http://foo.boo/moo.jpg";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));

       stringURI = "http://foo.boo/moo.jpeg";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));

       stringURI = "http://foo.boo/moo.mid";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));

       stringURI = "http://foo.boo/moo.mov";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));

       stringURI = "http://foo.boo/moo.mp2";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));

       stringURI = "http://foo.boo/moo.mp3";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));

       stringURI = "http://foo.boo/moo.mp4";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));

       stringURI = "http://foo.boo/moo.mpeg";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));

       stringURI = "http://foo.boo/moo.pdf";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.MISC_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.MISC_PATTERNS));

       stringURI = "http://foo.boo/moo.png";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));

       stringURI = "http://foo.boo/moo.ppt";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.MISC_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.MISC_PATTERNS));

       stringURI = "http://foo.boo/moo.ram";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));

       stringURI = "http://foo.boo/moo.rm";
       assertTrue(filter.accepts(stringURI));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));

       stringURI = "http://foo.boo/moo.smil";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));

       stringURI = "http://foo.boo/moo.swf";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.MISC_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.MISC_PATTERNS));

       stringURI = "http://foo.boo/moo.tif";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));

       stringURI = "http://foo.boo/moo.tiff";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.IMAGES_PATTERNS));

       stringURI = "http://foo.boo/moo.wav";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.AUDIO_PATTERNS));

       stringURI = "http://foo.boo/moo.wmv";
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));
       stringURI.toUpperCase();
       assertTrue(filter.accepts(stringURI));
       assertTrue(stringURI.matches(FilePatternFilter.VIDEO_PATTERNS));

       stringURI = "http://foo.boo/moo.asf";
       assertFalse(filter.accepts(stringURI));
       assertFalse(stringURI.matches(FilePatternFilter.MISC_PATTERNS));
       stringURI.toUpperCase();
       assertFalse(filter.accepts(stringURI));
       assertFalse(stringURI.matches(FilePatternFilter.MISC_PATTERNS));

    }
}
