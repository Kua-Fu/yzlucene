package advsearching;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.function.CustomScoreProvider;
import org.apache.lucene.search.function.CustomScoreQuery;
import org.apache.lucene.search.function.FieldScoreQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.util.Date;

public class FunctionQueryTest extends TestCase {
    IndexSearcher s;
    IndexWriter w;

    private void addDoc(int score, String content) throws Exception {
        Document doc = new Document();
        doc.add(new Field("score", Integer.toString(score), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        doc.add(new Field("content", content, Field.Store.NO, Field.Index.ANALYZED));
        w.addDocument(doc);
    }

    public void setUp() throws Exception {
        Directory dir = new RAMDirectory();
        w = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_30), IndexWriter.MaxFieldLength.UNLIMITED);
        addDoc(7, "this hat is green");
        addDoc(42, "this hat is blue");
        w.close();
        s = new IndexSearcher(dir, true);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        s.close();
    }

    // 按照score字段值，逆序排序
    public void testFieldScoreQuery() throws Exception {
        Query q = new FieldScoreQuery("score", FieldScoreQuery.Type.BYTE);
        TopDocs hits = s.search(q, 10);
        assertEquals(2, hits.scoreDocs.length);
        assertEquals(1, hits.scoreDocs[0].doc);
        assertEquals(42, (int) hits.scoreDocs[0].score);

        assertEquals(0, hits.scoreDocs[1].doc);
        assertEquals(7, (int) hits.scoreDocs[1].score);
    }

    // 使用自定义的打分机制
    public void testCustomScoreQuery() throws Throwable {
        Query q = new QueryParser(Version.LUCENE_30, "content", new StandardAnalyzer(Version.LUCENE_30))
                .parse("the green hat");
        FieldScoreQuery qf = new FieldScoreQuery("score", FieldScoreQuery.Type.BYTE);

        CustomScoreQuery customQ = new CustomScoreQuery(q, qf) {
            public CustomScoreProvider getCustomScoreProvider(IndexReader r) {
                return new CustomScoreProvider(r) {
                    public float customScore(int doc, float subQueryScore, float valSrcScore) {
                        return (float) (Math.sqrt(subQueryScore) * valSrcScore);
                    }
                };
            }
        };

        TopDocs hits = s.search(customQ, 10);
        assertEquals(2, hits.scoreDocs.length);

        assertEquals(1, hits.scoreDocs[0].doc);
        assertEquals(0, hits.scoreDocs[1].doc);

    }

    static class RecencyBoostingQuery extends CustomScoreQuery {
        double multiplier;
        int today;
        int maxDaysAgo;
        String dayField;
        static int MSEC_PER_DAY = 1000 * 3600 * 24;

        public RecencyBoostingQuery(Query q, double multiplier, int maxDaysAgo, String dayField) {
            super(q);
            today = (int) (new Date().getTime() / MSEC_PER_DAY);
            this.multiplier = multiplier;
            this.maxDaysAgo = maxDaysAgo;
            this.dayField = dayField;
        }

        private class RecencyBooster extends CustomScoreProvider {
            final int[] publishDay;

            public RecencyBooster(IndexReader r) throws IOException {
                super(r);
                publishDay = FieldCache.DEFAULT.getInts(r, dayField); // 从缓存中获取字段

            }

            // 重新计算分值
            public float customScore(int doc, float subQueryScore, float valSrcScore) {
                int daysAgo = today - publishDay[doc]; // 已经过去的时间
                if (daysAgo < maxDaysAgo) {
                    float boost = (float) (multiplier * (maxDaysAgo - daysAgo) / maxDaysAgo);
                    return (float) (subQueryScore * (1.0 + boost));
                } else {
                    return subQueryScore;
                }
            }
        }

        public CustomScoreProvider gCustomScoreProvider(IndexReader r) throws IOException {
            return new RecencyBooster(r);

        }
    }

    // 更新发布时间，修改打分数值
    public void testRecency() throws Throwable {
        String indexDir = "/Users/yz/work/github/yzlucene/src/advsearching/index";
        Directory dir = FSDirectory.open(new File(indexDir));
        IndexReader r = IndexReader.open(dir);
        IndexSearcher s = new IndexSearcher(r);
        s.setDefaultFieldSortScoring(true, true);

        QueryParser parser = new QueryParser(Version.LUCENE_30, "contents", new StandardAnalyzer(Version.LUCENE_30));
        Query q = parser.parse("java in action");
        Query q2 = new RecencyBoostingQuery(q, 2.0, 2 * 365, "pubmonthAsDay");

        Sort sort = new Sort(new SortField[] { SortField.FIELD_SCORE, new SortField("title2", SortField.STRING) });
        TopDocs hits = s.search(q2, null, 5, sort);

        for (int i = 0; i < hits.scoreDocs.length; i++) {
            Document doc = r.document(hits.scoreDocs[i].doc);
            System.out.println((1 + i) + ":" + doc.get("title") + ": pubmonth=" + doc.get("pubmonth") + " score="
                    + hits.scoreDocs[i].score);
        }

        s.close();
        r.close();
        dir.close();

    }

}