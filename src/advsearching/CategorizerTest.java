package advsearching;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;

import junit.framework.TestCase;

public class CategorizerTest extends TestCase {
    Map categoryMap;

    protected void setUp() throws Exception {
        categoryMap = new TreeMap<>();
        buildCategoryVectors();

    }

    private void buildCategoryVectors() throws IOException {
        String indexDir = "/Users/yz/work/github/yzlucene/src/advsearching/index";
        Directory directory = FSDirectory.open(new File(indexDir));
        IndexReader reader = IndexReader.open(directory);

        int maxDoc = reader.maxDoc();

        for (int i = 0; i < maxDoc; i++) {
            if (!reader.isDeleted(i)) {
                Document doc = reader.document(i);
                String category = doc.get("category");

                Map vectorMap = (Map) categoryMap.get(category);
                if (vectorMap == null) {
                    vectorMap = new TreeMap<>();
                    categoryMap.put(category, vectorMap);
                }

                byte[] tn = reader.norms("title");
                byte[] tn2 = reader.norms("title2");
                TermFreqVector termFreqVector = reader.getTermFreqVector(i, "subject");

                addTermFreqToMap(vectorMap, termFreqVector);
            }
        }

    }

    private void addTermFreqToMap(Map vectorMap, TermFreqVector termFreqVector) {
        String[] terms = termFreqVector.getTerms();
        int[] freqs = termFreqVector.getTermFrequencies();

        for (int i = 0; i < terms.length; i++) {
            String term = terms[i];

            if (vectorMap.containsKey(term)) {
                Integer value = (Integer) vectorMap.get(term);
                vectorMap.put(term, new Integer(value.intValue() + freqs[i]));
            } else {
                vectorMap.put(term, new Integer(freqs[i]));
            }
        }
    }

    // 通过取最小的余弦值，获取最近似的category
    private String getCategory(String subject) {
        String[] words = subject.split(" ");

        Iterator categoryIterator = categoryMap.keySet().iterator();

        double bestAngle = Double.MAX_VALUE;
        String bestCategory = null;

        while (categoryIterator.hasNext()) {
            String category = (String) categoryIterator.next();
            double angle = computeAngle(words, category);
            if (angle < bestAngle) {
                bestAngle = angle;
                bestCategory = category;
            }
        }

        return bestCategory;

    }

    // 计算余弦值
    private double computeAngle(String[] words, String category) {
        Map vectorMap = (Map) categoryMap.get(category);

        int dotProduct = 0;
        int sumOfSquares = 0;

        for (String word : words) {

            int categoryWordFreq = 0;

            if (vectorMap.containsKey(word)) {
                categoryWordFreq = ((Integer) vectorMap.get(word)).intValue();

            }

            dotProduct = dotProduct + categoryWordFreq;
            sumOfSquares = sumOfSquares + categoryWordFreq * categoryWordFreq;
        }

        double denominator;
        if (sumOfSquares == words.length) {
            denominator = sumOfSquares;
        } else {
            denominator = Math.sqrt(sumOfSquares) * Math.sqrt(words.length);
        }

        double ratio = dotProduct / denominator;
        return Math.acos(ratio);
    }

    //
    public void testCategorization() throws Exception {
        assertEquals("/technology/computers/programming/methodology", getCategory("extreme agile methodology"));

        assertEquals("/education/pedagogy", getCategory("montessori education philosophy"));
    }

}