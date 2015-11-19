/* ContentTypeRegExpFilter.java
 *
 * Created on Sep 13, 2004
 *
 * Copyright (C) 2004 Tom Emerson.
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
package org.archive.crawler.filter;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.TextUtils;

/**
 * Compares the content-type of the passed CrawlURI to a regular expression.
 *
 * @author Tom Emerson
 * @version $Date: 2006-09-25 18:41:10 +0000 (Mon, 25 Sep 2006) $, $Revision: 4652 $
 * @deprecated As of release 1.10.0.  To be replaced by an equivalent
 * {@link DecideRule}.
 */
public class ContentTypeRegExpFilter extends URIRegExpFilter {

    private static final long serialVersionUID = 206378978342655106L;

    private static final String DESCRIPTION = "ContentType regexp filter" +
    		"*Deprecated* To be replaced by an equivalent DecideRule. " +
        "Cannot be used until after fetcher processors. Only then is the" +
        " Content-Type known. A good place for this filter is at" +
        " the writer step processing.  If the content-type is null," +
        " 301s usually have no content-type, the filter returns true.";

    /**
     * @param name Filter name.
     */
    public ContentTypeRegExpFilter(String name) {
        super
        (name, DESCRIPTION, "");
    }

    public ContentTypeRegExpFilter(String name, String regexp) {
        super(name, DESCRIPTION, regexp);
    }
    
    protected boolean innerAccepts(Object o) {
        // FIXME: can o ever be anything but a CrawlURI?
        if (!(o instanceof CrawlURI)) {
            return false;
        }
        String content_type = ((CrawlURI)o).getContentType();
        String regexp = getRegexp(o);
        return (regexp == null)? false:
            (content_type == null)? true:
                TextUtils.matches(getRegexp(o), content_type);
    }
}
