/* CookieUtils
 * 
 * Created on Oct 20, 2004
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
package org.archive.crawler.admin.ui;

import javax.servlet.http.Cookie;

/**
 * Utility methods for accessing cookies.
 * Used by the JSP UI pages.
 * @author stack
 * @version $Date: 2004-10-21 01:34:37 +0000 (Thu, 21 Oct 2004) $, $Revision: 2705 $
 */
public class CookieUtils {
    public static String getCookieValue(Cookie[] cookies, String cookieName,
            String defaultValue) {
        if(cookies != null) {
            for(int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if(cookieName.equals(cookie.getName())) {
                    return(cookie.getValue());
                }
            }
        }
        return(defaultValue);
    }
}
