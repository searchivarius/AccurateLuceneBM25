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
import org.apache.commons.cli.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

import java.io.*;

/**
 * <p>A Lucene query application that reads questions from the Yahoo answers
 * manner file, extracts a random sample of queries (subject + content) 
 * and retrieve matching documents. The processes is repeated several times:
 * for each sample we create an output TREC-style run file that can
 * be evaluated using TREC utilities.</p> 
 * 
 * @author Leonid Boytsov
 *
 */

public class LuceneQuery {
  /** 
   * This is a run name to place in a TREC result file, currently we don't
   * employ the run name anywhere and, therefore, opt to use a fake run name value.  
   */
  private final static String TREC_RUN = "fakerun";
  private final static String NL = System.getProperty("line.separator");
  
  static void Usage(String err) {
    System.err.println("Error: " + err);
    System.err.println("Usage: " 
                       + "-d <index directory> "
                       + "-i <input file> "
                       + "-n <max # of results> "
                       + "-o <a prefix for TREC-style output files> "
                       + "-prob <optional question sampling probability> "
                       + "-sample_qty <an optional number of sampling iterations> "
                       + "-bm25_b <optional BM25 parameter: b> "
                       + "-bm25_k1 <optional BM25 parameter: k1> "
                       + "-r <an optional QREL file, if specified, we save results only for queries " +
                         " for which we find at least one relevant entry> "
                       );
    System.exit(1);
  }  

  public static void main(String[] args) {
    Options options = new Options();
    
    options.addOption("d",      null, true, "index directory");
    options.addOption("i",      null, true, "input file");
    options.addOption("n",      null, true, "max # of results");
    options.addOption("o",      null, true, "a prefix for TREC-style output files");
    options.addOption("r",      null, true, "an optional QREL file, if specified," +
        "we save results only for queries for which we find at least one relevant entry.");
    
    options.addOption("prob",       null, true, "question sampling probability");
    options.addOption("sample_qty", null, true, "a number of sampling iterations");
    options.addOption("bm25_b",     null, true, "BM25 parameter: b");
    options.addOption("bm25_k1",    null, true, "BM25 parameter: k1");

    CommandLineParser parser = new org.apache.commons.cli.GnuParser(); 
    
    QrelReader qrels = null;
    
    try {
      CommandLine cmd = parser.parse(options, args);
      
      String indexDir = null;
      
      if (cmd.hasOption("d")) {
        indexDir = cmd.getOptionValue("d");
      } else {
        Usage("Specify 'index directory'"); 
      }
      
      String inputFileName = null;
      
      if (cmd.hasOption("i")) {
        inputFileName = cmd.getOptionValue("i");
      } else {
        Usage("Specify 'input file'");
      }
      
      int numRet = 100;
      
      if (cmd.hasOption("n")) {
        numRet = Integer.parseInt(cmd.getOptionValue("n"));
        System.out.println("Retrieving at most " + numRet + " candidate entries.");
      }
      
      String trecOutFileNamePrefix = null;
      
      if (cmd.hasOption("o")) {
        trecOutFileNamePrefix = cmd.getOptionValue("o");
      } else {
        Usage("Specify 'a prefix for TREC-style output files'");
      }
      
      double fProb = 1.0f;
      
      if (cmd.hasOption("prob")) {
        try {
          fProb = Double.parseDouble(cmd.getOptionValue("prob"));
        } catch (NumberFormatException e) {
          Usage("Wrong format for 'question sampling probability'");
        }
      }   
      
      if (fProb <= 0 || fProb > 1) {
        Usage("Question sampling probability should be >0 and <=1");
      }
      
      System.out.println("Sample the following fraction of questions: " + fProb);
      
      float bm25_k1 = 1.2f, bm25_b = 0.75f;
      
      if (cmd.hasOption("bm25_k1")) {
        try {
          bm25_k1 = Float.parseFloat(cmd.getOptionValue("bm25_k1"));
        } catch (NumberFormatException e) {
          Usage("Wrong format for 'bm25_k1'");
        }
      }
      
      if (cmd.hasOption("bm25_b")) {
        try {
          bm25_b = Float.parseFloat(cmd.getOptionValue("bm25_b"));
        } catch (NumberFormatException e) {
          Usage("Wrong format for 'bm25_b'");
        }
      }   
      
      System.out.println(String.format("BM25 parameters k1=%f b=%f ", bm25_k1, bm25_b));
      
      EnglishAnalyzer   analyzer = new EnglishAnalyzer();
      Similarity        similarity = new BM25Similarity(bm25_k1, bm25_b);
      
      int sampleQty = 1;
      
      if (cmd.hasOption("sample_qty")) {
        try {
          sampleQty = Integer.parseInt(cmd.getOptionValue("sample_qty"));
        } catch (NumberFormatException e) {
          Usage("Wrong format for 'sample_qty'");
        }
      }
      
      System.out.println(String.format(
          "Carrying out %d sampling iterations using probability %f to select a question in each iteration",
                                        sampleQty, fProb));      
      
      if (cmd.hasOption("r")) {
        String qrelFile = cmd.getOptionValue("r");
        System.out.println("Using the qrel file: '" + qrelFile + "', queries not returning a relevant entry will be ignored.");
        qrels = new QrelReader(qrelFile);
      }      
      
      LuceneCandidateProvider candProvider = new LuceneCandidateProvider(indexDir, analyzer, similarity);
      TextCleaner             textCleaner = new TextCleaner();
      
      for (int iterNum = 1; iterNum <= sampleQty; ++iterNum) {
        // Let's re-read
        YahooAnswersStreamParser inpDoc = new YahooAnswersStreamParser(inputFileName,
            UtilConst.DO_XML_CLEANUP
            );
          
        BufferedWriter trecOutFile = 
            new BufferedWriter(new FileWriter(new File(trecOutFileNamePrefix + "." + iterNum)));
        
        int questNum = 0;
        while (inpDoc.hasNext()) {
          ++questNum;
          
          ParsedQuestion  quest = inpDoc.next();
          String queryID = quest.mQuestUri;
          
          if (Math.random() <= fProb) {
            // Using both the question and the content (i.e., detail field)
            String rawQuest = quest.mQuestion + " " + quest.mQuestDetail;
            String tokQuery = textCleaner.cleanUp(rawQuest);
//            System.out.println("=====================");
//            System.out.println(rawQuest);
//            System.out.println("=====================");
//            System.out.println(tokQuery);
//            System.out.println("#####################");
            String query = TextCleaner.luceneSafeCleanUp(tokQuery);
            
            ResEntry [] results = candProvider.getCandidates(questNum, 
                                                             query, 
                                                             numRet);
            boolean bSave = true;
            
            if (qrels != null) {
              boolean bOk = false;
              for (ResEntry r : results) {
                String label = qrels.get(queryID, r.mDocId);
                if (candProvider.isRelevLabel(label, 1)) {
                  bOk = true;
                  break;
                }
              }
              if (!bOk) bSave = false;
            }
            
//            System.out.println(String.format("Ranking results the query # %d queryId='%s' save results? %b", 
//                                              questNum, queryID, bSave));          
            if (bSave) {
              saveTrecResults(queryID, results, trecOutFile, TREC_RUN, numRet);
            }
          }
          
          if (questNum % 1000 == 0) 
            System.out.println(String.format("Proccessed %d questions, iteration %d", questNum, iterNum));

          
        }
        
        System.out.println(String.format("Proccessed %d questions, iteration %d", questNum, iterNum));        
        
        trecOutFile.close();
        inpDoc.close();
      }
      
    } catch (ParseException e) {
      Usage("Cannot parse arguments");
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    }  
  }
  
