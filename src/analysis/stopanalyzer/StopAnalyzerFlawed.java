package analysis.stopanalyzer;

import java.io.Reader;

import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LetterTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;

public class StopAnalyzerFlawed extends Analyzer {
    private Set stopWords;

    public StopAnalyzerFlawed() {
        stopWords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
    }

    public StopAnalyzerFlawed(String[] stopWords) {
        this.stopWords = StopFilter.makeStopSet(stopWords);
    }

    // 顺序很重要,
    // 先是通过stopWords筛选，
    // 然后对于筛选后的单词列表进行小写
    // 一般都是写小写，在进行stopWords过滤
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new LowerCaseFilter(new StopFilter(true, new LetterTokenizer(reader), stopWords));
    }
}