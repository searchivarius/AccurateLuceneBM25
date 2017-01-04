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

import com.google.common.base.Joiner;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import utils.*;
import source.*;


// Partially based on https://lucene.apache.org/core/6_0_0/demo/src-html/org/apache/lucene/demo/IndexFiles.html

/**
 * An application that reads text files in various formats and converts them into the XML format understood
 * by the KNN4QA OAQA application. 
 * 
 * @author Leonid Boytsov
 *
 */

public class Source2XML {

  static void Usage(String err, Options opt) {
    System.err.println("Error: " + err);
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Source2XML", opt);      
    System.exit(1);
  }
  
  static final Pattern mSomePunctPattern = Pattern.compile("[\\[\\]|+\\-#@\\^&~`\\\\]");
  
  public static String replaceSomePunct(String s) {    
    Matcher m = mSomePunctPattern.matcher(s);
    return m.replaceAll(" ");
  }
  
  public static void main(String [] args) {
    Options options = new Options();
    
    options.addOption("i", 			null, true, "input file");
    options.addOption("o", 			null, true, "output file");
    
    Joiner   commaJoin  = Joiner.on(',');
    
    options.addOption("source_type", null, true, 
                      "document source type: " + commaJoin.join(SourceFactory.getDocSourceList()));
    
    CommandLineParser parser = new org.apache.commons.cli.GnuParser();
    
    BufferedWriter    outputFile = null;
    
    int docNum = 0;
    
    try {
      CommandLine cmd = parser.parse(options, args);
      
      String inputFileName = null, outputFileName = null;
      
      if (cmd.hasOption("i")) {
        inputFileName = cmd.getOptionValue("i");
      } else {
        Usage("Specify 'input file'", options);
      }
      
      if (cmd.hasOption("o")) {
        outputFileName = cmd.getOptionValue("o");
      } else {
        Usage("Specify 'output file'", options);
      }
            
      outputFile = new BufferedWriter(new OutputStreamWriter(CompressUtils.createOutputStream(outputFileName)));
          
      String sourceName = cmd.getOptionValue("source_type");
      
      if (sourceName == null)
        Usage("Specify document source type", options);

      
      DocumentSource inpDocSource = SourceFactory.createDocumentSource(sourceName, inputFileName);
      DocumentEntry  inpDoc = null;
      TextCleaner    textCleaner = new TextCleaner(null);
    
      Map<String,String> outputMap = new HashMap<String,String>();

      outputMap.put(UtilConst.XML_FIELD_DOCNO, null);
      outputMap.put(UtilConst.XML_FIELD_TEXT, null);
      
      XmlHelper xmlHlp = new XmlHelper();
      
      while ((inpDoc = inpDocSource.next()) != null) {
        ++docNum;

        String partlyCleanedText = textCleaner.cleanUp(inpDoc.mDocText);
        String cleanText = XmlHelper.removeInvaildXMLChars(partlyCleanedText); 
        cleanText = replaceSomePunct(cleanText);
        
        outputMap.replace(UtilConst.XML_FIELD_DOCNO, inpDoc.mDocId);
        outputMap.replace(UtilConst.XML_FIELD_TEXT,  cleanText);

        String xml = xmlHlp.genXMLIndexEntry(outputMap);
        
        /*
        {
          System.out.println(inpDoc.mDocId);
          System.out.println("=====================");
          System.out.println(partlyCleanedText);
          System.out.println("=====================");
          System.out.println(cleanText);
        } 
        */               
        
        try {
          outputFile.write(xml);
          outputFile.write(NL);
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println("Error processing/saving a document!");
        }
        
        if (docNum % 1000 == 0) 
          System.out.println(String.format("Processed %d documents", docNum));
      }
      
    } catch (ParseException e) {
      e.printStackTrace(); 	
      Usage("Cannot parse arguments" + e, options);
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    } finally {
      System.out.println(String.format("Processed %d documents", docNum));
      
      try {
        if (null != outputFile) outputFile.close();
      } catch (IOException e) {
        System.err.println("IO exception: " + e);
        e.printStackTrace();
      }
    }
  }

  protected static final String NL = System.getProperty("line.separator");
  
}
