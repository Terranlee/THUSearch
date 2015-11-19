/* JmxUtilsTest.java
 *
 * $Id: JmxUtilsTest.java 4644 2006-09-20 22:40:21Z paul_jack $
 *
 * Created Dec 7, 2005
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
package org.archive.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;

import junit.framework.TestCase;

public class JmxUtilsTest extends TestCase {
    public void testCreateCompositeType() throws OpenDataException {
        Map<String,Object> m =  new HashMap<String,Object>();
        m.put("0", new Long(0));
        m.put("1", new Double(1));
        m.put("2", "2");
        CompositeType ct = JmxUtils.createCompositeType(m, "ct", "description");
        testCompositeDataHasMapContent(ct, m);
        // Now mess with the order.
        Map<String,Object> n = new HashMap<String,Object>();
        n.put("0", new Long(17));
        n.put("2", "Some old string");
        n.put("1", new Double(17.45));
        testCompositeDataHasMapContent(ct, n);
    }

    private void testCompositeDataHasMapContent(final CompositeType ct,
            final Map m)
    throws OpenDataException {
        CompositeData cd = new CompositeDataSupport(ct, m);
        for (final Iterator i = m.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            assertTrue(cd.containsKey(key));
            assertEquals(m.get(key), cd.get(key));
        }
    }

}
