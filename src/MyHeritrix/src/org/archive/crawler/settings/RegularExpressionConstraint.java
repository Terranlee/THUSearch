/*
 * RegularExpressionConstraint
 *
 * $Id: RegularExpressionConstraint.java 3666 2005-07-06 19:23:52Z stack-sf $
 *
 * Created on Mar 31, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or any later version.
 *
 * Heritrix is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along with
 * Heritrix; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.archive.crawler.settings;

import java.io.Serializable;
import java.util.logging.Level;

import org.archive.util.TextUtils;

/**
 * A constraint that checks that a value matches a regular expression. This
 * constraint can only be applied to textual attributes.
 *
 * @author John Erik Halse
 */
public class RegularExpressionConstraint
extends Constraint implements Serializable {
    private static final long serialVersionUID = -5916211981136071809L;
    private final String pattern;

    /**
     * Constructs a new RegularExpressionConstraint.
     *
     * @param pattern the regular expression pattern the value must match.
     * @param level the severity level.
     * @param msg the default error message.
     */
    public RegularExpressionConstraint(String pattern, Level level, String msg) {
        super(level, msg);
        this.pattern = pattern;
    }

    /**
     * Constructs a new RegularExpressionConstraint using default severity level
     * ({@link Level#WARNING}).
     *
     * @param pattern the regular expression pattern the value must match.
     * @param msg the default error message.
     */
    public RegularExpressionConstraint(String pattern, String msg) {
        this(pattern, Level.WARNING, msg);
    }

    /**
     * Constructs a new RegularExpressionConstraint using the default error
     * message.
     *
     * @param pattern the regular expression pattern the value must match.
     * @param level the severity level.
     */
    public RegularExpressionConstraint(String pattern, Level level) {
        this(pattern, level, "Value did not match pattern: \"" + pattern + "\"");
    }

    /**
     * Constructs a new RegularExpressionConstraint.
     *
     * @param pattern the regular expression pattern the value must match.
     */
    public RegularExpressionConstraint(String pattern) {
        this(pattern, Level.WARNING);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.archive.crawler.settings.Constraint#innerCheck(org.archive.crawler.settings.Type,
     *      javax.management.Attribute)
     */
    public FailedCheck innerCheck(CrawlerSettings settings, ComplexType owner,
            Type definition, Object value) {
        if (value instanceof CharSequence) {
            if (!TextUtils
                    .matches(pattern, (CharSequence) value)) {
                return new FailedCheck(settings, owner, definition, value);

            }
        } else {
            return new FailedCheck(settings, owner, definition, value,
                    "Can't do regexp on non CharSequence.");
        }
        return null;
    }

}