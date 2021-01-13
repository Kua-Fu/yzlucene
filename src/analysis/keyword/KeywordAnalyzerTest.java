package analysis.keyword;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import common.TestUtil;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;

import junit.framework.TestCase;

public class KeywordAnalyzerTest extends TestCase {
    private IndexSearcher searcher;

    public void setUp() throws Exception {
        Directory directory = new RAMDirectory();
        IndexWriter writer = new IndexWriter(directory, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);

        Document doc = new Document();
        doc.add(new Field("partnum", "Q36", Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS)); // partnum字段不分词

        doc.add(new Field("description", "Illidium Space Modulator", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);
        writer.close();
        searcher = new IndexSearcher(directory);
    }

    public void testTermQuery() throws Exception {
        Query query = new TermQuery(new Term("partnum", "Q36")); // term 不分词
        assertEquals(1, TestUtil.hitCount(searcher, query));

    }

    public void testBasicQueryParser() throws Exception {
        Query query = new QueryParser(Version.LUCENE_30, "description", new SimpleAnalyzer())
                .parse("partnum:Q36 AND SPACE"); // 没有符合的文档, 因为所有的字段都使用了SimpleAnalyzer， 但是partnum入库时候设置了不解析
        assertEquals("note Q36  -> q ", "+partnum:q +space", query.toString("description"));
        assertEquals("doc not found :(", 0, TestUtil.hitCount(searcher, query));
    }

    public void testPerFieldAnalyzer() throws Exception {
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new SimpleAnalyzer()); // 字段默认使用的分析器
        analyzer.addAnalyzer("partnum", new KeywordAnalyzer()); // partum字段使用特殊的分析器

        Query query = new QueryParser(Version.LUCENE_30, "description", analyzer).parse("partnum:Q36 AND SPACE");
        assertEquals("Q36 kept as-is", "+partnum:Q36 +space", query.toString("description"));
        assertEquals("doc found!", 1, TestUtil.hitCount(searcher, query)); // 发现符合条件的文档
    }
}