package analysis.stopanalyzer;

import analysis.AnalyzerUtils;
import junit.framework.TestCase;

public class StopAnalyzerAlternativesTest extends TestCase {
    // 测试stop word
    public void testStopAnalyzer2() throws Exception {
        AnalyzerUtils.assertAnalyzesTo(new StopAnalyzer2(), "The quick brown...", new String[] { "quick", "brown" });
    }

    // 测试 flawed stop word
    public void testStopAnalyzerFlawed() throws Exception {
        AnalyzerUtils.assertAnalyzesTo(new StopAnalyzerFlawed(), "The quick brown...",
                new String[] { "the", "quick", "brown" });
    }

    // 展示 stopWords
    public void main(String[] args) throws Exception {
        AnalyzerUtils.displayTokens(new StopAnalyzerFlawed(), "The quick brown...");
    }

}