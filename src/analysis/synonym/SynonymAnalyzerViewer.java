package analysis.synonym;

import java.io.IOException;

import analysis.AnalyzerUtils;

// 展示输入字符串，同义词分词后的效果
public class SynonymAnalyzerViewer {
    public static void main(String[] args) throws IOException {
        SynonymEngine engine = new TestSynonymEngine();

        AnalyzerUtils.displayTokensWithPositions(new SynonymAnalyzer(engine),
                "The quick brown fox jumps over the lazy dog");
    }
}