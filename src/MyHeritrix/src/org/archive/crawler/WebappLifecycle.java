/* WebappLifecycle
 * 
 * Created on Oct 26, 2004
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
package org.archive.crawler;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


/**
 * Calls start and stop of Heritrix when Heritrix is bundled as a webapp.
 * @author stack
 * @version $Date: 2005-11-17 00:55:56 +0000 (Thu, 17 Nov 2005) $, $Revision: 3959 $
 */
public class WebappLifecycle implements ServletContextListener {
    private Heritrix heritrix = null;
    public void contextInitialized(ServletContextEvent sce) {
        if (!Heritrix.isCommandLine()) {
            try {
				this.heritrix = new Heritrix(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
            if (this.heritrix != null) {
                this.heritrix.start();
            }
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        if (this.heritrix !=  null) {
            this.heritrix.destroy();
            this.heritrix = null;
        }
    }
}
