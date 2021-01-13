package advsearching;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.commons.lang.StringUtils;

public class SortingExample {
    private Directory directory;

    public SortingExample(Directory directory) {
        this.directory = directory;
    }

    public void displayResults(Query query, Sort sort) throws IOException {
        IndexSearcher searcher = new IndexSearcher(directory);
        searcher.setDefaultFieldSortScoring(true, false);

        TopDocs results = searcher.search(query, null, 20, sort);
        System.out.println("\nResults for: " + query.toString() + " sorted by " + sort);

        System.out.println(StringUtils.rightPad("Title", 30) + StringUtils.rightPad("pubmonth", 10)
                + StringUtils.center("id", 4) + StringUtils.center("score", 15));

        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        DecimalFormat scoreFormatter = new DecimalFormat("0.######");

        for (ScoreDoc sd : results.scoreDocs) {
            int docID = sd.doc;
            float score = sd.score;
            Document doc = searcher.doc(docID);
            out.println(StringUtils.rightPad(StringUtils.abbreviate(doc.get("title"), 29), 30)
                    + StringUtils.rightPad(doc.get("pubmonth"), 10) + StringUtils.center("" + docID, 4)
                    + StringUtils.leftPad(scoreFormatter.format(score), 12));
            out.println("   " + doc.get("category"));
        }
        searcher.close();
    }

    public static void main(String[] args) throws Exception {
        Query allBooks = new MatchAllDocsQuery();
        QueryParser parser = new QueryParser(Version.LUCENE_30, "contens", new StandardAnalyzer(Version.LUCENE_30));

        BooleanQuery query = new BooleanQuery();
        query.add(allBooks, BooleanClause.Occur.SHOULD);
        query.add(parser.parse("java OR action"), BooleanClause.Occur.SHOULD);

        String indexDir = "/Users/yz/work/github/yzlucene/src/advsearching/index";
        Directory directory = FSDirectory.open(new File(indexDir));

        SortingExample example = new SortingExample(directory);

        example.displayResults(query, Sort.RELEVANCE);
        example.displayResults(query, Sort.INDEXORDER);

        example.displayResults(query, new Sort(new SortField("category", SortField.STRING)));
        example.displayResults(query, new Sort(new SortField("pubmonth", SortField.INT, true)));

        example.displayResults(query, new Sort(new SortField("category", SortField.STRING), SortField.FIELD_SCORE,
                new SortField("pubmonth", SortField.INT, true)));

        example.displayResults(query,
                new Sort(new SortField[] { SortField.FIELD_SCORE, new SortField("category", SortField.STRING) }));

        directory.close();
    }
}