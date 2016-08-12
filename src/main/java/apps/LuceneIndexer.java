/*
 *  Copyright 2015 Carnegie Mellon University
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package apps;
import org.apache.commons.cli.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.*;

import com.google.common.base.Joiner;

import java.nio.file.Paths;
import java.io.*;

import utils.*;
import source.*;


// Partially based on https://lucene.apache.org/core/6_0_0/demo/src-html/org/apache/lucene/demo/IndexFiles.html

/**
 * <p>An indexing applications that reads a (compressed) Yahoo answers file
 * and creates two things:</p>
 * <ol>
 * <li>A Lucene index of Yahoo answers
 * <li>TREC-format QREL file: the only relevant answers is the best answer marked in the XML.
 * </ol> 
 * 
 * @author Leonid Boytsov
 *
 */

public class LuceneIndexer {
  static void Usage(String err, Options opt) {
    System.err.println("Error: " + err);
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( "LuceneIndexer", opt);      
    System.exit(1);
  } 
  
  public static void main(String [] args) {
    Options options = new Options();
    
    options.addOption("i", 			null, true, "input file");
    options.addOption("o", 			null, true, "output directory");
    options.addOption("r", 			null, true, "optional output TREC-format QREL file");
    
    options.addOption("bm25_b",     null, true, "BM25 parameter: b");
    options.addOption("bm25_k1",    null, true, "BM25 parameter: k1");
    options.addOption("bm25fixed", 	null, false, "use the fixed BM25 similarity");
    
    Joiner   commaJoin  = Joiner.on(',');
    
    options.addOption("source_type", null, true, 
                      "document source type: " + commaJoin.join(SourceFactory.getDocSourceList()));
    
    // If you increase this value, you may need to modify the following line in *.sh file
    // export MAVEN_OPTS="-Xms8192m -server"
    double ramBufferSizeMB = 1024 * 8; // 8 GB
    
    CommandLineParser parser = new org.apache.commons.cli.GnuParser();
    
    IndexWriter       indexWriter = null;
    BufferedWriter    qrelWriter = null;
    
    int docNum = 0;
    
    try {
      CommandLine cmd = parser.parse(options, args);
      
      String inputFileName = null, outputDirName = null, qrelFileName = null;
      
      if (cmd.hasOption("i")) {
        inputFileName = cmd.getOptionValue("i");
      } else {
        Usage("Specify 'input file'", options);
      }
      
      if (cmd.hasOption("o")) {
        outputDirName = cmd.getOptionValue("o");
      } else {
        Usage("Specify 'index directory'", options);
      }
      
      if (cmd.hasOption("r")) {
        qrelFileName = cmd.getOptionValue("r");
      } else {
        Usage("Specify 'TREC-format QREL file'", options);
      }      
      
      String sourceName = cmd.getOptionValue("source_type");
      
      if (sourceName == null)
        Usage("Specify document source type", options);
      
      qrelWriter = new BufferedWriter(new FileWriter(qrelFileName));
      
      File outputDir = new File(outputDirName);
      if (!outputDir.exists()) {
        if (!outputDir.mkdirs()) {
          System.out.println("couldn't create " + outputDir.getAbsolutePath());
          System.exit(1);
        }
      }
      if (!outputDir.isDirectory()) {
        System.out.println(outputDir.getAbsolutePath() + " is not a directory!");
        System.exit(1);
      }
      if (!outputDir.canWrite()) {
        System.out.println("Can't write to " + outputDir.getAbsolutePath());
        System.exit(1);
      }
      
      boolean useFixedBM25 = cmd.hasOption("bm25fixed");
      
      float bm25_k1 = UtilConst.BM25_K1_DEFAULT, bm25_b = UtilConst.BM25_B_DEFAULT;
      
      if (cmd.hasOption("bm25_k1")) {
        try {
          bm25_k1 = Float.parseFloat(cmd.getOptionValue("bm25_k1"));
        } catch (NumberFormatException e) {
          Usage("Wrong format for 'bm25_k1'", options);
        }
      }
      
      if (cmd.hasOption("bm25_b")) {
        try {
          bm25_b = Float.parseFloat(cmd.getOptionValue("bm25_b"));
        } catch (NumberFormatException e) {
          Usage("Wrong format for 'bm25_b'", options);
        }
      }        

      EnglishAnalyzer   analyzer = new EnglishAnalyzer();
      FSDirectory       indexDir    = FSDirectory.open(Paths.get(outputDirName));
      IndexWriterConfig indexConf   = new IndexWriterConfig(analyzer);
      
      indexConf.setOpenMode(OpenMode.CREATE); // Overwrite the 
      indexConf.setRAMBufferSizeMB(ramBufferSizeMB);
      
      System.out.println(String.format("BM25 parameters k1=%f b=%f ", bm25_k1, bm25_b));
      
      if (useFixedBM25) {
        System.out.println(String.format("Using fixed BM25Simlarity, k1=%f b=%f", bm25_k1, bm25_b));
        indexConf.setSimilarity(new BM25SimilarityFix(bm25_k1, bm25_b));
      } else {
        System.out.println(String.format("Using Lucene BM25Similarity, k1=%f b=%f", bm25_k1, bm25_b));
        indexConf.setSimilarity(new BM25Similarity(bm25_k1, bm25_b));
      }
      
      indexWriter = new IndexWriter(indexDir, indexConf);
      
      DocumentSource inpDocSource = SourceFactory.createDocumentSource(sourceName, inputFileName);
      DocumentEntry  inpDoc = null;
      TextCleaner    textCleaner = new TextCleaner(null);
      
      while ((inpDoc = inpDocSource.next()) != null) {
        ++docNum;

        Document  luceneDoc = new Document();
        String cleanText = textCleaner.cleanUp(inpDoc.mDocText);
        
        luceneDoc.add(new StringField(UtilConst.FIELD_ID, inpDoc.mDocId, Field.Store.YES));
        luceneDoc.add(new TextField(UtilConst.FIELD_TEXT, cleanText, Field.Store.YES));
               
        indexWriter.addDocument(luceneDoc);
        
        if (inpDoc.mIsRel != null) {
          saveQrelOneEntry(qrelWriter, inpDoc.mQueryId, inpDoc.mDocId, inpDoc.mIsRel ? 1:0);
        }
        if (docNum % 1000 == 0) 
          System.out.println(String.format("Indexed %d documents", docNum));

      }
      
    } catch (ParseException e) {
      e.printStackTrace(); 	
      Usage("Cannot parse arguments" + e, options);
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    } finally {
      System.out.println(String.format("Indexed %d documents", docNum));
      
      try {
        if (null != indexWriter) indexWriter.close();
        if (null != qrelWriter) qrelWriter.close();
      } catch (IOException e) {
        System.err.println("IO exception: " + e);
        e.printStackTrace();
      }
    }
  }

  protected static final String NL = System.getProperty("line.separator");
  
  /**
   * Add one line to the TREC QREL file. 
   * 
   * @param qrelFile
   * @param topicId
   * @param docId
   * @param relGrade
   */
  public static void saveQrelOneEntry(BufferedWriter qrelFile,
                                      String           topicId,
                                      String           docId,
                                      int              relGrade) throws IOException {
    qrelFile.write(String.format("%s 0 %s %d%s", topicId, docId, relGrade, NL));
  }
}
