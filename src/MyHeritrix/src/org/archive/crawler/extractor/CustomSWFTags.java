/*
 * SWFCustomAction
 *
 * $Id: CustomSWFTags.java 3392 2005-04-14 21:48:31Z stack-sf $
 *
 * Created on Mar 19, 2004
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

package org.archive.crawler.extractor;

import java.io.IOException;
import java.util.Vector;

import com.anotherbigidea.flash.interfaces.SWFActions;
import com.anotherbigidea.flash.writers.SWFTagTypesImpl;

/**
 * Overwrite action tags, that may hold URI, to use <code>CrawlUriSWFAction
 * <code> action.
 *
 * @author Igor Ranitovic
 */
public class CustomSWFTags extends SWFTagTypesImpl {
    SWFActions actions;

    public CustomSWFTags(SWFActions a) {
        super(null);
        actions = a;
    }

    public SWFActions tagDefineButton(int id, Vector buttonRecords)
            throws IOException {

        return actions;
    }

    public SWFActions tagDefineButton2(int id, boolean trackAsMenu,
            Vector buttonRecord2s) throws IOException {

        return actions;
    }

    public SWFActions tagDoAction() throws IOException {
        return actions;
    }
}
