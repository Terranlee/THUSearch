/* SinkHandlerTest.java
 *
 * $Id: SinkHandlerTest.java 4929 2007-02-21 10:22:05Z gojomo $
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

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import junit.framework.TestCase;

public class SinkHandlerTest extends TestCase {
    private static final Logger LOGGER =
        Logger.getLogger(SinkHandlerTest.class.getName());
    
    protected void setUp() throws Exception {
        super.setUp();
        String logConfig = "handlers = " +
            "org.archive.io.SinkHandler\n" +
            "org.archive.io.SinkHandler.level = ALL";
        ByteArrayInputStream bais =
            new ByteArrayInputStream(logConfig.getBytes());
        LogManager.getLogManager().readConfiguration(bais);
    }
    
    public void testLogging() throws Exception {
        LOGGER.severe("Test1");
        LOGGER.severe("Test2");
        LOGGER.warning("Test3");
        RuntimeException e = new RuntimeException("Nothing exception");
        LOGGER.log(Level.SEVERE, "with exception", e);
        SinkHandler h = SinkHandler.getInstance();
        assertEquals(4, h.getAllUnread().size());
//        SinkHandlerLogRecord shlr = h.get(3);
//        h.remove(3);
        SinkHandlerLogRecord shlr = h.getAllUnread().get(3);
        h.remove(shlr.getSequenceNumber());
        assertEquals(3, h.getAllUnread().size());
        h.publish(shlr);
        assertEquals(4, h.getAllUnread().size());
    }
    /*
    public void testToString() throws Exception {
        RuntimeException e = new RuntimeException("Some-Message");
        LOGGER.log(Level.SEVERE, "With-Exception", e);
        SinkHandler h = SinkHandler.getInstance();
        System.out.print(((SeenLogRecord)h.getSink().get(0)).toString());
        LOGGER.log(Level.SEVERE, "No-Exception");
        System.out.print(((SeenLogRecord)h.getSink().get(1)).toString());
    }*/
}
