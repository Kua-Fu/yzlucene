package advsearching;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

public class MultiSearchTest extends TestCase {
    private IndexSearcher[] searchers;

    public void setUp() throws Exception {
        String[] animals = { "aardvark", "beaver", "coati", "dog", "elephant", "frog", "gila monster", "horse",
                "iguana", "javelina", "kangaroo", "lemur", "moose", "nematode", "orca", "python", "quokka", "rat",
                "scorpion", "tarantula", "uromastyx", "vicuna", "walrus", "xiphias", "yak", "zebra" };

        Analyzer analyzer = new WhitespaceAnalyzer();

        Directory aTOmDirectory = new RAMDirectory(); // 两个目录
        Directory nTOZDirectory = new RAMDirectory();

        IndexWriter aTOmWriter = new IndexWriter(aTOmDirectory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
        IndexWriter nTOZWriter = new IndexWriter(nTOZDirectory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);

        for (int i = animals.length - 1; i >= 0; i--) {
            Document doc = new Document();
            String animal = animals[i];
            doc.add(new Field("animal", animal, Field.Store.YES, Field.Index.NOT_ANALYZED));
            if (animal.charAt(0) < 'n') {
                aTOmWriter.addDocument(doc); // 根据首字母分割为不同的索引存储
            } else {
                nTOZWriter.addDocument(doc);
            }
        }

        aTOmWriter.close();
        nTOZWriter.close();

        searchers = new IndexSearcher[2];
        searchers[0] = new IndexSearcher(aTOmDirectory); // 两个IndexSearcher
        searchers[1] = new IndexSearcher(nTOZDirectory);
    }

    public void testMulti() throws Exception {
        MultiSearcher searcher = new MultiSearcher(searchers);
        TermRangeQuery query = new TermRangeQuery("animal", "h", "t", true, true); // 范围查询
        TopDocs hits = searcher.search(query, 10);
        assertEquals("tarantula not included", 12, hits.totalHits);
    }

}
