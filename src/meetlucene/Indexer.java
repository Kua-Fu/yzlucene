package meetlucene;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.FileReader;

// From chapter 1

/**
 * This code was originally written for Erik's Lucene intro java.net article
 */
public class Indexer {

    public static void main(String[] args) throws Exception {
        // if (args.length != 2) {
        // throw new IllegalArgumentException("Usage: java " + Indexer.class.getName()
        // + " <index dir> <data dir>");
        // }
        // String indexDir = args[0]; //1
        // String dataDir = args[1]; //2

        // 生成的索引存储目录
        String indexDir = "/Users/yz/work/github/yzlucene/src/meetlucene/index";

        // 索引的文件目录
        String dataDir = "/Users/yz/work/github/yzlucene/src/meetlucene/data";

        long start = System.currentTimeMillis();
        Indexer indexer = new Indexer(indexDir);
        int numIndexed;
        try {
            numIndexed = indexer.index(dataDir, new TextFilesFilter());
        } finally {
            indexer.close();
        }
        long end = System.currentTimeMillis();

        System.out.println("Indexing " + numIndexed + " files took " + (end - start) + " milliseconds");
    }

    private IndexWriter writer;

    public Indexer(String indexDir) throws IOException {
        Directory dir = FSDirectory.open(new File(indexDir));
        writer = new IndexWriter(dir, // 3 创建IndexWriter
                new StandardAnalyzer( // 3
                        Version.LUCENE_30), // 3
                true, // 3
                IndexWriter.MaxFieldLength.UNLIMITED); // 3
        writer.setUseCompoundFile(false); // 不使用默认的复合文件
    }

    public void close() throws IOException {
        writer.close(); // 4 关闭writer
    }

    public int index(String dataDir, FileFilter filter) throws Exception {

        File[] files = new File(dataDir).listFiles();

        for (File f : files) {
            if (!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead()
                    && (filter == null || filter.accept(f))) {
                indexFile(f);
            }
        }

        return writer.numDocs(); // 5 返回索引的文档数量
    }

    private static class TextFilesFilter implements FileFilter {
        public boolean accept(File path) {
            return path.getName().toLowerCase() // 6 筛选txt文件
                    .endsWith(".txt"); // 6
        }
    }

    protected Document getDocument(File f) throws Exception {
        Document doc = new Document();
        System.out.println(f.getName());
        System.out.println(f.getCanonicalPath());
        doc.add(new Field("contents", new FileReader(f))); // 7 索引文件内容
        doc.add(new Field("filename", f.getName(), // 8
                Field.Store.YES, Field.Index.NOT_ANALYZED));// 8 索引文件名称
        doc.add(new Field("fullpath", f.getCanonicalPath(), // 9 索引文件路径
                Field.Store.YES, Field.Index.NOT_ANALYZED));// 9
        return doc;
    }

    private void indexFile(File f) throws Exception {
        System.out.println("Indexing " + f.getCanonicalPath());
        Document doc = getDocument(f);
        writer.addDocument(doc); // 10 文档索引
    }
}
