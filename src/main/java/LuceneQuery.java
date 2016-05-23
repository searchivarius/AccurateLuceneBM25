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
import java.util.Random;

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
  
  static void Usage(String err, Options opt) {
    System.err.println("Error: " + err);
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( "LuceneQuery", opt);      
    System.exit(1);
  } 

  public static void main(String[] args) {
    Options options = new Options();
    
    options.addOption("d",      null, true, "index directory");
    options.addOption("i",      null, true, "input file");
    options.addOption("s",      null, true, "stop word file");
    options.addOption("n",      null, true, "max # of results");
    options.addOption("o",      null, true, "a TREC-style output file");
    options.addOption("r",      null, true, "an optional QREL file, if specified," +
        "we save results only for queries for which we find at least one relevant entry.");
    
    options.addOption("prob",       null, true, "question sampling probability");
    options.addOption("max_query_qty", null, true, "a maximum number of queries to run");
    options.addOption("bm25_b",     null, true, "BM25 parameter: b");
    options.addOption("bm25_k1",    null, true, "BM25 parameter: k1");
    options.addOption("bm25fixed",  null, false, "use the fixed BM25 similarity");
    
    options.addOption("seed",       null, true, "random seed");

    CommandLineParser parser = new org.apache.commons.cli.GnuParser(); 
    
    QrelReader qrels = null;
    
    try {
      
      CommandLine cmd = parser.parse(options, args);
      
      String indexDir = null;
      
      if (cmd.hasOption("d")) {
        indexDir = cmd.getOptionValue("d");
      } else {
        Usage("Specify 'index directory'", options); 
      }
      
      String inputFileName = null;
      
      if (cmd.hasOption("i")) {
        inputFileName = cmd.getOptionValue("i");
      } else {
        Usage("Specify 'input file'", options);
      }
      
      DictNoComments    stopWords = null;
      
      if (cmd.hasOption("s")) {
        String stopWordFileName = cmd.getOptionValue("s");
        stopWords = new DictNoComments(new File(stopWordFileName), true /* lowercasing */);
        System.out.println("Using the stopword file: " + stopWordFileName);
      }      
      
      int numRet = 100;
      
      if (cmd.hasOption("n")) {
        numRet = Integer.parseInt(cmd.getOptionValue("n"));
        System.out.println("Retrieving at most " + numRet + " candidate entries.");
      }
      
      String trecOutFileName = null;
      
      if (cmd.hasOption("o")) {
        trecOutFileName = cmd.getOptionValue("o");
      } else {
        Usage("Specify 'a TREC-style output file'", options);
      }
      
      double fProb = 1.0f;
      
      if (cmd.hasOption("prob")) {
        try {
          fProb = Double.parseDouble(cmd.getOptionValue("prob"));
        } catch (NumberFormatException e) {
          Usage("Wrong format for 'question sampling probability'", options);
        }
      }   
      
      if (fProb <= 0 || fProb > 1) {
        Usage("Question sampling probability should be >0 and <=1", options);
      }
      
      System.out.println("Sample the following fraction of questions: " + fProb);
      
      float bm25_k1 = 1.2f, bm25_b = 0.75f;
      
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
      
      long seed = 0;
      
      String tmpl = cmd.getOptionValue("seed");
      
      if (tmpl != null) seed = Long.parseLong(tmpl);
      
      System.out.println("Using seed: " + seed);
      
      Random randGen = new Random(seed);
      
      System.out.println(String.format("BM25 parameters k1=%f b=%f ", bm25_k1, bm25_b));
      
      boolean useFixedBM25 = cmd.hasOption("bm25fixed");
      
      EnglishAnalyzer   analyzer = new EnglishAnalyzer();
      Similarity        similarity = null;
      
      if (useFixedBM25) {
        System.out.println("Using fixed BM25Simlarity!");
        similarity = new BM25SimilarityFix(bm25_k1, bm25_b);
      } else {
        System.out.println("Using Lucene BM25Similarity");
        similarity = new BM25Similarity(bm25_k1, bm25_b);
      }
      
      int maxQueryQty = Integer.MAX_VALUE;
      
      if (cmd.hasOption("max_query_qty")) {
        try {
          maxQueryQty = Integer.parseInt(cmd.getOptionValue("max_query_qty"));
        } catch (NumberFormatException e) {
          Usage("Wrong format for 'max_query_qty'", options);
        }
      }
      
      System.out.println(String.format(
          "Executing at most %d queries", maxQueryQty));      
      
      if (cmd.hasOption("r")) {
        String qrelFile = cmd.getOptionValue("r");
        System.out.println("Using the qrel file: '" + qrelFile + "', queries not returning a relevant entry will be ignored.");
        qrels = new QrelReader(qrelFile);
      }      
      
      LuceneCandidateProvider candProvider = new LuceneCandidateProvider(indexDir, analyzer, similarity);
      TextCleaner             textCleaner = new TextCleaner(stopWords);
      

      // Let's re-read
      YahooAnswersStreamParser inpDoc = new YahooAnswersStreamParser(inputFileName,
          UtilConst.DO_XML_CLEANUP
          );
        
      BufferedWriter trecOutFile = 
          new BufferedWriter(new FileWriter(new File(trecOutFileName)));
      
      int questNum = 0;
      
      long totalTimeMS = 0;
      
      while (inpDoc.hasNext()) {
        if (questNum >= maxQueryQty) break;
        ++questNum;
        
        ParsedQuestion  quest = inpDoc.next();
        String queryID = quest.mQuestUri;
        
        if (randGen.nextDouble() <= fProb) {
          // Using both the question and the content (i.e., detail field)
          String rawQuest = quest.mQuestion + " " + quest.mQuestDetail;
          String tokQuery = textCleaner.cleanUp(rawQuest);
          String query = TextCleaner.luceneSafeCleanUp(tokQuery).trim();
          
//            System.out.println("=====================");
//            System.out.println(rawQuest);
//            System.out.println("=====================");
//            System.out.println(query);
//            System.out.println("#####################");            
          
          ResEntry [] results = null;
          
          if (query.isEmpty()) {
            results = new ResEntry[0];
            System.out.println(
                String.format("WARNING, empty query id = '%s'", quest.mQuestUri));
          } else {
            
            try {
              long start = System.currentTimeMillis();
              
              results = candProvider.getCandidates(questNum, query, 
                                                             numRet);
              
              long end = System.currentTimeMillis();
              long searchTimeMS = end - start;
              totalTimeMS += searchTimeMS;
              
              System.out.println(String.format("Obtained results for the query # %d, the search took %d ms, we asked for max %d entries got %d", 
                                 questNum, searchTimeMS, numRet, results.length));

              
            } catch (ParseException e) {
              e.printStackTrace();
              System.err.println("Error parsing query: " + query + " orig question is :" 
                                 + rawQuest);
              System.exit(1);
            }
          }
          
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
          System.out.println(String.format("Proccessed %d questions", questNum));

        
      }
      
      System.out.println(String.format("Proccessed %d questions, the search took %f MS on average", questNum, (float)totalTimeMS/questNum));        
      
      trecOutFile.close();
      inpDoc.close();

      
    } catch (ParseException e) {
      e.printStackTrace();
      Usage("Cannot parse arguments: " + e, options);
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
