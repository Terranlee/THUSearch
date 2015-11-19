/* URIFrontierHostStatistics
 *
 * $Id: FrontierHostStatistics.java 2509 2004-09-02 02:16:11Z gojomo $
 *
 * Created on Mar 30, 2004
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
package org.archive.crawler.framework;


/**
 * An optional interface the Frontiers can implement to provide information
 * about specific hosts.
 *
 * <p>Some URIFrontier implmentations will want to provide a number of
 * statistics relating to the progress of particular hosts. This only applies
 * to those Frontiers whose internal structure  uses hosts to split up the
 * workload and (for example) implement politeness. Some other Frontiers may
 * also provide this info based on calculations.
 *
 * <ul>
 *     <li> {@link #activeHosts() Active hosts}
 *     <li> {@link #inactiveHosts() Inactive hosts}
 *     <li> {@link #deferredHosts() deferred hosts}
 *     <li> {@link #inProcessHosts() In process hosts}
 *     <li> {@link #readyHosts() Ready hosts}
 *     <li> {@link #hostStatus(String) Host status}
 * </ul>
 *
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.Frontier
 */
public interface FrontierHostStatistics {

    /**
     * Host has not been encountered by the Frontier, or has been encountered
     * but has been inactive so long that it has expired.
     */
    public static final int HOST_UNKNOWN = 0;
    /** Host has URIs ready to be emited. */
    public static final int HOST_READY = 1;
    /** Host has URIs currently being proessed. */
    public static final int HOST_INPROCESS = 2;
    /**
     * Host has been deferred for some amount of time, will become ready once
     * once that time has elapsed. This is most likely due to politeness or
     * waiting between retries. Other conditions may exist.
     */
    public static final int HOST_DEFERRED = 3;
    /**
     * Host has been encountered and all availible URIs for it have been
     * processed already. More URIs may become availible later or not.
     * Inactive hosts may eventually become 'forgotten'.
     */
    public static final int HOST_INACTIVE = 4;

    /**
     * Total number of hosts that are currently active.
     *
     * <p>Active hosts are considered to be those that are ready, deferred or
     * in process.
     *
     * @return Total number of hosts that are currently active.
     */
    public int activeHosts();

    /**
     * Total number of inactive hosts.
     *
     * <p>Inactive hosts are those hosts that have been active but have now been
     * exhausted and contain no more additional URIs.
     *
     * @return Total number of inactive hosts.
     */
    public int inactiveHosts();

    /**
     * Total number of deferred hosts.
     *
     * <p>Deferred hosts are currently active hosts that have been deferred
     * from processing for the time being (becausee of politeness or waiting
     * before retrying.
     *
     * @return Total number of deferred hosts.
     */
    public int deferredHosts();

    /**
     * Total number of hosts with URIs in process.
     *
     * <p>It is generally assumed that each host can have only 1 URI in
     * process at the same time. However some frontiers may implement
     * politeness differently meaning that the same host is both ready and
     * in process. {@link #activeHosts() activeHosts()} will not count them
     * twice though.
     *
     * @return Total number of hosts with URIs in process.
     */
    public int inProcessHosts();

    /**
     * Total number of hosts that have a URI ready for processing.
     *
     * @return Total number of hosts that have a URI ready for processing.
     */
    public int readyHosts();

    /**
     * Get the status of a host.
     *
     * <p>Hosts can be in one of the following states:
     * <ul>
     *     <li> {@link #HOST_READY Ready}
     *     <li> {@link #HOST_INPROCESS In process}
     *     <li> {@link #HOST_DEFERRED deferred}
     *     <li> {@link #HOST_INACTIVE Inactive}
     *     <li> {@link #HOST_UNKNOWN Unknown}
     * </ul>
     *
     * <p>Some Frontiers may allow a host to have more then one URI in process
     * at the same time. In those cases it will be reported as
     * {@link #HOST_READY Ready} as long as it is has more URIs ready for
     * processing. Only once it has no more possible URIs for processing will
     * it be reported as {@link #HOST_INPROCESS In process}
     *
     * @param host The name of the host to lookup the status for.
     * @return The status of the specified host.
     *
     * @see #HOST_DEFERRED
     * @see #HOST_INACTIVE
     * @see #HOST_INPROCESS
     * @see #HOST_READY
     * @see #HOST_UNKNOWN
     */
    public int hostStatus(String host);

}
