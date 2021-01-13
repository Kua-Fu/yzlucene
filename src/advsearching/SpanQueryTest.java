package advsearching;

import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.Spans;

import junit.framework.TestCase;

public class SpanQueryTest extends TestCase {
    private RAMDirectory directory;
    private IndexSearcher searcher;
    private IndexReader reader;

    private SpanTermQuery quick;
    private SpanTermQuery brown;
    private SpanTermQuery red;
    private SpanTermQuery fox;
    private SpanTermQuery lazy;
    private SpanTermQuery sleepy;
    private SpanTermQuery dog;
    private SpanTermQuery cat;

    private Analyzer analyzer;

    protected void setUp() throws Exception {
        directory = new RAMDirectory();
        analyzer = new WhitespaceAnalyzer();
        IndexWriter writer = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);

        Document doc = new Document();
        doc.add(new Field("f", "the quick brown fox jumps over the lazy dog", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new Field("f", "the quick red fox jumps over the sleepy cat", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        writer.close();

        searcher = new IndexSearcher(directory);
        reader = searcher.getIndexReader();

        quick = new SpanTermQuery(new Term("f", "quick"));
        brown = new SpanTermQuery(new Term("f", "brown"));
        red = new SpanTermQuery(new Term("f", "red"));
        fox = new SpanTermQuery(new Term("f", "fox"));
        lazy = new SpanTermQuery(new Term("f", "lazy"));
        sleepy = new SpanTermQuery(new Term("f", "sleepy"));
        dog = new SpanTermQuery(new Term("f", "dog"));
        cat = new SpanTermQuery(new Term("f", "cat"));

    }

    // 基础测试函数
    private void assertOnlyBrownFox(Query query) throws Exception {
        TopDocs hits = searcher.search(query, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("wrong dog", 0, hits.scoreDocs[0].doc);
    }

    // 基础测试函数
    private void assertBothFoxes(Query query) throws Exception {
        TopDocs hits = searcher.search(query, 10);
        assertEquals(2, hits.totalHits);
    }

    // 基础测试函数
    private void assertNoMatches(Query query) throws Exception {
        TopDocs hits = searcher.search(query, 10);
        assertEquals(0, hits.totalHits);
    }

    private void dumpSpans(SpanQuery query) throws Exception {
        Spans spans = query.getSpans(reader);
        System.out.println(query + ": ");
        int numSpans = 0;
        TopDocs hits = searcher.search(query, 10);

        float[] scores = new float[2];
        for (ScoreDoc sd : hits.scoreDocs) {
            scores[sd.doc] = sd.score;
        }

        while (spans.next()) { // 遍历span
            numSpans++;
            int id = spans.doc();
            Document doc = reader.document(id); // 获取文档

            TokenStream stream = analyzer.tokenStream("contents", new StringReader(doc.get("f")));
            TermAttribute term = stream.addAttribute(TermAttribute.class); // 重新分析

            StringBuilder buffer = new StringBuilder();
            buffer.append("   ");
            int i = 0;

            while (stream.incrementToken()) { // 遍历token

                if (i == spans.start()) { // 在span前后打印<>
                    buffer.append("<");
                }
                buffer.append(term.term());
                if (i + 1 == spans.end()) {
                    buffer.append(">");
                }
                buffer.append("  ");
                i++;
            }

            buffer.append("(").append(scores[id]).append(") ");
            System.out.println(buffer);

        }

        if (numSpans == 0) {
            System.out.println(" no spans");
        }

        System.out.println();

    }

    public void testSpanTermQuery() throws Exception {
        assertOnlyBrownFox(brown);
        dumpSpans(brown);
        dumpSpans(new SpanTermQuery(new Term("f", "the")));

    }

    // SpanFirstQuery, 需要指定一个位置范围
    public void testSpanFirstQuery() throws Exception {
        SpanFirstQuery sfq = new SpanFirstQuery(brown, 2);
        assertNoMatches(sfq);
        dumpSpans(sfq);

        sfq = new SpanFirstQuery(brown, 3);
        dumpSpans(sfq);
        assertOnlyBrownFox(sfq);
    }

    // SpanNearQuery和PhraseQuery之间的比较
    // SpanNearQuery有个参数，指定是否逆序，
    // 所以同样对于 fox quick的逆序查询，如果使用SpanNearQuery, 只需要stop=1，而PhraseQuery则需要stop=3
    // 但是对于顺序查找来说，SpanNearQuery和PhraseQuery相同
    // 原始field text: "the quick brown fox jumps over the lazy dog"
    public void testSpanNearQuery() throws Exception {
        SpanQuery[] quick_brown_dog = new SpanQuery[] { quick, brown, dog };
        SpanNearQuery snq = new SpanNearQuery(quick_brown_dog, 0, true);
        assertNoMatches(snq);
        dumpSpans(snq);

        snq = new SpanNearQuery(quick_brown_dog, 4, true);
        assertNoMatches(snq);
        dumpSpans(snq);

        snq = new SpanNearQuery(quick_brown_dog, 5, true);
        assertOnlyBrownFox(snq);
        dumpSpans(snq);

        snq = new SpanNearQuery(new SpanQuery[] { lazy, fox }, 3, false);
        assertOnlyBrownFox(snq);
        dumpSpans(snq);

        PhraseQuery pq = new PhraseQuery();
        pq.add(new Term("f", "quick"));
        pq.add(new Term("f", "brown"));
        pq.add(new Term("f", "dog"));
        pq.setSlop(4);
        assertNoMatches(pq);

        pq.setSlop(5);
        assertOnlyBrownFox(pq);

        PhraseQuery pqR = new PhraseQuery();
        pqR.add(new Term("f", "lazy"));
        pqR.add(new Term("f", "fox"));
        pqR.setSlop(4);
        assertNoMatches(pqR);

        pqR.setSlop(5);
        assertOnlyBrownFox(pqR);

    }

    // SpanNotQuery有两个参数，
    // 第一个参数，指定include条件
    // 第二个参数，是在include条件下，再添加exclude条件
    public void testSpanNotQuery() throws Exception {
        SpanNearQuery quick_fox = new SpanNearQuery(new SpanQuery[] { quick, fox }, 1, true);
        assertBothFoxes(quick_fox);
        dumpSpans(quick_fox);

        SpanNotQuery quick_fox_dog = new SpanNotQuery(quick_fox, dog);
        assertBothFoxes(quick_fox_dog);
        dumpSpans(quick_fox_dog);

        SpanNotQuery no_quick_red_fox = new SpanNotQuery(quick_fox, red);
        assertOnlyBrownFox(no_quick_red_fox);
        dumpSpans(no_quick_red_fox);
    }

    // 逻辑or关联，多个SpanQuery
    public void testSpanOrQuery() throws Exception {
        SpanNearQuery quick_fox = new SpanNearQuery(new SpanQuery[] { quick, fox }, 1, true);
        SpanNearQuery lazy_dog = new SpanNearQuery(new SpanQuery[] { lazy, dog }, 0, true);
        SpanNearQuery sleepy_cat = new SpanNearQuery(new SpanQuery[] { sleepy, cat }, 0, true);

        SpanNearQuery qf_near_ld = new SpanNearQuery(new SpanQuery[] { quick_fox, lazy_dog }, 3, true);
        assertOnlyBrownFox(qf_near_ld);
        dumpSpans(qf_near_ld);

        SpanNearQuery qf_near_sc = new SpanNearQuery(new SpanQuery[] { quick_fox, sleepy_cat }, 3, true);
        dumpSpans(qf_near_sc);

        SpanOrQuery or = new SpanOrQuery(new SpanQuery[] { qf_near_ld, qf_near_sc });
        assertBothFoxes(or);
        dumpSpans(or);

    }

}