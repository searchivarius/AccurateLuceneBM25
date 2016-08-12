package parsers;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import parsers.TrecContentSource.DateFormatInfo;



/*
 * 
 * Date helper functions & data
 * from org.apache.lucene.benchmark.byTask.feeds.TrecContentSource
 * 
 */
import org.apache.lucene.benchmark.byTask.feeds.ContentSource;

public abstract class ContentSourceDateUtil extends ContentSource {
  
  protected static final String DATE_FORMATS [] = {
    "EEE, dd MMM yyyy kk:mm:ss z",   // Tue, 09 Dec 2003 22:39:08 GMT
    "EEE MMM dd kk:mm:ss yyyy z",    // Tue Dec 09 16:45:08 2003 EST
    "EEE, dd-MMM-':'y kk:mm:ss z",   // Tue, 09 Dec 2003 22:39:08 GMT
    "EEE, dd-MMM-yyy kk:mm:ss z",    // Tue, 09 Dec 2003 22:39:08 GMT
    "EEE MMM dd kk:mm:ss yyyy",      // Tue Dec 09 16:45:08 2003
    "dd MMM yyyy",                   // 1 March 1994
    "MMM dd, yyyy",                  // February 3, 1994
    "yyMMdd",                        // 910513
    "hhmm z.z.z. MMM dd, yyyy",       // 0901 u.t.c. April 28, 1994
  };

  protected ThreadLocal<DateFormatInfo> dateFormats = new ThreadLocal<DateFormatInfo>();
  
  protected DateFormatInfo getDateFormatInfo() {
    DateFormatInfo dfi = dateFormats.get();
    if (dfi == null) {
      dfi = new DateFormatInfo();
      dfi.dfs = new SimpleDateFormat[DATE_FORMATS.length];
      for (int i = 0; i < dfi.dfs.length; i++) {
        dfi.dfs[i] = new SimpleDateFormat(DATE_FORMATS[i], Locale.ROOT);
        dfi.dfs[i].setLenient(true);
      }
      dfi.pos = new ParsePosition(0);
      dateFormats.set(dfi);
    }
    return dfi;
  }

  public Date parseDate(String dateStr) {
    dateStr = dateStr.trim();
    DateFormatInfo dfi = getDateFormatInfo();
    for (int i = 0; i < dfi.dfs.length; i++) {
      DateFormat df = dfi.dfs[i];
      dfi.pos.setIndex(0);
      dfi.pos.setErrorIndex(-1);
      Date d = df.parse(dateStr, dfi.pos);
      if (d != null) {
        // Parse succeeded.
        return d;
      }
    }
    // do not fail test just because a date could not be parsed
    if (verbose) {
      System.out.println("failed to parse date (assigning 'now') for: " + dateStr);
    }
    return null; 
  }  
}
