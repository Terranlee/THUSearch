/* ExternalImplInterface
 * 
 * Created on May 25, 2005
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
package org.archive.crawler.deciderules;

/**
 * Interface used by {@link ExternalImplDecideRule}.
 * @author stack
 * @version $Date: 2005-10-03 17:46:10 +0000 (Mon, 03 Oct 2005) $, $Revision: 3860 $
 * @see ExternalImplDecideRule
 */
public interface ExternalGeoLookupInterface {
    /**
     * @param obj Object to evaluate (Usually its type is InetAddress).
     * @return String of a country code that the object belongs to.
     */
    public String lookup (Object obj);
}
