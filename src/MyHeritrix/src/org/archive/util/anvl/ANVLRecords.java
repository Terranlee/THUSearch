/* $Id: ANVLRecords.java 4440 2006-08-05 01:15:47Z stack-sf $
 *
 * Created Aug 2, 2006
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
package org.archive.util.anvl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.archive.io.UTF8Bytes;

/**
 * List of {@link ANVLRecord}s.
 * @author stack
 * @version $Date: 2006-08-05 01:15:47 +0000 (Sat, 05 Aug 2006) $ $Version$
 */
public class ANVLRecords extends ArrayList<ANVLRecord> implements UTF8Bytes {
	private static final long serialVersionUID = 5361551920550106113L;

	public ANVLRecords() {
	    super();
	}

	public ANVLRecords(int initialCapacity) {
		super(initialCapacity);
	}

	public ANVLRecords(Collection<ANVLRecord> c) {
		super(c);
	}

	public byte[] getUTF8Bytes() throws UnsupportedEncodingException {
		return toString().getBytes(UTF8);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (final Iterator i = iterator(); i.hasNext();) {
			sb.append(i.next().toString());
		}
		return super.toString();
	}
}