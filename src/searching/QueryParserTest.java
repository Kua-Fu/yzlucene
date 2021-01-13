package searching;

import junit.framework.TestCase;
import common.TestUtil;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;

public class QueryParserTest extends TestCase {
    private Analyzer analyzer;
    private Directory dir;
    private IndexSearcher searcher;

    protected void setUp() throws Exception {
        analyzer = new WhitespaceAnalyzer();
        dir = FSDirectory.open(new File("/Users/yz/work/github/yzlucene/src/searching/index"));
        searcher = new IndexSearcher(dir);
    }

    protected void tearDown() throws Exception {
        searcher.close();
        dir.close();
    }

    public static boolean queryHitsIncludeTitle(IndexSearcher searcher, TopDocs hits, String title) throws IOException {
        for (ScoreDoc match : hits.scoreDocs) {
            Document doc = searcher.doc(match.doc);
            if (title.equals(doc.get("title"))) {
                return true;
            }
        }
        System.out.println("title '" + title + "' not found");
        return false;
    }

    // term范围查询
    public void testTermRangeQuery() throws Exception {
        Query query = new QueryParser(Version.LUCENE_30, "subject", analyzer).parse("title2:[Q TO V]");
        assertTrue(query instanceof TermRangeQuery);

        TopDocs matches = searcher.search(query, 10);
        assertTrue(queryHitsIncludeTitle(searcher, matches, "Tapestry in Action"));

        query = new QueryParser(Version.LUCENE_30, "subject", analyzer).parse("title2:{Q TO \"Tapestry in Action\"}");

        matches = searcher.search(query, 10);
        assertFalse(queryHitsIncludeTitle(searcher, matches, "Tapestry in Action"));

    }

    // 前缀、通配查询
    public void testLowercasing() throws Exception {
        Query q = new QueryParser(Version.LUCENE_30, "field", analyzer).parse("PerfixQuery*");
        assertEquals("lowercased", "perfixquery*", q.toString("field")); // 统一变为小写

        QueryParser qp = new QueryParser(Version.LUCENE_30, "field", analyzer);
        qp.setLowercaseExpandedTerms(false);
        q = qp.parse("PerfixQuery*");

        assertEquals("not lowercased", "PerfixQuery*", q.toString("field"));

    }

    // 词组查询
    public void testPhraseQuery() throws Exception {
        Query q = new QueryParser(Version.LUCENE_30, "field", new StandardAnalyzer(Version.LUCENE_30))
                .parse("\"This is Some Phrase* \"");
        assertEquals("analyzed", "\"? ? some phrase\"", q.toString("field"));

        q = new QueryParser(Version.LUCENE_30, "field", analyzer).parse("\"term\"");
        assertTrue("reduced to TermQuery", q instanceof TermQuery);
    }

    // slop
    public void testSlop() throws Exception {
        Query q = new QueryParser(Version.LUCENE_30, "field", analyzer).parse("\"exact phrase\"");

        assertEquals("zero slop", "\"exact phrase\"", q.toString("field"));

        QueryParser qp = new QueryParser(Version.LUCENE_30, "field", analyzer);
        qp.setPhraseSlop(5);

        q = qp.parse("\"sloppy phrase\"");
        assertEquals("sloppy, implicitly", "\"sloppy phrase\"~5", q.toString("field"));
    }

    // fuzzy
    public void testFuzzyQuery() throws Exception {
        QueryParser parser = new QueryParser(Version.LUCENE_30, "subject", analyzer);
        Query query = parser.parse("kountry~");
        System.out.println("fuzzy: " + query);

        query = parser.parse("kountry~0.7");
        System.out.println("fuzzy 2: " + query);
    }

}