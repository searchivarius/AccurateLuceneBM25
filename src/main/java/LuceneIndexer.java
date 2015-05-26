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
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import java.io.*;


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
  static void Usage(String err) {
    System.err.println("Error: " + err);
    System.err.println("Usage: -i <input file> " +
                       "-o <output directory> " +
                       "-r <output TREC-format QREL file>");

    System.exit(1);
  }  
  
  public static void main(String [] args) {
    Options options = new Options();
    
    options.addOption("i", null, true, "input file");
    options.addOption("o", null, true, "output directory");
    options.addOption("r", null, true, "output TREC-format QREL file");
    
    CommandLineParser parser = new org.apache.commons.cli.GnuParser();
    
    try {
      CommandLine cmd = parser.parse(options, args);
      
      String inputFileName = null, outputDirName = null, qrelFileName = null;
      
      if (cmd.hasOption("i")) {
        inputFileName = cmd.getOptionValue("i");
      } else {
        Usage("Specify 'input file'");
      }
      
      if (cmd.hasOption("o")) {
        outputDirName = cmd.getOptionValue("o");
      } else {
        Usage("Specify 'index directory'");
      }
      
      if (cmd.hasOption("r")) {
        qrelFileName = cmd.getOptionValue("r");
      } else {
        Usage("Specify 'TREC-format QREL file'");
      }      
      
      BufferedWriter qrelWriter = new BufferedWriter(new FileWriter(qrelFileName));
      
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

      EnglishAnalyzer   analyzer = new EnglishAnalyzer();
      FSDirectory       indexDir    = FSDirectory.open(outputDir);
      IndexWriterConfig indexConf   = new IndexWriterConfig(analyzer.getVersion(), analyzer);
      IndexWriter       indexWriter = new IndexWriter(indexDir, indexConf);
      
      YahooAnswersStreamParser inpDoc = new YahooAnswersStreamParser(inputFileName,
                                                                     UtilConst.DO_XML_CLEANUP
                                                                     );
      TextCleaner              textCleaner = new TextCleaner();
      
      int answNum = 0;
      int questNum = 0;
      while (inpDoc.hasNext()) {
        ++questNum;
        
        ParsedQuestion  quest = inpDoc.next();
        
        for (int answId = 0; answId < quest.mAnswers.size(); ++answId) {                  
          Document  luceneDoc = new Document();
          
          String    id = quest.mQuestUri + "-" + answId;
          String    rawAnswer = quest.mAnswers.get(answId);
          String    cleanAnswer = textCleaner.cleanUp(rawAnswer);
          
//          System.out.println("=====================");
//          System.out.println(rawAnswer);
//          System.out.println("=====================");
//          System.out.println(cleanAnswer);
//          System.out.println("#####################");
        
          luceneDoc.add(new StringField(UtilConst.FIELD_ID, id, Field.Store.YES));
          luceneDoc.add(new TextField(UtilConst.FIELD_TEXT, cleanAnswer, Field.Store.YES));
               
          indexWriter.addDocument(luceneDoc);
          
          saveQrelOneEntry(qrelWriter, quest.mQuestUri, id, answId == quest.mBestAnswId ? 1:0);
          ++answNum;
        }
        if (questNum % 1000 == 0) 
          System.out.println(String.format("Indexed %d questions (%d answers)", questNum, answNum));

      }
      
      System.out.println(String.format("Indexed %d questions (%d answers)", questNum, answNum));
      
      indexWriter.close();
      qrelWriter.close();
    } catch (ParseException e) {
      Usage("Cannot parse arguments");
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
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
