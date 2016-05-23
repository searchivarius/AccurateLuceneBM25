
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
        isGood = true;
      } else isGood = (c <= 0xE000) && (c <= 0xFFFD);
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
