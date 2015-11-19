/* CrawlJobErrorHandler
 *
 * $Id: CrawlJobErrorHandler.java 4666 2006-09-26 17:53:28Z paul_jack $
 *
 * Created on Apr 2, 2004
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
package org.archive.crawler.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.archive.crawler.settings.ValueErrorHandler;
import org.archive.crawler.settings.Constraint.FailedCheck;


/**
 * An implementation of the ValueErrorHandler for the UI.
 *
 * <p>The UI uses this class to trap errors in the settings of it's jobs and
 * profiles and manage their presentation to the user.
 *
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.settings.ValueErrorHandler
 */
public class CrawlJobErrorHandler implements ValueErrorHandler {
    /** All encountered errors */
    HashMap<String,FailedCheck> errors = null;
    Level level = Level.INFO;
    Level highestEncounteredLevel = Level.OFF;

    public CrawlJobErrorHandler(){
        errors = new HashMap<String,FailedCheck>();
    }

    public CrawlJobErrorHandler(Level level){
        this();
        this.level = level;
    }

    public void handleValueError(FailedCheck error) {
        String key = error.getOwner().getAbsoluteName() +
            "/" + error.getDefinition().getName();
        errors.put(key,error);
        if(error.getLevel().intValue()>highestEncounteredLevel.intValue()){
            highestEncounteredLevel = error.getLevel();
        }
    }

    /**
     * Get error for a specific attribute.
     *
     * <p>Uses currently set error level
     *
     * @param absoluteName The absolute name of the attribute
     * @return error for a specific attribute at or above current error
     *           level. null if no matching error is found.
     */
    public FailedCheck getError(String absoluteName){
        return getError(absoluteName,level);
    }

    /**
     * Get error for a specific attribute
     * 
     * @param absoluteName
     *            The absolute name of the attribute.
     * @param level
     *            Limit errors to those at this or higher level.
     * @return error for a specific attribute at or above specified error level.
     *         null if no matching error is found.
     */
    public FailedCheck getError(String absoluteName, Level level) {
        FailedCheck fc = (FailedCheck) errors.get(absoluteName);
        if (fc != null && fc.getLevel().intValue() >= level.intValue()) {
            return fc;
        }
        return null;
    }

    /**
     * Has there been an error with severity (level) equal to or higher then
     * this handlers set level.
     * @return has there ben an error.
     */
    public boolean hasError(){
        return hasError(level);
    }

    /**
     * Has there been an error with severity (level) equal to or higher then
     * specified.
     * @param level The severity.
     * @return has there ben an error.
     */
    public boolean hasError(Level level){
        return highestEncounteredLevel.intValue() >= level.intValue();
    }

    /**
     * @return Returns the level.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @param level The level to set.
     */
    public void setLevel(Level level) {
        this.level = level;
    }

    /**
     * Reset handler.
     *
     * <p>Delets all encountered errors of any level.
     */
    public void clearErrors(){
        errors = new HashMap<String,FailedCheck>();
    }

    /**
     * Get an List of all the encountered errors.
     *
     * <p>The List contains a set of
     * {@link org.archive.crawler.settings.Constraint.FailedCheck
     * FailedCheck} objects.
     *
     * @return an list of all encountered errors (with level equal to
     *         or higher then current level).
     *
     * @see org.archive.crawler.settings.Constraint.FailedCheck
     */
    public List getErrors(){
        return getErrors(level);
    }

    /**
     * Get an List of all the encountered errors.
     *
     * <p>The List contains a set of
     * {@link org.archive.crawler.settings.Constraint.FailedCheck
     * FailedCheck} objects.
     *
     * @param level Get all errors of this level or higher
     *
     * @return an list of all encountered errors (with level equal to
     *         or higher then specified level).
     *
     * @see org.archive.crawler.settings.Constraint.FailedCheck
     */
    public List getErrors(Level level){
        ArrayList<FailedCheck> list = new ArrayList<FailedCheck>(errors.size());
        for (FailedCheck fc: errors.values()) {
            if(fc.getLevel().intValue() >= level.intValue()){
                list.add(fc);
            }
        }
        return list;
    }
}
