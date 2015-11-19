/* $Id: GeneratorFactory.java 4417 2006-08-02 01:12:00Z stack-sf $
 *
 * Created on July 27th, 2006
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
package org.archive.uid;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Factory that generates uids.
 * Singleton.  Default implementation is {@link UUIDGenerator}. To
 * change, specify alternate implementation of {@link Generator} with
 * {@link #SYSTEM_PROPERTY_GENERATOR_KEY} system property.
 * @author stack
 * @version $Revision: 4417 $ $Date: 2006-08-02 01:12:00 +0000 (Wed, 02 Aug 2006) $
 */
public class GeneratorFactory implements Generator {
	public final String SYSTEM_PROPERTY_GENERATOR_KEY =
		this.getClass().toString() + ".generator";
	private static final String DEFAULT_GENERATOR =
		"org.archive.uid.UUIDGenerator";
	private static final GeneratorFactory factory = new GeneratorFactory();
	private final Generator generator;
	
	private GeneratorFactory() {
		super();
		String className = System.getProperty(SYSTEM_PROPERTY_GENERATOR_KEY,
			DEFAULT_GENERATOR);
		Generator ridg = null;
		try {
			Class c = Class.forName(className);
			ridg = (Generator) c.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.generator = ridg;
	}
	
	public URI getRecordID() throws URISyntaxException {
		return this.generator.getRecordID();
	}
	
	public URI getQualifiedRecordID(Map<String, String> qualifiers)
	throws URISyntaxException {
		return this.generator.getQualifiedRecordID(qualifiers);
	}
	
	public URI getQualifiedRecordID(String key, String value)
	throws URISyntaxException {
		return this.generator.getQualifiedRecordID(key, value);
	}
	
	public URI qualifyRecordID(final URI uri,
			final Map<String, String> qualifiers)
	throws URISyntaxException {
		return this.generator.qualifyRecordID(uri, qualifiers);
	}

	public static GeneratorFactory getFactory() {
		return factory;
	}
}
