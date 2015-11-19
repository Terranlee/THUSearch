/* $Id:  $
 *
 * Copyright (C) 2007 Olaf Freyer
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

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.frontier.WorkQueue;

/**
 * Applies configured decision to every candidate URI that would 
 * overbudget its queue. (total expended + pending > total budget).
 * This rule has no impact on allready enqueued URIs, thus
 * the right place to use it is the DecidingScope (triggered via LinksScoper)
 * 
 * (Originally named QueueSizeLimiterDecideRule).
 * @author Olaf Freyer
 */
public class QueueOverbudgetDecideRule extends PredicatedDecideRule {

    private static final long serialVersionUID = 5165201864629344642L;

    public QueueOverbudgetDecideRule(String name) {
        super(name);
        setDescription("QueueOverbudgetDecideRule. "
                + "Applies configured decision to every candidate URI that would "
                + "overbudget its queue. (total expended + pending > total budget)."
                + "This rule has no impact on already enqueued URIs, thus "
                + "the right place to use it is the DecidingScope (triggered via LinksScoper) ");
    }

    @Override
    protected boolean evaluate(Object object) {
        if(! (object instanceof CandidateURI)) {
            return false; 
        }
        
        CandidateURI caUri = (CandidateURI) object;
        Frontier frontier = getController().getFrontier();

        CrawlURI curi;
        if (caUri instanceof CrawlURI) {
            // this URI already has been enqueued - don't change previous
            // decision
            return false;
        } else {
            curi = new CrawlURI(caUri.getUURI());
            curi.setClassKey(frontier.getClassKey(curi));
        }
        WorkQueue wq = (WorkQueue) frontier.getGroup(curi);
        return (wq.getPendingExpenditure() + wq.getTotalExpenditure()) 
                    > wq.getTotalBudget();
    }
}
