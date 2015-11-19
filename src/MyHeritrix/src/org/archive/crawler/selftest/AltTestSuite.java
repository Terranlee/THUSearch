/* AltTestSuite.java
 *
 * Created on Feb 20, 2007
 *
 * Copyright (C) 2007 Internet Archive.
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
package org.archive.crawler.selftest;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Variant TestSuite that can build tests including methods with an alternate
 * prefix (other than 'test'). Copies code from TestSuite because necessary
 * methods to change are private rather than protected. 
 *
 * @author gojomo
 * @version $Id: MaxLinkHopsSelfTest.java 4667 2006-09-26 20:38:48 +0000 (Tue, 26 Sep 2006) paul_jack $
 */
public class AltTestSuite extends TestSuite {
    /** a method prefix other than 'test' that is also recognized as tests */
    String altPrefix;
    
    /**
     * Constructs a TestSuite from the given class. Copied from superclass so
     * that local alternate addTestMethod() will be visible, which in turn uses
     * an isTestMethod() that accepts methods with the altPrefix in addition
     * to 'test'.
     * @param theClass Class from which to build suite
     * @param prefix alternate method prefix to also find test methods
     */
     public AltTestSuite(final Class theClass, String prefix) {
        this.altPrefix = prefix;
        setName(theClass.getName());
        try {
            getTestConstructor(theClass); // Avoid generating multiple error messages
        } catch (NoSuchMethodException e) {
            addTest(warning("Class "+theClass.getName()+" has no public constructor TestCase(String name) or TestCase()"));
            return;
        }

        if (!Modifier.isPublic(theClass.getModifiers())) {
            addTest(warning("Class "+theClass.getName()+" is not public"));
            return;
        }

        Class superClass= theClass;
        Vector names= new Vector();
        while (Test.class.isAssignableFrom(superClass)) {
            Method[] methods= superClass.getDeclaredMethods();
            for (int i= 0; i < methods.length; i++) {
                addTestMethod(methods[i], names, theClass);
            }
            superClass= superClass.getSuperclass();
        }
        if (testCount() == 0)
            addTest(warning("No tests found in "+theClass.getName()));
    }

    // copied from superclass
    private void addTestMethod(Method m, Vector names, Class theClass) {
        String name= m.getName();
        if (names.contains(name))
            return;
        if (! isPublicTestMethod(m)) {
            if (isTestMethod(m))
                addTest(warning("Test method isn't public: "+m.getName()));
            return;
        }
        names.addElement(name);
        addTest(createTest(theClass, name));
    }

    // copied from superclass
    private boolean isPublicTestMethod(Method m) {
        return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
     }
    
    // copied & extended from superclass
    private boolean isTestMethod(Method m) {
        String name= m.getName();
        Class[] parameters= m.getParameterTypes();
        Class returnType= m.getReturnType();
        return parameters.length == 0 
            && (name.startsWith("test")||name.startsWith(altPrefix)) 
            && returnType.equals(Void.TYPE);
     }
    
    /* filler constructor to avoid JUNit "no public constructor" warnings */
	public AltTestSuite() {
		super();
	}
	/* noop test to avoid "no tests found" warnings */
	public void testNoop() {
		
	}
}
