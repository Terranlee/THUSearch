/* ValueConstraint
 *
 * $Id: Constraint.java 5387 2007-08-09 16:46:13Z gojomo $
 *
 * Created on Mar 29, 2004
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

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.Level;


/**
 * Superclass for constraints that can be set on attribute definitions.
 * <p>
 * Constraints will be checked against attribute values. If a constraint check
 * fails, an object of type FailedCheck is returned containing information that
 * can be used to build meaningful information to the user.
 * <p>
 * A constraint has one of three levels:
 * <ul>
 * <li>{@link java.util.logging.Level#SEVERE}The attribute could not be set
 * whatsoever.
 * <li>{@link java.util.logging.Level#WARNING}The attribute is illegal i
 * CrawlJobs, but could be set in profiles. Mostly used as holder value for
 * settings that should be changed for every entity running a crawl.
 * <li>{@link java.util.logging.Level#INFO}The attribute has a legal value,
 * but is outside the bounds of what are considered a reasonable value. The user
 * could be warned that she should investigate if the value actally is what she
 * wants it be.
 * </ul>
 *
 * @author John Erik Halse
 */
public abstract class Constraint 
implements Comparable<Constraint>, Serializable {
    static final long serialVersionUID = -646814290764700497L;
    
    private final Level severity;
    private final String msg;

    /** Constructs a new Constraint.
     *
     * @param level the level for this constraint.
     * @param msg default message to return if the check fails.
     */
    public Constraint(Level level, String msg) {
        if (level != Level.SEVERE && level != Level.WARNING
                && level != Level.INFO) {
            throw new IllegalArgumentException("Illegal level: "
                    + level.getName());
        }
        this.severity = level;
        this.msg = msg;
    }

    /**
     * Run the check.
     *
     * @param owner the ComplexType owning the attribute to check.
     * @param definition the definition to check the attribute against.
     * @param value the value to check.
     * @return null if ok, or an instance of {@link FailedCheck}if the check
     *         failed.
     */
    public final FailedCheck check(CrawlerSettings settings, ComplexType owner,
            Type definition, Object value) {
        return innerCheck(settings, owner, definition, value);
    }

    /** The method all subclasses should implement to do the actual checking.
     *
     * @param owner the ComplexType owning the attribute to check.
     * @param definition the definition to check the attribute against.
     * @param value the value to check.
     * @return null if ok, or an instance of {@link FailedCheck}if the check
     *         failed.
     */
    public abstract FailedCheck innerCheck(CrawlerSettings settings,
            ComplexType owner, Type definition, Object value);

    /** Get the default message to return if a check fails.
     *
     * @return the default message to return if a check fails.
     */
    protected String getDefaultMessage() {
        return msg;
    }

    /** Objects of this class represents failed constraint checks.
     *
     * @author John Erik Halse
     */
    public class FailedCheck {
        private final String msg;
        private final CrawlerSettings settings;
        private final ComplexType owner;
        private final Type definition;
        private final Object value;
        protected final ArrayList<Object> messageArguments
         = new ArrayList<Object>();

        /**
         * Construct a new FailedCheck object.
         *
         * @param settings the CrawlerSettings object for which this check was
         *            executed.
         * @param owner the ComplexType owning the attribute to check.
         * @param definition the definition to check the attribute against.
         * @param value the value to check.
         * @param msg a message describing what went wrong and possibly hints to
         *            the user on how to fix it.
         */
        public FailedCheck(CrawlerSettings settings, ComplexType owner,
                Type definition, Object value, String msg) {
            this.msg = msg;
            this.settings = settings;
            this.owner = owner;
            this.definition = definition;
            this.value = value;
            this.messageArguments.add(definition.getName());
            this.messageArguments.add(value);
            this.messageArguments.add(owner.getName());
        }

        /**
         * Construct a new FailedCheck object using the constraints default
         * message.
         *
         * @param settings the CrawlerSettings object for which this check was
         *            executed.
         * @param owner the ComplexType owning the attribute to check.
         * @param definition the definition to check the attribute against.
         * @param value the value to check.
         */
        public FailedCheck(CrawlerSettings settings, ComplexType owner,
                Type definition, Object value) {
            this(settings, owner, definition, value, getDefaultMessage());
        }

        /** Get the error message.
         *
         * @return the error message.
         */
        public String getMessage() {
            return MessageFormat.format(msg, messageArguments.toArray());
        }

        /** Get the severity level.
         *
         * @return the severity level.
         */
        public Level getLevel() {
            return severity;
        }

        /** Get the definition for the checked attribute.
         *
         * @return the definition for the checked attribute.
         */
        public Type getDefinition() {
            return definition;
        }

        /** Get the value of the checked attribute.
         *
         * @return the value of the checked attribute.
         */
        public Object getValue() {
            return value;
        }

        /** Get the {@link ComplexType} owning the checked attribute.
         *
         * @return the {@link ComplexType} owning the checked attribute.
         */
        public ComplexType getOwner() {
            return owner;
        }

        /** Get the {@link CrawlerSettings} for the checked attribute.
         *
         * @return the {@link CrawlerSettings} for the checked attribute.
         */
        public CrawlerSettings getSettings() {
            return settings;
        }

        /** Returns a human readeable string for the failed check.
         * Returns the same as {@link #getMessage()}
         *
         * @return A human readeable string for the failed check.
         */
        public String toString() {
            return getMessage();
        }
    }

    /** Compare this constraints level to another constraint.
     * This method is implemented to let constraints be sorted with the highest
     * level first.
     *
     * @param o a Constraint to compare to.
     */
    public int compareTo(Constraint o) {
        Constraint c = (Constraint) o;
        return c.severity.intValue() - severity.intValue();
    }

}
