/* $Id: Md5URLConnection.java 4510 2006-08-18 16:13:32Z stack-sf $
 *
 * Created August 11th, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.net.md5;

import java.net.URL;

import org.archive.net.DownloadURLConnection;

/**
 * Md5 URL connection.
 * @author stack
 * @version $Date: 2006-08-18 16:13:32 +0000 (Fri, 18 Aug 2006) $, $Revision: 4510 $
 */
public class Md5URLConnection extends DownloadURLConnection {
    protected Md5URLConnection(URL u) {
        super(u);
    }
}