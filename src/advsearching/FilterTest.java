package advsearching;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.FieldCacheRangeFilter;
import org.apache.lucene.search.FieldCacheTermsFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SpanQueryFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeFilter;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.Term;

import common.TestUtil;
import junit.framework.TestCase;

public class FilterTest extends TestCase {
    private Query allBooks;
    private IndexSearcher searcher;
    private Directory dir;

    protected void setUp() throws Exception {
        allBooks = new MatchAllDocsQuery();
        String indexDir = "/Users/yz/work/github/yzlucene/src/advsearching/index";
        dir = FSDirectory.open(new File(indexDir));
        searcher = new IndexSearcher(dir);
    }

    protected void tearDown() throws Exception {
        searcher.close();
        dir.close();
    }

    // term范围过滤
    public void testTermRangeFilter() throws Exception {
        Filter filter = new TermRangeFilter("title2", "d", "j", true, true);
        assertEquals(3, TestUtil.hitCount(searcher, allBooks, filter));
        TopDocs hits = searcher.search(allBooks, filter, 10);
        for (ScoreDoc match : hits.scoreDocs) {
            Document doc = searcher.doc(match.doc);
            System.out.println(match.score + ":" + doc.get("title2"));
        }
    }

    // number范围过滤
    public void testNumericDateFilter() throws Exception {
        Filter filter = NumericRangeFilter.newIntRange("pubmonth", 201001, 201006, true, true);
        assertEquals(2, TestUtil.hitCount(searcher, allBooks, filter));

        TopDocs hits = searcher.search(allBooks, filter, 10);
        for (ScoreDoc match : hits.scoreDocs) {
            Document doc = searcher.doc(match.doc);
            System.out.println(match.score + ":" + doc.get("pubmonth"));
        }
    }

    // cache filter
    public void testFieldCacheRangeFilter() throws Exception {
        Filter filter = FieldCacheRangeFilter.newStringRange("title2", "d", "j", true, true);
        assertEquals(3, TestUtil.hitCount(searcher, allBooks, filter));

        filter = FieldCacheRangeFilter.newIntRange("pubmonth", 201001, 201006, true, true);
        assertEquals(2, TestUtil.hitCount(searcher, allBooks, filter));
    }

    // cache terms filter
    public void testFieldCacheTermsFilter() throws Exception {
        Filter filter = new FieldCacheTermsFilter("category", new String[] { "/health/alternative/chinese",
                "/technology/computers/ai", "/technology/computers/programming" });
        assertEquals("expected 7 hits", 7, TestUtil.hitCount(searcher, allBooks, filter));
    }

    // query ——> filter
    public void testQueryWrapperFilter() throws Exception {
        TermQuery categoryQuery = new TermQuery(new Term("category", "/philosophy/eastern"));
        Filter categoryFilter = new QueryWrapperFilter(categoryQuery);
        assertEquals("only tao te ching", 1, TestUtil.hitCount(searcher, allBooks, categoryFilter));
    }

    // span query ——> span filter
    public void testSpanQueryFilter() throws Exception {
        SpanQuery categoryQuery = new SpanTermQuery(new Term("category", "/philosophy/eastern"));
        Filter categoryFilter = new SpanQueryFilter(categoryQuery);
        assertEquals("only tao te ching", 1, TestUtil.hitCount(searcher, allBooks, categoryFilter));
    }

    // boolean 过滤
    public void testFilterAlternative() throws Exception {
        TermQuery categoryQuery = new TermQuery(new Term("category", "/philosophy/eastern"));
        BooleanQuery constrainedQuery = new BooleanQuery();
        constrainedQuery.add(allBooks, BooleanClause.Occur.MUST);
        constrainedQuery.add(categoryQuery, BooleanClause.Occur.MUST);

        assertEquals("only tao te ching", 1, TestUtil.hitCount(searcher, constrainedQuery));
    }

    // prefixFilter
    public void testPrefixFilter() throws Exception {
        Filter prefixFilter = new PrefixFilter(new Term("category", "/technology/computers"));
        assertEquals("only /technology/computers/* books", 8, TestUtil.hitCount(searcher, allBooks, prefixFilter));
    }

    // cache term filter
    public void testCachingWrapper() throws Exception {
        Filter filter = new TermRangeFilter("title2", "d", "j", true, true);
        CachingWrapperFilter cachingFilter;
        cachingFilter = new CachingWrapperFilter(filter);
        assertEquals(3, TestUtil.hitCount(searcher, allBooks, cachingFilter));

    }
}