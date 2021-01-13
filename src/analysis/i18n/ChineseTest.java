package analysis.i18n;

import junit.framework.TestCase;
import org.apache.lucene.store.FSDirectory;

import common.TestUtil;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.search.Query;
import org.apache.lucene.index.Term;
import java.io.File;

public class ChineseTest extends TestCase {
    public void testChinese() throws Exception {
        String indexDir = "/Users/yz/work/github/yzlucene/src/analysis/index";
        Directory dir = FSDirectory.open(new File(indexDir));

        IndexSearcher searcher = new IndexSearcher(dir);
        Query query = new TermQuery(new Term("contents", "道"));
        assertEquals("道德经", 1, TestUtil.hitCount(searcher, query));
    }
}