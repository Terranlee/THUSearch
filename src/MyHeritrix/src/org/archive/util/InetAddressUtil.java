/* InetAddressUtil
 * 
 * Created on Nov 19, 2004
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
package org.archive.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InetAddress utility.
 * @author stack
 * @version $Date: 2009-08-27 23:45:31 +0000 (Thu, 27 Aug 2009) $, $Revision: 6477 $
 */
public class InetAddressUtil {
    private static Logger logger =
        Logger.getLogger(InetAddressUtil.class.getName());
    
    /**
     * ipv4 address.
     */
    public static Pattern IPV4_QUADS = Pattern.compile(
        "([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})");
    
    private InetAddressUtil () {
        super();
    }
    
    /**
     * Returns InetAddress for passed <code>host</code> IF its in
     * IPV4 quads format (e.g. 128.128.128.128).
     * <p>TODO: Move to an AddressParsingUtil class.
     * @param host Host name to examine.
     * @return InetAddress IF the passed name was an IP address, else null.
     */
    public static InetAddress getIPHostAddress(String host) {
        InetAddress result = null;
        Matcher matcher = IPV4_QUADS.matcher(host);
        if (matcher == null || !matcher.matches()) {
            return result;
        }
        try {
            // Doing an Inet.getByAddress() avoids a lookup.
            result = InetAddress.getByAddress(host,
                    new byte[] {
                    (byte)(new Integer(matcher.group(1)).intValue()),
                    (byte)(new Integer(matcher.group(2)).intValue()),
                    (byte)(new Integer(matcher.group(3)).intValue()),
                    (byte)(new Integer(matcher.group(4)).intValue())});
        } catch (NumberFormatException e) {
            logger.warning(e.getMessage());
        } catch (UnknownHostException e) {
            logger.warning(e.getMessage());
        }
        return result;
    }
    
    /**
     * @return All known local names for this host or null if none found.
     */
    public static List<String> getAllLocalHostNames() {
        List<String> localNames = new ArrayList<String>();
        Enumeration e = null;
        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch(SocketException exception) {
            throw new RuntimeException(exception);
        }
        for (; e.hasMoreElements();) {
            for (Enumeration ee =
                ((NetworkInterface)e.nextElement()).getInetAddresses();
                    ee.hasMoreElements();) {
                InetAddress ia = (InetAddress)ee.nextElement();
                if (ia != null) {
                    if (ia.getHostName() != null) {
                        localNames.add(ia.getCanonicalHostName());
                    }
                    if (ia.getHostAddress() !=  null) {
                        localNames.add(ia.getHostAddress());
                    }
                }
            }
        }
        final String localhost = "localhost";
        if (!localNames.contains(localhost)) {
            localNames.add(localhost);
        }
        final String localhostLocaldomain = "localhost.localdomain";
        if (!localNames.contains(localhostLocaldomain)) {
            localNames.add(localhostLocaldomain);
        }
        return localNames;
    }
}