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
import javax.xml.stream.*;

import org.apache.commons.lang.exception.ExceptionUtils;

import utils.*;

/**
 * An XML-stream based iterator over Yahoo-Answers collection. Modeled
 * after the code of Di Wang : 
 * https://github.com/Digo/lapps-gigaword-lucene/blob/master/src/main/java/edu/cmu/lti/oaqa/lapps/GwDocIterator.java
 * 
 * @author Leonid Boytsov
 *
 */
public class YahooAnswersStreamParser implements Iterator<ParsedQuestion> {
  XMLStreamReader   mReader;
  ParsedQuestion    mNextDoc = null;
  File              mFile;
  boolean           mDoCleanUp = false;
  
  public YahooAnswersStreamParser(String fileName, boolean bDoCleanUp) 
                                  throws IOException, XMLStreamException {
    mFile = new File(fileName);
    mDoCleanUp = bDoCleanUp;

    final XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.IS_COALESCING, true);
    InputStream is = CompressUtils.createInputStream(fileName);
    mReader = factory.createXMLStreamReader(new InvalidXmlCharFilter(new InputStreamReader(is, "UTF-8")));    
    mNextDoc = fetchNext();
  }
  
  @Override
  public boolean hasNext() {
    return  mNextDoc != null;
  }
  
  @Override
  public ParsedQuestion next() {
    ParsedQuestion doc = mNextDoc;
    if (doc == null) return null;
    try {
      mNextDoc = fetchNext();
    } catch (XMLStreamException e) {
      System.err.println("Error parsing file: " + mFile.getAbsolutePath());
      System.err.println(ExceptionUtils.getFullStackTrace(e));      
      return null;
    }
    return doc;
  }

  private ParsedQuestion fetchNext() throws XMLStreamException {
    ParsedQuestion parsed = null;
    
    String                question       = "";
    String                questDetail    = "";
    String                questUri       = "";
    String                bestAnsw       = "";
    ArrayList<String>     answers = new ArrayList<String>();
    
    boolean               bStart = false;
    
    String                tagContent = null;
    
    while (mReader.hasNext()) {
      switch (mReader.next()) {
        case XMLStreamConstants.START_ELEMENT:
          switch (mReader.getLocalName()) {
            case "document" : // synonym for vespadd 
            case "vespaadd" : bStart = true; break;
          }
          break;
        case XMLStreamConstants.CHARACTERS:
          tagContent = mReader.getText();
          break;
        case XMLStreamConstants.END_ELEMENT:
          switch (mReader.getLocalName()) {
            case "document" : // synonym for vespadd           
            case "vespaadd" : 
              return new ParsedQuestion(question, questDetail, questUri,
                                          answers, bestAnsw, mDoCleanUp);
            case "uri"      : questUri = tagContent; 
                              break;
            case "subject"  : question = tagContent; break;
            case "content"  : questDetail = tagContent; break;
            case "bestanswer": bestAnsw = tagContent; break;
            case "answer_item": answers.add(tagContent); break;
          }

          break;
      }      
    }
   
    return null;    
  }
  
  public void close() throws XMLStreamException {
    mReader.close();
  }

  @Override
  public void remove() {
      throw new UnsupportedOperationException();
  }  
}
