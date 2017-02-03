/*
 *  Copyright 2017 Carnegie Mellon University
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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.lemurproject.kstem.KrovetzStemmer;

import com.google.common.base.Joiner;

import source.TextCleaner;
import utils.CompressUtils;
import utils.DictNoComments;
import utils.UtilConst;
import utils.XmlHelper;

public class QueryReaderFactory {
  protected static final String NL = System.getProperty("line.separator");

  public static QueryReader createReader(String fileName, String readerType) throws Exception {
    if (readerType.equalsIgnoreCase("tera")) {
      return new TrecTerabyteQueryReader(fileName);
    }
    if (readerType.equalsIgnoreCase("simple1")) {
      return new SimpleFormatQueryReader(fileName, 1);
    }
    if (readerType.equalsIgnoreCase("simple2")) {
      return new SimpleFormatQueryReader(fileName, 2);
    }    
    throw new Exception("Unknown reader type: '" + readerType + "'");
  }  
  
  public static void main(String [] argv) {
    if (argv.length != 3) {
      System.err.println("Usage: <input file> <input type> <output file>");
    }
    
    BufferedWriter outputFile = null;
    
    try {
      QueryReader qr = createReader(argv[0], argv[1]);
      
      outputFile = new BufferedWriter(new OutputStreamWriter(CompressUtils.createOutputStream(argv[2])));
      
      XmlHelper xmlHlp = new XmlHelper();
      
      TextCleaner    textCleaner = 
          new TextCleaner(new DictNoComments(new File("data/stopwords.txt"), true /* lower case */), 
                          Source2XML.USE_STANFORD, Source2XML.USE_LEMMATIZER);
      
      Joiner   spaceJoin  = Joiner.on(' ');
      
      //Stemmer stemmer = new Stemmer();
      KrovetzStemmer stemmer = new KrovetzStemmer();
      
      System.out.println("Using Stanford NLP?        " + Source2XML.USE_STANFORD);
      System.out.println("Using Stanford lemmatizer? " + Source2XML.USE_LEMMATIZER);
      System.out.println("Using stemmer?             " + Source2XML.USE_STEMMER + 
                          (Source2XML.USE_STEMMER ? " (class: " + stemmer.getClass().getCanonicalName() +")" :""));
      
      
      for (int qid = 0; qid < qr.getQueryQty(); ++qid) {
        Map<String,String> outputMap = new HashMap<String,String>();

        ArrayList<String> toks = textCleaner.cleanUp(qr.getQuery(qid));
        
        outputMap.put(UtilConst.XML_FIELD_DOCNO, qr.getQueryId(qid));
        outputMap.put(UtilConst.XML_FIELD_TEXT,  spaceJoin.join(toks));   
        
        String xml = xmlHlp.genXMLIndexEntry(outputMap);
        
        try {
          outputFile.write(xml);
          outputFile.write(NL);
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println("Error processing/saving a document!");
        }        
      }                 
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      System.err.println("Terminated due to exception: " + e);
      System.exit(1);
    } finally {
      try {
        if (null != outputFile) outputFile.close();
      } catch (IOException e) {
        System.err.println("IO exception: " + e);
        e.printStackTrace();
      }
    }
  }
}



/**
 * A reader of TREC Terabyte (2005-2006) ad hoc title queries.
 * 
 * @author Leonid Boytsov
 *
 */
class TrecTerabyteQueryReader extends QueryReader {
  TrecTerabyteQueryReader(String fileName) throws Exception {
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
    
    String id = null;
    String ID_PREFIX = "<num> Number: ";
    String TITLE_PREFIX = "<title> ";
    
    String line;
    int lineNum = 0;
    while ((line = r.readLine()) != null) {
      ++lineNum;
      line = line.trim();
      if (line.equals("<top>")) {
        id = null;
      } else if (line.startsWith(ID_PREFIX)) {
        id = line.substring(ID_PREFIX.length());
      } else if (line.startsWith(TITLE_PREFIX)) {
        if (null == id)
          throw new Exception("There is no query ID found for the query in line: " + lineNum + " file: '" + fileName);
        mQueryIds.add(id);
        mQueries.add(line.substring(TITLE_PREFIX.length()));
      }
    }
    
    r.close();
  }
}
  
/**
 * A reader of queries in a simpler format, where each query occupies a single line preceeded by several
 * colon-separated numbers (query ID is always the first number).
 */
class SimpleFormatQueryReader extends QueryReader {
  /**
   * Constructor
   * 
   * @param fileName input file name
   * @param numCol the expected number of colons
   * @throws Exception
   */
  SimpleFormatQueryReader(String fileName, int numCol) throws Exception {      
    if (numCol < 1) 
      throw new Exception("numCol in the constructor of " + this.getClass().getCanonicalName() + " should be >=1");
    
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
    
    String line;
    int lineNum = 0;
    while ((line = r.readLine()) != null) {
      ++lineNum;
      line = line.trim();
      int idx = line.indexOf(':');
      if (idx < 0) 
        throw new Exception("First colon not found in line: " + lineNum + " file: '" + fileName);

      mQueryIds.add(line.substring(0, idx));
      
      for (int cid = 2; cid <= numCol; ++cid) {
        idx = line.indexOf(':', idx + 1);
        if (idx < 0)
          throw new Exception("Colon # " + cid + " not found in line: " + lineNum + " file: '" + fileName);
      }
      mQueries.add(line.substring(idx+1));        
    }
    
    r.close();
  }
}

