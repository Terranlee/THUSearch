/* TimespanCriteriaTest
 *
 * $Id: TimespanCriteriaTest.java 3287 2005-03-31 03:35:18Z stack-sf $
 *
 * Created on Apr 8, 2004
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
package org.archive.crawler.settings.refinements;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import junit.framework.TestCase;


/**
 *
 * @author John Erik Halse
 *
 */
public class TimespanCriteriaTest extends TestCase {
    public final void testIsWithinRefinementBounds() throws ParseException {
        DateFormat timeFormat;
        TimeZone TZ = TimeZone.getTimeZone("GMT");
        timeFormat = new SimpleDateFormat("HHmm");
        timeFormat.setTimeZone(TZ);
        Date now = timeFormat.parse(timeFormat.format(new Date()));

        String nowTime = timeFormat.format(now);
        String beforeTime1 = timeFormat.format(new Date(now.getTime() -
            1000 * 60 * 2));
        String beforeTime2 = timeFormat.format(new Date(now.getTime() -
            1000 * 60 * 1));
        String afterTime1 = timeFormat.format(new Date(now.getTime() +
            1000 * 60 * 1));

        // now is inside and before is less than after
        TimespanCriteria c = new TimespanCriteria(beforeTime1, afterTime1);
        assertTrue(c.isWithinRefinementBounds(null));

        // now is equal to before and less than after
        c = new TimespanCriteria(nowTime, afterTime1);
        assertTrue(c.isWithinRefinementBounds(null));

        // now is equal to before and less than after
        c = new TimespanCriteria(beforeTime1, nowTime);
        assertTrue(c.isWithinRefinementBounds(null));

        // now is outside and before is less than after
        c = new TimespanCriteria(beforeTime1, beforeTime2);
        assertFalse(c.isWithinRefinementBounds(null));

        // now is outside and before is greater than after
        c = new TimespanCriteria(afterTime1, beforeTime1);
        assertFalse(c.isWithinRefinementBounds(null));

        // now is inside and before is greater than after
        c = new TimespanCriteria(beforeTime2, beforeTime1);
        assertTrue(c.isWithinRefinementBounds(null));

        // now is equal to before and before is greater than after
        c = new TimespanCriteria(nowTime, beforeTime1);
        assertTrue(c.isWithinRefinementBounds(null));

        // now is equal to before and before is greater than after
        c = new TimespanCriteria(afterTime1, nowTime);
        assertTrue(c.isWithinRefinementBounds(null));
}

}
