/* RootFilter
 * 
 * Created on Oct 25, 2004
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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter that redirects accesses to 'index.jsp'.
 * @author stack
 * @version $Date: 2005-08-29 21:52:36 +0000 (Mon, 29 Aug 2005) $, $Revision: 3771 $
 */
public class RootFilter implements Filter {
    private FilterConfig filterConfig = null;
    
    public void init(FilterConfig config) {
        this.filterConfig = config;
    }
    
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain)
    throws IOException, ServletException {
        if (this.filterConfig == null) {
            return;
        }
        if (req instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest)req;
            String path = httpRequest.getRequestURI();
            if (path == null || path.equals(httpRequest.getContextPath()) ||
                    (path.equals(httpRequest.getContextPath() + "/"))) {
                String tgt = this.filterConfig.
                    getInitParameter("rootFilter.redirectTo");
                ((HttpServletResponse)res).sendRedirect((tgt == null)?
                    httpRequest.getContextPath() + "/index.jsp":
                    httpRequest.getContextPath() + tgt);
                return;
            }
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
        this.filterConfig = null;
    }
}
