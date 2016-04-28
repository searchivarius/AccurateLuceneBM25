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
import java.util.*;
import java.io.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import com.google.common.base.Splitter;

public class LuceneCandidateProvider {
  /**
   * Determines if a QREL label defines a relevant entry.
   * 
   * @param label       the string label (can be null).
   * @param minRelLevel the minimum value to be considered relevant
   * @return true if the relevance is at least minRelLevel or false, if the
   *         the label is null.
   * @throws Exception throws an exception if the label is not numeric
   */
  public boolean isRelevLabel(String label, int minRelLevel) throws Exception {
    if (null == label) return false;
    int relVal = 0;
    try {
      relVal = Integer.parseInt(label);
    } catch (NumberFormatException e) {
      throw new Exception("Label '" + label + "' is not numeric!");
    }
    return relVal >= minRelLevel;
  }

  public LuceneCandidateProvider(String indexDirName, Analyzer analyzer, Similarity similarity) throws Exception {
    File indexDir = new File(indexDirName);
    mSimilarity = similarity;
    mAnalyzer = analyzer;
    
    if (!indexDir.exists()) {
      throw new Exception(String.format("Directory '%s' doesn't exist", indexDirName)); 
    }
    mReader = DirectoryReader.open(FSDirectory.open(indexDir));
    mSearcher = new IndexSearcher(mReader);
    mSearcher.setSimilarity(mSimilarity);
    
    mParser = new QueryParser(UtilConst.FIELD_TEXT, mAnalyzer);
    mParser.setDefaultOperator(QueryParser.OR_OPERATOR);
  }
  
  public ResEntry[] getCandidates(int queryNum, 
                                String query, 
                                int maxQty) throws Exception {
    ArrayList<String>   toks = new ArrayList<String>();
    for (String s: mSpaceSplit.split(query)) {  
      toks.add(s);
    }
    if (2 * toks.size() > BooleanQuery.getMaxClauseCount()) {
      // This a heuristic, but it should work fine in many cases
      BooleanQuery.setMaxClauseCount(2 * toks.size());
    }

    ArrayList<ResEntry> resArr = new ArrayList<ResEntry>();
    
    Query       queryParsed = mParser.parse(query);
    
    TopDocs     hits = mSearcher.search(queryParsed, maxQty);
    ScoreDoc[]  scoreDocs = hits.scoreDocs;
    
    for (ScoreDoc oneHit: scoreDocs) {
      Document doc = mSearcher.doc(oneHit.doc);
      String id = doc.get(UtilConst.FIELD_ID);
      float score = oneHit.score;
      
      resArr.add(new ResEntry(id, score));
    }
    
    ResEntry[] results = resArr.toArray(new ResEntry[resArr.size()]);
    Arrays.sort(results);
        
    return results;
  }
  
  private IndexReader   mReader = null;
  private IndexSearcher mSearcher = null;
  private Similarity    mSimilarity = null;
  private Analyzer      mAnalyzer = null;
  private QueryParser   mParser = null;

  private static Splitter mSpaceSplit = Splitter.on(' ').omitEmptyStrings().trimResults();

}
