/* SimpleTypeTest
 *
 * $Id: SimpleTypeTest.java 2168 2004-05-28 22:33:09Z stack-sf $
 *
 * Created on Apr 15, 2004
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

import junit.framework.TestCase;


/**
 * Testing of the SimpleType
 *
 * @author John Erik Halse
 */
public class SimpleTypeTest extends TestCase {
    public void testGetName() {
        SimpleType t1 = new SimpleType("a", "b", "c");
        assertEquals("a", t1.getName());
    }

    public void testGetDescription() {
        SimpleType t1 = new SimpleType("a", "b", "c");
        assertEquals("b", t1.getDescription());
    }

    public void testGetDefaultValue() {
        SimpleType t1 = new SimpleType("a", "b", "c");
        assertEquals("c", t1.getDefaultValue());
    }

    public void testGetLegalValues() {
        SimpleType t1 = new SimpleType("a", "b", "c", new String[] {"d", "e"});
        checkArray(new String[] {"d", "e"}, t1.getLegalValues());
    }

    public void testSetLegalValues() {
        SimpleType t1 = new SimpleType("a", "b", "c", new String[] {"d", "e"});
        t1.setLegalValues(new String[] {"f", "g"});
        checkArray(new String[] {"f", "g"}, t1.getLegalValues());
    }

    public void testGetConstraints() {
        SimpleType t1 = new SimpleType("a1", "b1", "c1");
        SimpleType t2 = new SimpleType("a2", "b2", "c2", new String[] {"d", "e"});
        assertNotNull(t1.getConstraints());
        assertSame(LegalValueTypeConstraint.class, t2.getConstraints().get(0)
                .getClass());
        assertSame(LegalValueListConstraint.class, t2.getConstraints().get(1)
                .getClass());
    }

    public void testGetLegalValueType() {
        SimpleType t1 = new SimpleType("a1", "b1", "c1");
        SimpleType t2 = new SimpleType("a2", "b2", new Integer(1));
        SimpleType t3 = new SimpleType("a3", "b3", new TextField("c3"));
        assertSame(String.class, t1.getLegalValueType());
        assertSame(Integer.class, t2.getLegalValueType());
        assertSame(TextField.class, t3.getLegalValueType());
    }

    public void testEquals() {
        SimpleType t1 = new SimpleType("a1", "b1", "c1");
        SimpleType t2 = new SimpleType("a1", "b1", "c1");
        SimpleType t3 = new SimpleType("a2", "b2", "c2");
        assertTrue(t1.equals(t2));
        assertFalse(t1.equals(t3));
        assertTrue(t1.equals(t1));
        assertFalse(t1.equals(null));
    }

    private void checkArray(Object a1[], Object a2[]) {
        assertEquals("Arrays not of same length.", a1.length, a2.length);
        for (int i = 0; i < a1.length; i++) {
            assertEquals(a1[i], a2[i]);
        }
    }
}
