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

import java.io.*;

import utils.CompressUtils;

public class TrecWebQuerySource implements QuerySource {

  public TrecWebQuerySource(String fileName) throws IOException {
    mInp = new BufferedReader(new InputStreamReader(CompressUtils.createInputStream(fileName)));
  }

  @Override
  public QueryEntry next() throws IOException {
    String s = mInp.readLine();
    if (s == null) {
      return null;
    }
    int ch1 = s.indexOf(':');
    int ch2 = s.indexOf(':', ch1+1);
    if (!(ch1 >= 0 && ch2 > ch1)) {
      throw new IOException("Invalid entry format: '" + s + "'");
    }   
    QueryEntry qe = new QueryEntry(s.substring(0, ch1), s.substring(ch2+1));
    System.out.println(qe.mQueryId + " " + qe.mQueryText);
    return qe;
  }
  
  BufferedReader mInp;
}
