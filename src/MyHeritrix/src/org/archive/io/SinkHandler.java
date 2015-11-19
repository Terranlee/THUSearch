/* SinkHandler
 * Copyright (C) 2005 Internet Archive.
 *
 * Created on Jul 7, 2003
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
package org.archive.io;

import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;



/**
 * A handler that keeps an in-memory vector of all events deemed loggable by
 * configuration.
 * Use it to iterate over logged events long after their occurance. One such
 * use is as a sink for WARNING+SEVERE loggable events. Has support for
 * whether a log record has been already-read.
 * TODO: Add being able to get LogRecords by log level: i.e. return all
 * Level.SEVERE, etc.
 * TODO: Back the vector with a bdbje collection.
 * @author stack
 * @version $Date: 2006-09-22 17:23:04 +0000 (Fri, 22 Sep 2006) $ $Revision: 4646 $
 */
public class SinkHandler extends Handler {
    /**
     * Alerts that have occured.
     */
    private Vector<SinkHandlerLogRecord> sink
     = new Vector<SinkHandlerLogRecord>();
    
    public SinkHandler() {
        LogManager manager = LogManager.getLogManager();
        String className = getClass().getName();
        String tmp = manager.getProperty(className + ".level");
        if (tmp != null) {
            setLevel(Level.parse(tmp));
        }
    }
    
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        this.sink.add(new SinkHandlerLogRecord(record));
    }

    public void flush() {
        // Nothing to do.
    }

    public void close() throws SecurityException {
        flush();
    }
    
    /**
     * @return SinkHandler instance if one registered or null.
     */
    public static SinkHandler getInstance() {
        SinkHandler h = null;
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i] instanceof SinkHandler) {
                h = (SinkHandler) handlers[i];
                break;
            }
        }
        if (h == null) {
            // None setup automatically (Not found in heritrix.properties --
            // can happen when deployed in a containter such as tomcat).
            // Create one manually here.
            h = new SinkHandler();
            h.setLevel(Level.WARNING);
            Logger.getLogger("").addHandler(h);
        }
        return h;
    }
    
    /**
     * @return all SinkHandlerLogRecords.
     */
    public Vector getAll() {
        return this.sink;
    }
    
    /**
     * @return Return all unread SinkHandlerLogRecords or null if none unread
     */
    public Vector<SinkHandlerLogRecord> getAllUnread() {
        if (this.sink == null) {
            return null;
        }
        Vector<SinkHandlerLogRecord> newLogRecords;
        newLogRecords = new Vector<SinkHandlerLogRecord>();
        for (final Iterator i = this.sink.iterator(); i.hasNext();) {
            SinkHandlerLogRecord lr = (SinkHandlerLogRecord) i.next();
            if (!lr.isRead()) {
                newLogRecords.add(lr);
            }
        }
        return (newLogRecords.size() == 0) ? null : newLogRecords;
    }
    
    /**
     * @return Count of all records.
     */
    public int getCount() {
        return this.sink != null? this.sink.size(): 0;
    }
    
    /**
     * @return The count of unread log records.
     */
    public int getUnreadCount() {
        if (this.sink == null) {
            return 0;
        }
        int n = 0;
        for (final Iterator i = this.sink.iterator(); i.hasNext();) {
            SinkHandlerLogRecord lr = (SinkHandlerLogRecord)i.next();
            if (!lr.isRead()) {
                n++;
            }
        }
        return n;
    }

    /**
     * @param id The <code>sequenceNumber</code> ID of the log record to find.
     * @return A SinkHandlerLogRecord of the given ID or null if not found.
     */
    public SinkHandlerLogRecord get(long id) {
        if (this.sink == null) {
            return null;
        }
        for (final Iterator i = this.sink.iterator(); i.hasNext();) {
            SinkHandlerLogRecord lr = (SinkHandlerLogRecord)i.next();
            if (lr.getSequenceNumber() == id) {
                return lr;
            }
        }
        return null;
    }

    /**
     * @param id The <code>sequenceNumber</code> ID of the log record to find.
     * @return The removed SinkHandlerLogRecord or null if none removed.
     */
    public SinkHandlerLogRecord remove(final long id) {
        SinkHandlerLogRecord shlr = null;
        if (this.sink == null) {
            return  shlr;
        }
        for (final Iterator i = this.sink.iterator(); i.hasNext();) {
            SinkHandlerLogRecord lr = (SinkHandlerLogRecord)i.next();
            if (lr.getSequenceNumber() == id) {
                i.remove();
                shlr = lr;
                break;
            }
        }
        return shlr;
    }

    /**
     * @param id The <code>sequenceNumber</code> ID of the log record to find
     * and mark as read.
     */
    public void read(final long id) {
        for (final Iterator i = this.sink.iterator(); i.hasNext();) {
            SinkHandlerLogRecord lr = (SinkHandlerLogRecord)i.next();
            if (lr.getSequenceNumber() == id) {
                lr.setRead();
                break;
            }
        }
    }
}
