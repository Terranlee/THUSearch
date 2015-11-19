/* SimpleType
 *
 * $Id: SimpleType.java 4661 2006-09-25 23:11:16Z paul_jack $
 *
 * Created on Jan 8, 2004
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
package org.archive.crawler.settings;

/**
 * A type that holds a Java type.
 *
 * @author John Erik Halse
 *
 */
public class SimpleType extends Type {

    private static final long serialVersionUID = -5134952907004648419L;

    private final String description;
    private Object[] legalValues = null;

    /**
     * Create a new instance of SimpleType.
     *
     * @param name the name of the type.
     * @param description a description suitable for the UI.
     * @param defaultValue the default value for this type. This also set what
     *            kind of Java type that this Type can hold.
     */
    public SimpleType(String name, String description, Object defaultValue) {
        super(name, defaultValue);
        this.description = description;
    }

    /**
     * Create a new instance of SimpleType.
     *
     * @param name the name of the type.
     * @param description a description suitable for the UI.
     * @param defaultValue the default value for this type. This also set what
     *            kind of Java type that this Type can hold.
     * @param legalValues an array of legal values for this simple type. The
     *            objects in this array must be of the same type as the default
     *            value.
     */
    public SimpleType(String name, String description, Object defaultValue,
            Object[] legalValues) {
        this(name, description, defaultValue);
        setLegalValues(legalValues);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.Type#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.Type#getDefaultValue()
     */
    public Object getDefaultValue() {
        return getValue();
    }

    /**
     * Get the array of legal values for this Type.
     */
    public Object[] getLegalValues() {
        return legalValues;
    }

    /**
     * Set the array of legal values for this type.
     * <p>
     *
     * The objects in this array must be of the same type as the default value.
     *
     * @param legalValues
     */
    public void setLegalValues(Object[] legalValues) {
        this.legalValues = legalValues;
        if (legalValues != null) {
            addConstraint(new LegalValueListConstraint());
        }
    }
}
