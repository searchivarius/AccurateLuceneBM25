/*
 *  Copyright 2016 Carnegie Mellon University
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
package source;

import java.util.*;
import java.io.*;

import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;

import parsers.*;

public class ContentSourceSource implements DocumentSource {

  public static final String SOURCE_TYPE_CLUEWEB    = "CLUEWEB";
  public static final String SOURCE_TYPE_GOV2       = "GOV2";
  public static final String SOURCE_TYPE_WIKIPEDIA  = "WIKIPEDIA";
  
  public ContentSourceSource(String indexType, String indexSource) throws Exception {
    String typeLC = indexType.toUpperCase();
    mProperties = new Properties();
    
    if (typeLC.equals(SOURCE_TYPE_WIKIPEDIA)) {  
      File wikipediafile = new File(indexSource);
      if (!wikipediafile.exists()) {
        throw new Exception("Can't find " + wikipediafile.getAbsolutePath());
      }
      if (!wikipediafile.canRead()) {
        throw new Exception("Can't read " + wikipediafile.getAbsolutePath());
      }

      mProperties.setProperty("docs.file", wikipediafile.getAbsolutePath());
      mProperties.setProperty("keep.image.only.docs", "false");

      mSource = new EnwikiContentSource();
    } else if (typeLC.equals(SOURCE_TYPE_GOV2)) {        
      String parserTREC = "parsers.TrecGov2Parser";


      mProperties.setProperty("html.parser", "parsers.DemoHTMLParser");
      mProperties.setProperty("trec.doc.parser", parserTREC);
      mProperties.setProperty("docs.dir", indexSource);
      mProperties.setProperty("work.dir", "/tmp");

      mSource = new TrecContentSource();        
    } else if (typeLC.equals(SOURCE_TYPE_CLUEWEB)) {
      // parsers.DemoHTMLParser HTML parser fails on this collection
      //mProperties.setProperty("html.parser", "parsers.LeoHTMLParser");
      mProperties.setProperty("html.parser", "parsers.DemoHTMLParser");
      mProperties.setProperty("docs.dir", indexSource);
      mProperties.setProperty("work.dir", "/tmp");

      mSource = new ClueWebContentSource();      
    } else {
      throw new Exception("Unsupported index type: " + indexType);
    }
 
    mConfig = new Config(mProperties);    
    mSource.setConfig(mConfig);
    mSource.resetInputs(); // not clear if this is 100% needed, but let's keep it    
  }
  
  private Config        mConfig;
  private ContentSource mSource;
  private Properties    mProperties;
  private DocData       mDocData = new DocData();

  @Override
  public DocumentEntry next() throws IOException {
    if (mSource == null) return null;
    try {
      mDocData = mSource.getNextDocData(mDocData);
      String docId = mDocData.getName();
      return new DocumentEntry(null /* no respective query ID */, null /* no relevance information */,
                               docId, mDocData.getTitle() + ' ' + mDocData.getBody());
    } catch (NoMoreDataException e) {
      mSource = null;
    }
    return null;
  }

}
