package indexing;

/**
 * Copyright Manning Publications Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific lan      
*/

import junit.framework.TestCase;

import common.TestUtil;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

import java.io.IOException;

// From chapter 2
public class IndexingTest extends TestCase {
  protected String[] ids = { "1", "2" };
  protected String[] unindexed = { "Netherlands", "Italy" };
  protected String[] unstored = { "Amsterdam has lots of bridges", "Venice has lots of canals" };
  protected String[] text = { "Amsterdam", "Venice" };

  private Directory directory;

  protected void setUp() throws Exception { // 1 创建一个内存目录
    directory = new RAMDirectory();

    IndexWriter writer = getWriter(); // 2

    for (int i = 0; i < ids.length; i++) { // 3
      Document doc = new Document();
      doc.add(new Field("id", ids[i], Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field("country", unindexed[i], Field.Store.YES, Field.Index.NO));
      doc.add(new Field("contents", unstored[i], Field.Store.NO, Field.Index.ANALYZED));
      doc.add(new Field("city", text[i], Field.Store.YES, Field.Index.ANALYZED));
      writer.addDocument(doc);
    }
    writer.close();
  }

  private IndexWriter getWriter() throws IOException { // 2
    return new IndexWriter(directory, new WhitespaceAnalyzer(), // 2
        IndexWriter.MaxFieldLength.UNLIMITED); // 2
  }

  protected int getHitCount(String fieldName, String searchString) throws IOException {
    IndexSearcher searcher = new IndexSearcher(directory); // 4
    Term t = new Term(fieldName, searchString);
    Query query = new TermQuery(t); // 5 简单的term查询
    int hitCount = TestUtil.hitCount(searcher, query); // 6 获取匹配的文档数量
    searcher.close();
    return hitCount;
  }

  public void testIndexWriter() throws IOException {
    IndexWriter writer = getWriter();
    assertEquals(ids.length, writer.numDocs()); // 7
    writer.close();
  }

  public void testIndexReader() throws IOException {
    IndexReader reader = IndexReader.open(directory);
    assertEquals(ids.length, reader.maxDoc()); // 8
    assertEquals(ids.length, reader.numDocs()); // 8
    reader.close();
  }

  /*
   * #1 Run before every test #2 Create IndexWriter #3 Add documents #4 Create new
   * searcher #5 Build simple single-term query #6 Get number of hits #7 Verify
   * writer document count #8 Verify reader document count
   */

  public void testDeleteBeforeOptimize() throws IOException {
    IndexWriter writer = getWriter();
    assertEquals(2, writer.numDocs()); // A 判断索引中有2个文档
    writer.deleteDocuments(new Term("id", "1")); // B 删除id为1的文档
    writer.commit();
    assertTrue(writer.hasDeletions()); // 1 判断存在文档有删除标记
    assertEquals(2, writer.maxDoc()); // 2 判断实际有2个文档
    assertEquals(1, writer.numDocs()); // 2 判断只有1个文档存在，另外1个文档有删除标记
    writer.close();
  }

  public void testDeleteAfterOptimize() throws IOException {
    IndexWriter writer = getWriter();
    assertEquals(2, writer.numDocs());
    writer.deleteDocuments(new Term("id", "1"));
    writer.optimize(); // 3 主动优化索引，即将有删除标记的文档删除（磁盘flush）
    writer.commit();
    assertFalse(writer.hasDeletions());
    assertEquals(1, writer.maxDoc()); // C 此时实际存在的文档数量
    assertEquals(1, writer.numDocs()); // C
    writer.close();
  }

  /*
   * #A 2 docs in the index #B Delete first document #C 1 indexed document, 0
   * deleted documents #1 Index contains deletions #2 1 indexed document, 1
   * deleted document #3 Optimize compacts deletes
   */

  public void testUpdate() throws IOException {

    assertEquals(1, getHitCount("city", "Amsterdam"));

    IndexWriter writer = getWriter();

    Document doc = new Document(); // A 创建一个新的文档，用于更新查询到的文档
    doc.add(new Field("id", "1", Field.Store.YES, Field.Index.NOT_ANALYZED)); // A
    doc.add(new Field("country", "Netherlands", Field.Store.YES, Field.Index.NO)); // A
    doc.add(new Field("contents", "Den Haag has a lot of museums", Field.Store.NO, Field.Index.ANALYZED)); // A
    doc.add(new Field("city", "Den Haag", Field.Store.YES, Field.Index.ANALYZED)); // A

    writer.updateDocument(new Term("id", "1"), // B 更新文档（即删除原本的文档，添加新的文档）
        doc); // B
    writer.close();

    assertEquals(0, getHitCount("city", "Amsterdam"));// C 验证原来的文档已经不存在
    assertEquals(1, getHitCount("city", "Haag")); // D 验证新的文档已经添加
  }

  /*
   * #A Create new document with "Haag" in city field #B Replace original document
   * with new version #C Verify old document is gone #D Verify new document is
   * indexed
   */

  public void testMaxFieldLength() throws IOException {

    assertEquals(1, getHitCount("contents", "bridges")); // 1

    IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), // 2
        new IndexWriter.MaxFieldLength(1)); // 2
    Document doc = new Document(); // 3
    doc.add(new Field("contents", "these bridges can't be found", // 3
        Field.Store.NO, Field.Index.ANALYZED)); // 3
    writer.addDocument(doc); // 3
    writer.close(); // 3

    assertEquals(1, getHitCount("contents", "bridges")); // 4
  }

  /*
   * #1 One initial document has bridges #2 Create writer with maxFieldLength 1 #3
   * Index document with bridges #4 Document can't be found
   */

}
