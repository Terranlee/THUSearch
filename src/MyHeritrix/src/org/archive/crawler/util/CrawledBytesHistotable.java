package org.archive.crawler.util;

import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.recrawl.IdenticalDigestDecideRule;
import org.archive.util.Accumulator;
import org.archive.util.ArchiveUtils;
import org.archive.util.Histotable;

public class CrawledBytesHistotable extends Histotable<String> 
implements Accumulator<CrawlURI>, CoreAttributeConstants {
    private static final long serialVersionUID = 7923431123239026213L;
    
    public static final String NOTMODIFIED = "not-modified";
    public static final String DUPLICATE = "dup-by-hash";
    public static final String NOVEL = "novel";

    
    public CrawledBytesHistotable() {
        super();
        tally(NOVEL,0);
    }

    public void accumulate(CrawlURI curi) {
        if(curi.getFetchStatus()==HttpStatus.SC_NOT_MODIFIED) {
            tally(NOTMODIFIED, curi.getContentSize());
        } else if (IdenticalDigestDecideRule.hasIdenticalDigest(curi)) {
            tally(DUPLICATE,curi.getContentSize());
        } else {
            tally(NOVEL,curi.getContentSize());
        }
    }
    
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(ArchiveUtils.formatBytesForDisplay(getTotal()));
        sb.append(" crawled (");
        sb.append(ArchiveUtils.formatBytesForDisplay(get(NOVEL)));
        sb.append(" novel");
        if(get(DUPLICATE)!=null) {
            sb.append(", ");
            sb.append(ArchiveUtils.formatBytesForDisplay(get(DUPLICATE)));
            sb.append(" ");
            sb.append(DUPLICATE);
        }
        if(get(NOTMODIFIED)!=null) {
            sb.append(", ");
            sb.append(ArchiveUtils.formatBytesForDisplay(get(NOTMODIFIED)));
            sb.append(" ");
            sb.append(NOTMODIFIED);
        }
        sb.append(")");
        return sb.toString();
    }
}
