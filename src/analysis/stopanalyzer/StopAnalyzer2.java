package analysis.stopanalyzer;

import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LetterTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;

public class StopAnalyzer2 extends Analyzer {
    private Set stopWords;

    public StopAnalyzer2() {
        stopWords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
    }

    public StopAnalyzer2(String[] stopWords) {
        this.stopWords = StopFilter.makeStopSet(stopWords);
    }

    // 顺序很重要,
    // 先是对于筛选后的单词列表进行小写
    // 然后通过stopWords筛选
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new StopFilter(true, new LowerCaseFilter(new LetterTokenizer(reader)), stopWords);
    }

}