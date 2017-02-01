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
import java.util.ArrayList;
import java.util.Properties;

import utils.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


/**
 * 
 * A simple class to extract a "clean" sequence of tokens from a potentially
 * messy text.
 * 
 * @author Leonid Boytsov, modeled after http://nlp.stanford.edu/software/corenlp.shtml
 *
 */
public class TextCleaner {
  public TextCleaner(DictNoComments stopWords) {
    initTextCleaner(stopWords, UtilConst.USE_STANFORD, UtilConst.DO_LEMMATIZE);
  }
  public TextCleaner(DictNoComments stopWords, boolean bUseStanford, boolean bLemmatize) {
    initTextCleaner(stopWords, bUseStanford, bLemmatize);
  }
  private void initTextCleaner(DictNoComments stopWords, 
                               boolean useStanford, 
                               boolean lemmatize) {
    mStopWords = stopWords;
    mUseStanford = useStanford;
    if (mUseStanford) {
      mLemmatize = lemmatize;
      Properties props = new Properties();
      if (lemmatize)
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
      else
        props.setProperty("annotators", "tokenize");
      
      mPipeline = new StanfordCoreNLP(props);
    }
  }
  
  public ArrayList<String> cleanUp(String text) {
    ArrayList<String>  res = new ArrayList<String>();
    
    if (mUseStanford) {
      Annotation doc = new Annotation(text);
      mPipeline.annotate(doc);
      
 
      
      for (CoreLabel token: doc.get(CoreAnnotations.TokensAnnotation.class)) {
        String word = mLemmatize ?
                      token.get(LemmaAnnotation.class) :
                      token.get(TextAnnotation.class);
        
        word = word.toLowerCase();
        // Ignore stop words if the stopword dictionary is present
//        System.out.println(String.format("%s %b", word, mStopWords.contains(word)));
        if (mStopWords != null && mStopWords.contains(word)) continue; 
        res.add(word);
      }
    } else {
      // If Stanford is not present using a simpler tokenizer.
      for (String s: text.replaceAll("\\s", " ").split("[!:;, ]+")) {
        String word = s.toLowerCase();
        // Ignore stop words if the stopword dictionary is present
        if (mStopWords != null && mStopWords.contains(word)) continue; 
        res.add(word);        
      }
    }
    return res;
  }
  
  /**
   * Removes characters that get a special treatment by the Lucene query parser.
   * 
   */
  public static String luceneSafeCleanUp(String s) {
    return s.replace('+', ' ').replaceAll("[-&|!(){}\\[\\]^\"~*?:\\\\/]", " ");
  }
  
  private StanfordCoreNLP   mPipeline = null;
  private DictNoComments    mStopWords = null;
  private boolean           mLemmatize = false;
  private boolean           mUseStanford = false;
}
