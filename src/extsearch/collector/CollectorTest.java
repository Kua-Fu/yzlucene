package extsearch.collector;

import java.io.File;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import common.TestUtil;
import junit.framework.TestCase;

public class CollectorTest extends TestCase {

    public void testCollecting() throws Exception {
        String indexDir = "/Users/yz/work/github/yzlucene/src/extsearch/index";
        Directory dir = FSDirectory.open(new File(indexDir));
        TermQuery query = new TermQuery(new Term("contents", "junit"));
        IndexSearcher searcher = new IndexSearcher(dir);

        BookLinkCollector collector = new BookLinkCollector();
        searcher.search(query, collector);

        Map<String, String> linkMap = collector.getLinks();
        assertEquals("ant in action", linkMap.get("http://www.manning.com/loughran"));

        TopDocs hits = searcher.search(query, 10);
        TestUtil.dumpHits(searcher, hits);

        searcher.close();
        dir.close();
    }

}