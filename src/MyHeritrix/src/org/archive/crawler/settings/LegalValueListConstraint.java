/*
 * legalValueListConstraint
 *
 * $Id: LegalValueListConstraint.java 4661 2006-09-25 23:11:16Z paul_jack $
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

import java.util.logging.Level;

/**
 * A constraint that checks that an attribute value matches one of the items in
 * the list of legal values.
 *
 * @author John Erik Halse
 */
public class LegalValueListConstraint extends Constraint {

    private static final long serialVersionUID = -4293290799574408033L;

    /**
     * Constructs a new LegalValueListConstraint.
     *
     * @param level the severity level.
     * @param msg the default error message.
     */
    public LegalValueListConstraint(Level level, String msg) {
        super(level, msg);
    }

    /**
     * Constructs a new LegalValueListConstraint using default severity level
     * ({@link Level#WARNING}).
     *
     * @param msg the default error message.
     */
    public LegalValueListConstraint(String msg) {
        this(Level.WARNING, msg);
    }

    /**
     * Constructs a new LegalValueListConstraint using default error message.
     *
     * @param level
     */
    public LegalValueListConstraint(Level level) {
        this(level, "Value not in legal values list");
    }

    /**
     * Constructs a new LegalValueListConstraint using default severity level
     * ({@link Level#WARNING}) and default error message.
     *
     */
    public LegalValueListConstraint() {
        this(Level.WARNING);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.archive.crawler.settings.Constraint#innerCheck(org.archive.crawler.settings.Type,
     *      java.lang.Object)
     */
    public FailedCheck innerCheck(CrawlerSettings settings, ComplexType owner,
            Type definition,
            Object value) {
        FailedCheck res = null;

        // If this attribute is constrained by a list of legal values,
        // check that the value is in that list
        Object legalValues[] = definition.getLegalValues();
        if (legalValues != null) {
            boolean found = false;
            for (int i = 0; i < legalValues.length && !found; i++) {
                if (legalValues[i].equals(value)) {
                    found = true;
                }
            }
            if (!found) {
                res = new FailedCheck(settings, owner, definition, value);
            }
        }
        return res;
    }

}