/* AlertManager.java
 *
 * Created Aug 4, 2005
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

import java.util.Vector;

import org.archive.io.SinkHandlerLogRecord;


/**
 * Manager for application alerts.
 * An alert is a message to a human operator created by Heritrix when
 * exceptional conditions.
 * @author stack
 * @version $Date: 2006-09-25 23:59:43 +0000 (Mon, 25 Sep 2006) $ $Revision: 4664 $
 */
public interface AlertManager {
    /**
     * @param record The new alert to add.
     */
    public void add(final SinkHandlerLogRecord record);
    
    /**
     * @param alertID the ID of the alert to remove.
     */
    public void remove(final String alertID);

    /**
     * @param alertID The ID of the alert to return.
     * @return an alert with the given ID or null if none found.
     */
    public SinkHandlerLogRecord get(final String alertID);

    /**
     * @return All current alerts
     */
    public Vector getAll();

    /**
     * @return Vector of all new alerts.
     */
    public Vector getNewAll();

    /**
     * @return The number of alerts
     */
    public int getCount();

    /**
     * @return The number of new alerts
     */
    public int getNewCount();
    
    /**
     * @param alertID of the ID of the alert to mark as 'seen'.
     */
    public void read(final String alertID);
}
