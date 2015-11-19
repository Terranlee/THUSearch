/*
 * MatchesFilePatternDecideRule
 *
 * $Id: MatchesFilePatternDecideRule.java 6110 2009-01-15 02:47:33Z nlevitt $
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

package org.archive.crawler.deciderules;

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.settings.SimpleType;

/**
 * Compares suffix of a passed CrawlURI, UURI, or String against a regular
 * expression pattern, applying its configured decision to all matches.
 *
 * Several predefined patterns are available for convenience. Choosing
 * 'custom' makes this the same as a regular MatchesRegExpDecideRule. 
 *
 * @author Igor Ranitovic
 */
public class MatchesFilePatternDecideRule extends MatchesRegExpDecideRule {

    private static final long serialVersionUID = -4182743018517062411L;

    private static final Logger logger =
        Logger.getLogger(MatchesFilePatternDecideRule.class.getName());
    public static final String ATTR_USE_PRESET = "use-preset-pattern";
    public static final String IMAGES_PATTERNS = 
        ".*(?i)(\\.(bmp|gif|jpe?g|png|svg|tiff?))$";
    public static final String AUDIO_PATTERNS = 
        ".*(?i)(\\.(aac|aiff?|m3u|m4a|midi?|mp2|mp3|mp4|mpa|ogg|ra|ram|wav|wma))$";
    public static final String VIDEO_PATTERNS = 
        ".*(?i)(\\.(asf|asx|avi|flv|mov|mp4|mpeg|mpg|qt|ram|rm|smil|wmv))$";
    public static final String MISC_PATTERNS = 
        ".*(?i)(\\.(doc|pdf|ppt|swf))$";
    public static final String ALL_DEFAULT_PATTERNS = 
        ".*(?i)(\\.(bmp|gif|jpe?g|png|svg|tiff?|aac|aiff?|m3u|m4a|midi?|mp2" +
        "|mp3|mp4|mpa|ogg|ra|ram|wav|wma|asf|asx|avi|flv|mov|mp4|mpeg|mpg|qt" +
        "|ram|rm|smil|wmv|doc|pdf|ppt|swf))$";

    public static final String ALL = "All";
    public static final String IMAGES = "Images";
    public static final String AUDIO = "Audio";
    public static final String VIDEO = "Video";
    public static final String MISC = "Miscellaneous";
    public static final String CUSTOM = "Custom";

    /**
     * Usual constructor.
     * @param name
     */
    public MatchesFilePatternDecideRule(String name) {
        super(name);
        setDescription("MatchesFilePatternDecideRule. Applies its decision " +            "to all URIs that end with the specified pattern(s). Anything " +
            " that does not match is let PASS. " +
            " Default file patterns are: .avi, .bmp, " +
            ".doc, .gif, .jp(e)g, .mid, .mov, .mp2, .mp3, .mp4, .mpeg, " +
            ".pdf, .png, .ppt, .ram, .rm,.smil, .swf, .tif(f), .wav, .wmv. " +
            "It is also possible to specify a custom regular expression, " +
            "in which case this behaves exactly like the " +
            " MatchesRegExpDecideRule. See also " +
            "NotMatchesFilePatternDecideRule.");

        String[] options = new String[] {ALL, IMAGES, AUDIO, VIDEO, MISC,
            CUSTOM};

        addElementToDefinition(
            new SimpleType(ATTR_USE_PRESET, "URIs that match selected file " +
                "patterns will have the decision applied. Default file " +
                "patterns are:\n" +
                "Images: .bmp, .gif, .jp(e)g, .png, .tif(f)\n" +
                "Audio: .mid, mp2, .mp3, .mp4, .wav\n" +
                "Video: .avi, .mov, .mpeg, .ram, .rm, .smil, .wmv\n" +
                "Miscellaneous: .doc, .pdf, .ppt, .swf\n" +
                "All: All above patterns\n" + 
                "Choose 'Custom' to specify your own pattern. Preset " +
                "patterns are case insensitive.",
                "All", options));

        addElementToDefinition(
            new SimpleType(ATTR_REGEXP, "Custom java regular expression. " +
                    "This regular expression will be used instead of the " +
                    "supplied pattern groups for matching. An example " +
                    "of such a regular expression (Miscellaneous): " +
                    ".*(?i)(\\.(doc|pdf|ppt|swf))$ " +
                    "Any arbitrary regular expression may be entered and " +
                    "will be applied to the URI.", ""));
    }

    /**
     * Use a preset if configured to do so.
     * @param o Context
     * @return Regex to use.
     * 
     * @see org.archive.crawler.filter.URIRegExpFilter#getRegexp(Object)
     */
    protected String getRegexp(Object o) {
        try {
            String patternType = (String) getAttribute(o, ATTR_USE_PRESET);
            if (patternType.equals(ALL)) {
                return ALL_DEFAULT_PATTERNS;
            } else if (patternType.equals(IMAGES)) {
                return IMAGES_PATTERNS;
            } else if (patternType.equals(AUDIO)) {
                return AUDIO_PATTERNS;
            } else if (patternType.equals(VIDEO)) {
                return VIDEO_PATTERNS;
            } else if (patternType.equals(MISC)) {
                return MISC_PATTERNS;
            } else if (patternType.equals(CUSTOM)) {
                return super.getRegexp(o);
            } else {
                assert false : "Unrecognized pattern type " + patternType
                        + ". Should never happen!";
            }
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return null; // Basically the rule is inactive if this occurs.
    }
}
