package advsearching;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.search.Filter;

import common.TestUtil;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import junit.framework.TestCase;

public class SecurityFilterTest extends TestCase {
    private IndexSearcher searcher;

    protected void setUp() throws Exception {
        Directory directory = new RAMDirectory();
        IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
        Document document = new Document();
        document.add(new Field("owner", "elwood", Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("keywords", "elwood's sensitive info", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(document);

        document = new Document();
        document.add(new Field("owner", "jake", Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("keywords", "jake's sensitive info", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(document);

        writer.close();
        searcher = new IndexSearcher(directory);

    }

    // 按照owner字段 过滤用户，保证只能查询到自己的数据
    public void testSecurityFilter() throws Exception {
        TermQuery query = new TermQuery(new Term("keywords", "info"));
        assertEquals("both documents match", 2, TestUtil.hitCount(searcher, query));

        Filter jakeFilter = new QueryWrapperFilter(new TermQuery(new Term("owner", "jake")));
        TopDocs hits = searcher.search(query, jakeFilter, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("elwood is safe, jake's sensitive info", searcher.doc(hits.scoreDocs[0].doc).get("keywords"));
    }

}