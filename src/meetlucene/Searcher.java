package meetlucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

public class Searcher {
    public static void main(String[] args) throws IllegalArgumentException, IOException, ParseException {
        // if (args.length != 2) {
        // throw new IllegalArgumentException("Usage: java " + Searcher.class.getName()
        // + " <index dir> <query>");
        // }
        // String indexDir = args[0];
        // String q = args[1];

        // 检索的索引目录
        String indexDir = "/Users/yz/work/github/yzlucene/src/meetlucene/index";

        // 查询语句
        String q = "逢";
        search(indexDir, q);

    }

    public static void search(String indexDir, String q) throws IOException, ParseException {

        Directory dir = FSDirectory.open(new File(indexDir)); // 3 打开索引
        IndexSearcher is = new IndexSearcher(dir); // 3

        QueryParser parser = new QueryParser(Version.LUCENE_30, // 4 翻译查询语句
                "contents", // 4
                new StandardAnalyzer( // 4
                        Version.LUCENE_30)); // 4
        Query query = parser.parse(q); // 4
        long start = System.currentTimeMillis();
        TopDocs hits = is.search(query, 10); // 5 开始检索
        long end = System.currentTimeMillis();

        System.err.println("Found " + hits.totalHits + // 6 查询结果信息
                " document(s) (in " + (end - start) + // 6
                " milliseconds) that matched query '" + // 6
                q + "':"); // 6

        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc); // 7 查询文档
            System.out.println(doc.get("fullpath")); // 8 文档名称
        }

        is.close(); // 9 关闭索引
    }
}