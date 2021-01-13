package analysis.codec;

import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import analysis.AnalyzerUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import junit.framework.TestCase;

public class MetaPhoneAnalyzerTest extends TestCase {
    public void testKoolKat() throws Exception {
        RAMDirectory directory = new RAMDirectory();
        Analyzer analyzer = new MetaphoneReplacementAnalyzer();
        IndexWriter writer = new IndexWriter(directory, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);

        Document doc = new Document();
        doc.add(new Field("contents", "cool cat", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc); // 写入文档
        writer.close();

        IndexSearcher searcher = new IndexSearcher(directory);
        Query query = new QueryParser(Version.LUCENE_30, "contents", analyzer).parse("kool cat"); // parse搜索语句

        TopDocs hits = searcher.search(query, 1); // 验证搜索结果
        assertEquals(1, hits.totalHits);

        int docID = hits.scoreDocs[0].doc;
        doc = searcher.doc(docID);
        assertEquals("cool cat", doc.get("contents")); // 查看搜索文档的原始字段
        searcher.close();

    }

    public static void main(String[] args) throws Exception {
        MetaphoneReplacementAnalyzer analyzer = new MetaphoneReplacementAnalyzer();
        AnalyzerUtils.displayTokens(analyzer, "The quick brown fox jumped over the lazy dog");

        System.out.println("");
        AnalyzerUtils.displayTokens(analyzer, "Tha quik brown phox jumpd ovvar tha lazi dag");
    }

}