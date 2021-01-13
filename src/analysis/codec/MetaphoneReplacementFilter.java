package analysis.codec;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;

import org.apache.commons.codec.language.Metaphone;

// 构造一个变音位过滤器 metaphone replacement
public class MetaphoneReplacementFilter extends TokenFilter {
    public static final String METAPHONE = "metaphone";
    private Metaphone metaphoner = new Metaphone();
    private TermAttribute termAttr;
    private TypeAttribute typeAttr;

    public MetaphoneReplacementFilter(TokenStream input) {
        super(input);
        termAttr = addAttribute(TermAttribute.class);
        typeAttr = addAttribute(TypeAttribute.class);
    }

    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }

        String encoded;
        encoded = metaphoner.encode(termAttr.term()); // 重新编码
        termAttr.setTermBuffer(encoded); // 在相同的位置 重置token（编码后）
        typeAttr.setType(METAPHONE); // 设置token的类型
        return true;
    }
}
