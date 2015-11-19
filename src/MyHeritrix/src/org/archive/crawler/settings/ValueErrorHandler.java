/*
 * ValueErrorHandler
 *
 * $Id: ValueErrorHandler.java 3760 2005-08-15 23:35:10Z stack-sf $
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

/**
 * If a ValueErrorHandler is registered with a {@link SettingsHandler}, only
 * constraints with level {@link java.util.logging.Level#SEVERE} will throw an
 * {@link javax.management.InvalidAttributeValueException}.
 *
 * The ValueErrorHandler will recieve a notification for all failed checks
 * with level equal or greater than the error reporting level.
 *
 * @author John Erik Halse
 */
public interface ValueErrorHandler {
    public void handleValueError(Constraint.FailedCheck error);
}
