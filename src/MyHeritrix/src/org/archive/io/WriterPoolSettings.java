/* FilePoolMemberSettings
 * 
 * Created on July 19th, 2006
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
package org.archive.io;

import java.io.File;
import java.util.List;

/**
 * Settings object for a {@link WriterPool}.
 * Used creating {@link WriterPoolMember}s.
 * @author stack
 * @version $Date: 2007-09-20 01:33:53 +0000 (Thu, 20 Sep 2007) $, $Revision: 5479 $
 */
public interface WriterPoolSettings {
    public long getMaxSize();
    public String getPrefix();
    public String getSuffix(); 
    public List<File> getOutputDirs();
    public boolean isCompressed();
    public List<String> getMetadata();
}