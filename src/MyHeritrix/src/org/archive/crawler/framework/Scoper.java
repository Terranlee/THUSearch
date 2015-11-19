/* Scoper
 * 
 * Created on Jun 6, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.framework;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.util.LogUtils;

/**
 * Base class for Scopers.
 * Scopers test CandidateURIs against a scope.
 * Scopers allow logging of rejected CandidateURIs.
 * @author stack
 * @version $Date: 2006-09-25 23:59:43 +0000 (Mon, 25 Sep 2006) $, $Revision: 4664 $
 */
public abstract class Scoper extends Processor {
    private static Logger LOGGER =
        Logger.getLogger(Scoper.class.getName());
    
    /**
     * Protected so avaiilable to subclasses.
     */
    protected static final String ATTR_OVERRIDE_LOGGER_ENABLED =
        "override-logger";
    
    /**
     * Constructor.
     * @param name
     * @param description
     */
    public Scoper(String name, String description) {
        super(name, description);
        Type t = addElementToDefinition(
            new SimpleType(ATTR_OVERRIDE_LOGGER_ENABLED,
            "If enabled, override default logger for this class (Default " +
            "logger writes the console).  Override " +
            "logger will instead send all logging to a file named for this " +
            "class in the job log directory. Set the logging level and " +
            "other " +
            "characteristics of the override logger such as rotation size, " +
            "suffix pattern, etc. in heritrix.properties. This attribute " +
            "is only checked once, on startup of a job.",
            new Boolean(false)));
        t.setExpertSetting(true);
    }
    
    protected void initialTasks() {
        super.initialTasks();
        if (!isOverrideLogger(null)) {
            return;
        }
        // Set up logger for this instance.  May have special directives
        // since this class can log scope-rejected URLs.
        LogUtils.createFileLogger(getController().getLogsDir(),
            this.getClass().getName(),
            Logger.getLogger(this.getClass().getName()));
    }
    
    /**
     * @param context Context to use looking up attribute.
     * @return True if we are to override default logger (default logs
     * to console) with a logger that writes all loggings to a file
     * named for this class.
     */
    protected boolean isOverrideLogger(Object context) {
        boolean result = true;
        try {
            Boolean b = (Boolean)getAttribute(context,
                ATTR_OVERRIDE_LOGGER_ENABLED);
            if (b != null) {
                result = b.booleanValue();
            }
        } catch (AttributeNotFoundException e) {
            LOGGER.warning("Failed get of 'enabled' attribute.");
        }

        return result;
    }
    
    /**
     * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
     * @param caUri The CandidateURI to be scheduled.
     * @return true if CandidateURI was accepted by crawl scope, false
     * otherwise.
     */
    protected boolean isInScope(CandidateURI caUri) {
        boolean result = false;
        if (getController().getScope().accepts(caUri)) {
            result = true;
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Accepted: " + caUri);
            }
        } else {
            outOfScope(caUri);
        }
        return result;
    }
    
    /**
     * Called when a CandidateUri is ruled out of scope.
     * Override if you don't want logs as coming from this class.
     * @param caUri CandidateURI that is out of scope.
     */
    protected void outOfScope(CandidateURI caUri) {
        if (!LOGGER.isLoggable(Level.INFO)) {
            return;
        }
        LOGGER.info(caUri.getUURI().toString());
    }
}