  /** Some fake document ID, which is unlikely to be equal to a real one */
  private static final String FAKE_DOC_ID = 
      "THIS_IS_A_VERY_LONG_FAKE_DOCUMENT_ID_THAT_SHOULD_NOT_MATCH_ANY_REAL_ONES";
  
    /**
     * Saves query results to a TREC result file.
     * 
     * @param topicId 
     *          a question ID.
     * @param results
     *          found entries to memorize.
     * @param trecFile
     *          an object used to write to the output file.
     * @param runId
     *          a run ID.
     * @param maxNum
     *          a maximum number of results to save (can be less than the number
     *          of retrieved entries).
     * @throws IOException
     */
    public static void saveTrecResults(String topicId, ResEntry[] results,
        BufferedWriter trecFile, String runId, int maxNum) throws IOException {
      boolean bNothing = true;
      for (int i = 0; i < Math.min(results.length, maxNum); ++i) {
        bNothing = false;
        saveTrecOneEntry(trecFile, topicId, results[i].mDocId, (i + 1),
            results[i].mScore, runId);
      }
      /*
       * If nothing is returned, let's a fake entry, otherwise trec_eval will
       * completely ignore output for this query (it won't give us zero as it
       * should have been!)
       */
      if (bNothing) {
        saveTrecOneEntry(trecFile, topicId, FAKE_DOC_ID, 1, 0, runId);
      }
    }

  /**
   * Save positions, scores, etc information for a single retrieved documents.
   * 
   * @param trecFile
   *          an object used to write to the output file.
   * @param topicId
   *          a question ID.
   * @param docId
   *          a document ID of the retrieved document.
   * @param docPos
   *          a position in the result set (the smaller the better).
   * @param score
   *          a score of the document in the result set.
   * @param runId
   *          a run ID.
   * @throws IOException
   */
    private static void saveTrecOneEntry(BufferedWriter trecFile,
                                         String         topicId,
                                         String         docId,
                                         int            docPos,
                                         float          score,
                                         String         runId
                                         ) throws IOException {
      trecFile.write(String.format("%s\tQ0\t%s\t%d\t%f\t%s%s",
          topicId, docId, 
          docPos, score, runId,
          NL));    
    }  
}
