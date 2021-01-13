package analysis.nutch;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.searcher.Query;
import org.apache.nutch.analysis.NutchDocumentAnalyzer;
import org.apache.nutch.searcher.QueryFilters;
import analysis.AnalyzerUtils;

public class NutchExample {

    public static void main(String[] args) throws IOException {
        Configuration conf = new Configuration();
        conf.addResource("/Users/yz/work/github/yzlucene/src/analysis/nutch-default.xml");
        NutchDocumentAnalyzer analyzer = new NutchDocumentAnalyzer(conf);

        AnalyzerUtils.displayTokensWithFullDetails(analyzer, "The quick brown fox....");

        // 老版本的lucene lucene-core-2.4.0.jar
        // TokenStream ts = analyzer.tokenStream("content", new StringReader("The quick
        // brown fox..."));

        // int position = 0;
        // while (ts.incrementToken()) {
        // Token token = "";
        // if (token == null) {
        // break;
        // }
        // int increment = token.getPositionIncrement();
        // if (increment > 0) {
        // position = position + increment;
        // System.out.println();
        // System.out.print(position + ": ");
        // }

        // System.out.print("[" + token.termText() + ":" + token.startOffset() + "->" +
        // token.endOffset() + ":"
        // + token.type() + "] ");
        // }

        // System.out.println();

        Query nutchQuery = Query.parse("\"the quick brown\"", conf);

        org.apache.lucene.search.Query luceneQuery;
        luceneQuery = new QueryFilters(conf).filter(nutchQuery);

        System.out.println("Translated: " + luceneQuery);

    }

}