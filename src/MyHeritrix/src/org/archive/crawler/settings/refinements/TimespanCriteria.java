/*
 * TimespanCriteria
 *
 * $Id: TimespanCriteria.java 3704 2005-07-18 17:30:21Z stack-sf $
 *
 * Created on Apr 8, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or any later version.
 *
 * Heritrix is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along with
 * Heritrix; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.archive.crawler.settings.refinements;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.archive.net.UURI;

/**
 * A refinement criteria that checks if a URI is requested within a specific
 * time frame. <p/>
 *
 * The timeframe's resolution is minutes and always operates in 24h GMT. The
 * format is <code>hhmm</code>, exmaples:
 * <p>
 * <code> 1200</code> for noon GMT <br>
 * <code> 1805</code> for 5 minutes past six in the afternoon GMT.
 *
 * @author John Erik Halse
 */
public class TimespanCriteria implements Criteria {

    private static DateFormat timeFormat;
    static {
        final TimeZone TZ = TimeZone.getTimeZone("GMT");
        timeFormat = new SimpleDateFormat("HHmm");
        timeFormat.setTimeZone(TZ);
    }

    private Date from;

    private Date to;

    /**
     * Create a new instance of TimespanCriteria.
     *
     * @param from start of the time frame (inclusive).
     * @param to end of the time frame (inclusive).
     * @throws ParseException
     */
    public TimespanCriteria(String from, String to) throws ParseException {
        setFrom(from);
        setTo(to);
    }

    public boolean isWithinRefinementBounds(UURI uri) {
        try {
            Date now = timeFormat.parse(timeFormat.format(new Date()));
            if (from.before(to)) {
                if (now.getTime() >= from.getTime()
                        && now.getTime() <= to.getTime()) {
                    return true;
                }
            } else {
                if (!(now.getTime() > to.getTime() && now.getTime() < from
                        .getTime())) {
                    return true;
                }
            }
        } catch (ParseException e) {
            // Should never happen since we are only parsing system time at
            // this place.
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get the beginning of the time frame to check against.
     *
     * @return Returns the from.
     */
    public String getFrom() {
        return timeFormat.format(from);
    }

    /**
     * Set the beginning of the time frame to check against.
     *
     * @param from The from to set.
     * @throws ParseException
     */
    public void setFrom(String from) throws ParseException {
        this.from = timeFormat.parse(from);
    }

    /**
     * Get the end of the time frame to check against.
     *
     * @return Returns the to.
     */
    public String getTo() {
        return timeFormat.format(to);
    }

    /**
     * Set the end of the time frame to check against.
     *
     * @param to The to to set.
     * @throws ParseException
     */
    public void setTo(String to) throws ParseException {
        this.to = timeFormat.parse(to);
    }

    public boolean equals(Object o) {
        if (o instanceof TimespanCriteria) {
            TimespanCriteria other = (TimespanCriteria) o;
            if (this.from.equals(other.from) && this.to.equals(other.to)) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#getName()
     */
    public String getName() {
        return "Time of day criteria";
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.refinements.Criteria#getDescription()
     */
    public String getDescription() {
        return "Accept any URIs between the hours of " + getFrom() + "(GMT) and "
            + getTo() + "(GMT) each day.";
    }
}