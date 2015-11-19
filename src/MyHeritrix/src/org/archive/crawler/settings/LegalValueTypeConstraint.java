/*
 * legalValueListConstraint
 *
 * $Id: LegalValueTypeConstraint.java 3666 2005-07-06 19:23:52Z stack-sf $
 *
 * Created on Mar 30, 2004
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

/**
 * A constraint that checks that an attribute value is of the right type
 *
 * @author John Erik Halse
 */
public class LegalValueTypeConstraint
extends Constraint implements Serializable {
    private static final long serialVersionUID = 6106774072922858976L;

    /**
     * Constructs a new LegalValueListConstraint.
     *
     * @param level the severity level.
     * @param msg the default error message.
     */
    public LegalValueTypeConstraint(Level level, String msg) {
        super(level, msg);
    }

    /**
     * Constructs a new LegalValueListConstraint using default severity level
     * ({@link Level#WARNING}).
     *
     * @param msg the default error message.
     */
    public LegalValueTypeConstraint(String msg) {
        this(Level.SEVERE, msg);
    }

    /**
     * Constructs a new LegalValueListConstraint using default error message.
     *
     * @param level
     */
    public LegalValueTypeConstraint(Level level) {
        this(level, "Value of illegal type: ''{3}'', ''{4}'' was expected.");
    }

    /**
     * Constructs a new LegalValueListConstraint using default severity level
     * ({@link Level#WARNING}) and default error message.
     *
     */
    public LegalValueTypeConstraint() {
        this(Level.SEVERE);
    }

    public FailedCheck innerCheck(CrawlerSettings settings, ComplexType owner,
            Type definition, Object value) {
        FailedCheck res = null;

        // Check that the value is of right type
        if (!definition.getLegalValueType().isInstance(value)) {
            res = new FailedCheck(settings, owner, definition, value);
            res.messageArguments.add((value != null)?
                value.getClass().getName(): "null");
            res.messageArguments.add(definition.getLegalValueType().getName());
        }
        return res;
    }
}