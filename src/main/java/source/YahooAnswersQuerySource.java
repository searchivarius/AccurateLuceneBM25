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

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import utils.UtilConst;

public class YahooAnswersQuerySource implements QuerySource {

  public YahooAnswersQuerySource(String fileName) throws IOException, XMLStreamException {
    mSource = new YahooAnswersStreamParser(fileName, UtilConst.DO_XML_CLEANUP);
  }

  @Override
  public QueryEntry next() {
    if (mSource.hasNext()) {
      ParsedQuestion  quest = mSource.next();
      String          queryId = quest.mQuestUri;      
      String          rawQuest = quest.mQuestion + " " + quest.mQuestDetail;
      
      return new QueryEntry(queryId, rawQuest);
    }
    return null;
  }

  YahooAnswersStreamParser  mSource;
  TextCleaner               mTextCleaner = new TextCleaner(null);
}
