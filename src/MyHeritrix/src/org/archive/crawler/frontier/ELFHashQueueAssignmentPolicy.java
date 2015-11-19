package org.archive.crawler.frontier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.framework.CrawlController;

public class ELFHashQueueAssignmentPolicy extends QueueAssignmentPolicy {
	private Log logger = LogFactory.getLog(this.getClass());
	
	public long ELFHash(String str) {
		long hash = 0;
		long x = 0;
		for (int i = 0; i < str.length(); i++) {
			hash = (hash << 4) + str.charAt(i);
			if ((x = hash & 0xF0000000L) != 0) {
				hash ^= (x >> 24);
				hash &= ~x;
			}
		}
		return (hash & 0x7FFFFFFF);
	}

	@Override
    public String getClassKey(CrawlController controller, CandidateURI cauri) {
        String uri = cauri.getUURI().toString();
        long hash = ELFHash(uri);
        String a = Long.toString(hash % 100);
        return a;
    }
}
