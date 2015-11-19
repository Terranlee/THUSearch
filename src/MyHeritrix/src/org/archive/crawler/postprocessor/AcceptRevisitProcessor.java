package org.archive.crawler.postprocessor;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants;

/**
 * Set a URI to be revisited by the ARFrontier. This only makes sense when using
 * the ARFrontier and a decide-rule chain granting only selected access to this
 * processor. This is the opposite of the RejectRevisitProcessor class.
 * 
 * @author mzsanford
 */
public class AcceptRevisitProcessor extends Processor implements
        AdaptiveRevisitAttributeConstants {
    private static final long serialVersionUID = 4310432303089418844L;

    private static final Logger logger = Logger
            .getLogger(AcceptRevisitProcessor.class.getName());

    public AcceptRevisitProcessor(String name) {
        super(name, "Set a URI to be revisited by the ARFrontier.");
    }

    @Override
    protected void initialTasks() {
        CrawlURI.addAlistPersistentMember(A_DISCARD_REVISIT);
    }

    @Override
    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        if (curi != null && curi.containsKey(A_DISCARD_REVISIT)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Removing DISCARD_REVISIT boolean from crawl URI: "
                        + curi.getUURI().toString());
            }
            curi.remove(A_DISCARD_REVISIT);
        }
    }

}
