/* ReplayCharSequence
 *
 * Created on Mar 5, 2004
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
package org.archive.io;

import java.io.IOException;


/**
 * CharSequence interface with addition of a {@link #close()} method.
 *
 * Users of implementations of this interface must call {@link #close()} so
 * implementations get a chance at cleaning up after themselves.
 *
 * @author stack
 * @version $Revision: 3288 $, $Date: 2005-03-31 17:43:23 +0000 (Thu, 31 Mar 2005) $
 */
public interface ReplayCharSequence extends CharSequence {

    /**
     * Call this method when done so implementation has chance to clean up
     * resources.
     *
     * @throws IOException Problem cleaning up file system resources.
     */
    public void close() throws IOException;
}
