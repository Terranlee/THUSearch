/* SinkHandlerLogRecord
 *
 * Created Aug 9, 2005
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

package org.archive.io;

import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.archive.crawler.framework.ToeThread;

/**
 * Version of LogRecord used by SinkHandler.
 * Adds being able to mark the LogRecord as already-read and timestamping time
 * of creation. Also adds a different {@link #toString()} implementation.
 * Delegates all other calls to the passed LogRecord.
 * @author stack
 * @version $Date: 2006-08-15 04:39:00 +0000 (Tue, 15 Aug 2006) $ $Version$
 */
public class SinkHandlerLogRecord extends LogRecord {
    private static final long serialVersionUID = -7782942650334713560L;
    boolean read = false;
    private final LogRecord delegatee;
    private final Date creationTime = new Date();
    private static final int SHORT_MSG_LENGTH = 80;
    
    protected SinkHandlerLogRecord() {
        this(null);
    }

    public SinkHandlerLogRecord(final LogRecord record) {
        super(record.getLevel(), record.getMessage());
        // if available, append current processor name to message
        // [ 1108006 ] alerts should show current processor
        // http://sourceforge.net/tracker/index.php?func=detail&aid=1108006&group_id=73833&atid=539102
        if(Thread.currentThread() instanceof ToeThread) {
            String newMessage = this.getMessage();
            ToeThread tt = (ToeThread) Thread.currentThread();
            newMessage = newMessage + " (in thread '"+tt.getName()+"'";
            if(tt.getCurrentProcessorName().length()>0) {
                newMessage = newMessage + "; in processor '"
                    +tt.getCurrentProcessorName() + "'";
            }
            newMessage = newMessage + ")";
            this.setMessage(newMessage);
        }
        this.delegatee = record;
    }
    
    public boolean equals(final long id) {
        return id == getSequenceNumber();
    }
    
    public boolean equals(final SinkHandlerLogRecord compare) {
        return equals(compare.getSequenceNumber());
    }
    
    public boolean isRead() {
        return this.read;
    }

    /**
     * Mark alert as seen (That is, isNew() no longer returns true).
     */
    public void setRead() {
        this.read = true;
    }
    
    /**
     * @return Time of creation
     */
    public Date getCreationTime() {
        return this.creationTime;
    }
    
    public Level getLevel() {
        return this.delegatee.getLevel();
    }
    
    public String getLoggerName() {
        return this.delegatee.getLoggerName();
    }
    
    public String getShortMessage() {
        String msg = getMessage();
        return msg == null || msg.length() < SHORT_MSG_LENGTH?
                msg: msg.substring(0, SHORT_MSG_LENGTH) + "...";
    }
    
    public Throwable getThrown() {
        return this.delegatee.getThrown();
    }
    
    public String getThrownToString() {
        StringWriter sw = new StringWriter();
        Throwable t = getThrown();
        if (t == null) {
            sw.write("No associated exception.");
        } else {
            String tStr = t.toString();
            sw.write(tStr);
            if (t.getMessage() != null && t.getMessage().length() > 0 &&
                    !tStr.endsWith(t.getMessage())) {
                sw.write("\nMessage: ");
                sw.write(t.getMessage());
            }
            if (t.getCause() != null) {
                sw.write("\nCause: ");
                t.getCause().printStackTrace(new java.io.PrintWriter(sw));
            }
            sw.write("\nStacktrace: ");
            t.printStackTrace(new java.io.PrintWriter(sw));
        }
        return sw.toString();
    }
    
    public String toString() {
        StringWriter sw = new StringWriter();
        sw.write(getLevel().toString());
        sw.write(" ");
        sw.write(getMessage());
        sw.write(getThrownToString());
        return sw.toString();
    }
}
