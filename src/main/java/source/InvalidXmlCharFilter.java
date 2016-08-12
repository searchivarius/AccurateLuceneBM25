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

/**
 * Filtering out invalid XML characters.
 * 
 * @author Leonid Boytsov
 *
 */

public class InvalidXmlCharFilter extends Reader {
  private final Reader mReader;

  public InvalidXmlCharFilter(Reader reader) {
    mReader = reader;
  }

  // For the reference see https://en.wikipedia.org/wiki/Valid_characters_in_XML 
  private static char replaceInvalidChar(char c) {
    boolean isGood = false;
    if (c < 0x20) {
      isGood = (c==0x9) || (c==0xA) || (c==0xD);
    } else {
      if (c <= 0xD7FF) {
        // U+0020 -> U+D7FF
        isGood = true;
        // U+E000 -> U+FFFD
      } else isGood = (c >= 0xE000) && (c <= 0xFFFD);
    }
    if (!isGood) {
      System.err.println(String.format("Ignoring invalid Unicode character with the code 0x%X", (long)c));
      return ' ';
    }
    return c;
  }
  
  @Override
  public int read(char[] buff, int offset, int len) throws IOException {
    int nRead = mReader.read(buff, offset, len);
    if (nRead == -1) {
      return -1;
    }
    for (int i = offset; i < offset + nRead; ++i) {
      buff[i] = replaceInvalidChar(buff[i]);
    }
    return nRead;
  }

  @Override
  public void close() throws IOException {
    mReader.close();
  }  
}
