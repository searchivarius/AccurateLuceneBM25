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
package utils;
public class UtilConst {
  /**
   * An answer ID field.
   */
  public static final String FIELD_ID = "ID";
  /**
   * An text field
   */
  public static final String FIELD_TEXT = "TEXT";
  
  /**
   * True, if we lemmatize text during indexing AND retrieval. 
   */
  public static final boolean DO_LEMMATIZE = true;
  /**
   * True, if we use Stanford tokenizer.
   */
  public static final boolean USE_STANFORD = false;
  /**
   * True, if we want to carry out some basic cleanup of the XML fields.
   */
  public static final boolean DO_XML_CLEANUP = true;

  public static final float BM25_K1_DEFAULT= 1.2f;
      
  public static final float BM25_B_DEFAULT = 0.75f;
      
}
