package analysis.synonym;

import java.io.IOException;
import java.util.Stack;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

public class SynonymFilter extends TokenFilter {
    public static final String TOKEN_TYPE_SYNONYM = "SYNONYM";
    private Stack<String> synonymStack; // 定义同义词的buffer
    private SynonymEngine engine;
    private AttributeSource.State current;

    private final TermAttribute termAtt;
    private final PositionIncrementAttribute posIncrAtt;

    public SynonymFilter(TokenStream in, SynonymEngine engine) {
        super(in);
        synonymStack = new Stack<String>();
        this.engine = engine;
        this.termAtt = addAttribute(TermAttribute.class);
        this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);

    }

    private boolean addAliasesToStack() throws IOException {
        String[] synonyms = engine.getSynonyms(termAtt.term());
        if (synonyms == null) {
            return false;
        }
        for (String synonym : synonyms) {
            synonymStack.push(synonym); // 将同义词push到stack上
        }
        return true;
    }

    public boolean incrementToken() throws IOException {
        if (synonymStack.size() > 0) {
            String syn = synonymStack.pop(); // 从内存中取出同义词
            restoreState(current);
            termAtt.setTermBuffer(syn);
            posIncrAtt.setPositionIncrement(0); // 设置同义词与input为相同的位置
            return true;
        }
        if (!input.incrementToken()) {
            return false;
        }
        if (addAliasesToStack()) {
            current = captureState(); // 保存当前的token
        }
        return true;
    }
}