/* JndiUtils.java
 *
 * Created Aug 11, 2005
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

/**
 * JNDI utilities.
 * @author stack
 * @version $Date: 2005-10-27 22:20:20 +0000 (Thu, 27 Oct 2005) $ $Version$
 */
public class JndiUtils {
    /**
     * Syntax that will work with jmx ObjectNames (i.e. will escape '.' and
     * will add treat ',' and '=' specially.
     */
    private static final Properties COMPOUND_NAME_SYNTAX = new Properties();
    static {
        COMPOUND_NAME_SYNTAX.put("jndi.syntax.direction", "left_to_right");
        COMPOUND_NAME_SYNTAX.put("jndi.syntax.separator", "+");
        COMPOUND_NAME_SYNTAX.put("jndi.syntax.ignorecase", "false");
        COMPOUND_NAME_SYNTAX.put("jndi.syntax.escape", "\\");
        COMPOUND_NAME_SYNTAX.put("jndi.syntax.beginquote", "'");
        COMPOUND_NAME_SYNTAX.put("jndi.syntax.trimblanks", "true");
        COMPOUND_NAME_SYNTAX.put("jndi.syntax.separator.ava", ",");
        COMPOUND_NAME_SYNTAX.put("jndi.syntax.separator.typeval", "=");
    }
    
    public static CompoundName getCompoundName(final String name)
    throws InvalidNameException {
        return new CompoundName(name, COMPOUND_NAME_SYNTAX);
    }
    
    /**
     * Return name to use as jndi name.
     * Used to do a subset of the ObjectName fields but not just
     * let all through so its easy to just use the jndi name to 
     * find mbean.
     * @param on ObjectName instance to work with.
     * @return Returns a compound name to use as jndi key.
     * @throws NullPointerException
     * @throws InvalidNameException
     */
    public static CompoundName getCompoundName(final ObjectName on)
    throws NullPointerException,
    InvalidNameException {
        return getCompoundName(on.getCanonicalKeyPropertyListString());
    }
    
    /**
     * @param on ObjectName instance to work with.
     * @return A simple reference based on passed <code>on</code>
     */
    public static Reference getReference(final ObjectName on) {
       Reference r = new Reference(String.class.getName());
       Hashtable ht = on.getKeyPropertyList();
       r.add(new StringRefAddr(JmxUtils.HOST, (String)ht.get(JmxUtils.HOST)));
       r.add(new StringRefAddr(JmxUtils.NAME, (String)ht.get(JmxUtils.NAME)));
       // Put in a value to serve as a unique 'key'.
       r.add(new StringRefAddr(JmxUtils.KEY,
               on.getCanonicalKeyPropertyListString()));
       return r;
    }
    
    /**
     * Get subcontext.  Only looks down one level.
     * @param subContext Name of subcontext to return.
     * @return Sub context.
     * @throws NamingException 
     */
    public static Context getSubContext(final String subContext)
    throws NamingException {
        return getSubContext(getCompoundName(subContext));
    }
    
    /**
     * Get subcontext.  Only looks down one level.
     * @param subContext Name of subcontext to return.
     * @return Sub context.
     * @throws NamingException 
     */
    public static Context getSubContext(final CompoundName subContext)
    throws NamingException {
        Context context = new InitialContext();
        try {
            context = (Context)context.lookup(subContext);
        } catch (NameNotFoundException e) {
            context = context.createSubcontext(subContext); 
        }
        return context;
    }
    
    /**
     * 
     * @param context A subcontext named for the <code>on.getDomain()</code>
     * (Assumption is that caller already setup this subcontext).
     * @param on The ObjectName we're to base our bind name on.
     * @return Returns key we used binding this ObjectName.
     * @throws NamingException
     * @throws NullPointerException
     */
    public static CompoundName bindObjectName(Context context,
            final ObjectName on)
    throws NamingException, NullPointerException {
        // I can't call getNameInNamespace in tomcat. Complains about
        // unsupported operation -- that I can't get absolute name.
        // Therefore just skip this test below -- at least for now.
        // Check that passed context has the passed ObjectNames' name.
        //
//        String name = getCompoundName(context.getNameInNamespace()).toString();
//        if (!name.equals(on.getDomain())) {
//            throw new NamingException("The current context is " + name +
//                " but domain is " + on.getDomain() + " (Was expecting " +
//                "them to be the same).");
//        }
        CompoundName key = getCompoundName(on);
        context.rebind(key, getReference(on));
        return key;
    }
    
    public static CompoundName unbindObjectName(final Context context,
            final ObjectName on)
    throws NullPointerException, NamingException {
        CompoundName key = getCompoundName(on);
        context.unbind(key);
        return key;
    }


    /**
     * Testing code.
     * @param args Command line arguments.
     * @throws NullPointerException 
     * @throws MalformedObjectNameException 
     * @throws NamingException 
     * @throws InvalidNameException 
     */
    public static void main(String[] args)
    throws MalformedObjectNameException, NullPointerException,
    InvalidNameException, NamingException {
        final ObjectName on = new ObjectName("org.archive.crawler:" +
                "type=Service,name=Heritrix00,host=debord.archive.org");
        Context c = getSubContext(getCompoundName(on.getDomain()));
        CompoundName key = bindObjectName(c, on);
        Reference r = (Reference)c.lookup(key);
        for (Enumeration e = r.getAll(); e.hasMoreElements();) {
            System.out.println(e.nextElement());
        }
        unbindObjectName(c, on);
    }
}
