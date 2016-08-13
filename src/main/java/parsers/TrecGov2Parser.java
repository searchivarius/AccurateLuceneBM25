package parsers;

/*
 * Modified by Leonid Boytsov.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import org.apache.lucene.benchmark.byTask.feeds.DocData;

/**
 * Parser for the GOV2 collection format
 */
public class TrecGov2Parser extends TrecDocParser {

  private static final String DATE = "Date: ";
  private static final String DATE_END = TrecContentSource.NEW_LINE;
  
  private static final String DOCNO= "<DOCNO>";
  private static final String TERMINATING_DOCNO = "</DOCNO>";
  
  private static final String DOCHDR = "<DOCHDR>";
  private static final String TERMINATING_DOCHDR = "</DOCHDR>";

  @Override
  public DocData parse(DocData docData, String name, TrecContentSource trecSrc, 
      StringBuilder docBuf, ParsePathType pathType) throws IOException {
    // skip some of the non-html text, optionally set date
    Date date = null;
    int start = 0;
    final int h1 = docBuf.indexOf(DOCHDR);
    if (h1 >= 0) {
      final int hStart2dLine = h1 + DOCHDR.length() + 1;
      final int hEnd2dLine = docBuf.indexOf("\n", hStart2dLine);
      
      if (hEnd2dLine >= 0) {
        String url = docBuf.substring(hStart2dLine, hEnd2dLine)
            .toLowerCase().trim();

        if (url.startsWith("http://") || 
            url.startsWith("ftp://") ||
            url.startsWith("https://")
            ) {          
          final int h2 = docBuf.indexOf(TERMINATING_DOCHDR, h1);
          final String dateStr = extract(docBuf, DATE, DATE_END, h2, null);
          if (dateStr != null) {
            date = trecSrc.parseDate(dateStr);
          }
          start = h2 + TERMINATING_DOCHDR.length();

          final String html = docBuf.substring(start);
          docData = trecSrc.getHtmlParser().parse(docData, name, date, new StringReader(html), trecSrc);
          // This should be done after parse(), b/c parse() resets properties
          docData.getProps().put("url", url);
          docData.setName(name);
          return docData;
        } else {
          System.err.println("Ignoring schema in URI: " + url);  
        }
      } else {
        throw new RuntimeException("Invalid header: " + docBuf.toString());
      }
    }
    
    /*
     *  TODO: @leo What do we do here exactly? 
     *  The interface doesn't allow us to signal that an entry should be skipped. 
     */    
    
    return docData;
  } 
}
