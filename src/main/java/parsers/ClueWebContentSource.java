package parsers;

/*
 * Re-using some of the code from 
 * org.apache.lucene.benchmark.byTask.feeds.TrecContentSource
 * 
 * This code is released under the
 * Apache License Version 2.0 http://www.apache.org/licenses/.
 * 
 */

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.comparator.NameFileComparator;

import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.benchmark.byTask.utils.StreamUtils;

import edu.cmu.lemurproject.WarcRecord;;

/**
 * Implements a ContentSource for the TREC ClueWeb09/12 collections.
 * <p>
 * Supports the following configuration parameters (on top of
 * {@link ContentSource}):
 * <ul>
 * <li><b>work.dir</b> - specifies the working directory. Required if "docs.dir"
 * denotes a relative path (<b>default=work</b>).
 * <li><b>docs.dir</b> - specifies the directory where the TREC files reside.
 * Can be set to a relative path if "work.dir" is also specified
 * (<b>default=trec</b>).
 * <li><b>html.parser</b> - specifies the {@link HTMLParser} class to use for
 * parsing the HTML parts of the TREC documents content (<b>default=DemoHTMLParser</b>).
 * </ul>
 * 
 * @author Leonid Boytsov
 * 
 * Re-using some of the code from org.apache.lucene.benchmark.byTask.feeds.TrecContentSource
 * 
 */
public class ClueWebContentSource extends ContentSourceDateUtil {

  static final class DateFormatInfo {
    DateFormat[] dfs;
    ParsePosition pos;
  }

  private File dataDir = null;
  private ArrayList<Path> inputFiles = new ArrayList<Path>();
  private int nextFile = 0;
  // Use to synchronize threads on reading from the TREC documents.
  private Object lock = new Object();

  // Required for test
  DataInputStream reader;
  int iteration = 0;
  HTMLParser htmlParser;
    
  void openNextFile() throws NoMoreDataException, IOException {
    close();

    while (true) {
      if (nextFile >= inputFiles.size()) { 
        // exhausted files, start a new round, unless forever set to false.
        if (!forever) {
          throw new NoMoreDataException();
        }
        nextFile = 0;
        iteration++;
      }
      Path f = inputFiles.get(nextFile++);
      if (verbose) {
        System.out.println("opening: " + f + " length: " + f.toFile().length());
      }
      try {
        // supports gzip, bzip2, or regular text file, extension is used to detect
        InputStream inputStream = StreamUtils.inputStream(f);   
        reader = new DataInputStream(inputStream);
        return;
      } catch (Exception e) {
        if (verbose) {
          System.out.println("Skipping 'bad' file " + f.toFile().getAbsolutePath()+" due to "+e.getMessage());
          continue;
        }
        throw new NoMoreDataException();
      }
    }
  }
  
  @Override
  public void close() throws IOException {
    if (reader == null) {
      return;
    }

    try {
      reader.close();
    } catch (IOException e) {
      if (verbose) {
        System.out.println("failed to close reader !");
        e.printStackTrace(System.out);
      }
    }
    reader = null;
  }
  

  
  @Override
  public DocData getNextDocData(DocData docData) throws NoMoreDataException, IOException {
    WarcRecord  CurrRec = null;
    
    // protect reading from the TREC files by multiple threads. The rest of the
    // method, i.e., parsing the content and returning the DocData can run unprotected.
    synchronized (lock) {
      if (reader == null) {
        openNextFile();
      }
      
      do {
        CurrRec = WarcRecord.readNextWarcRecord(reader);
        /*
         *  We need to skip special auxiliary entries, e.g., in the
         *  beginning of the file.
         */
        
      } while (CurrRec != null && !CurrRec.getHeaderRecordType().equals("response"));
      
      if (CurrRec == null) {
        openNextFile();
        return getNextDocData(docData);
      }
    }       
 
    Date    date = parseDate(CurrRec.getHeaderMetadataItem("WARC-Date"));    
    String  url = CurrRec.getHeaderMetadataItem("WARC-Target-URI");
    String  trecId = CurrRec.getHeaderMetadataItem("WARC-TREC-ID");
    
    if (null == trecId)
      throw new RuntimeException("No WARC-TREC-ID field for url: '" + url + "'");
      
      
    // This code segment relies on HtmlParser being thread safe. When we get 
    // here, everything else is already private to that thread, so we're safe.
    if (url.startsWith("http://") || 
        url.startsWith("ftp://") ||
        url.startsWith("https://")
        ) {          
      String Response = CurrRec.getContentUTF8();

      int EndOfHead = Response.indexOf("\n\n");
      
      if (EndOfHead >= 0) {
        String html = Response.substring(EndOfHead + 2);
                
        docData = htmlParser.parse(docData, url, date, new StringReader(html), this);
     // This should be done after parse(), b/c parse() resets properties
        docData.getProps().put("url", url);
        docData.setName(trecId);
        
      } else {
        /*
         *  TODO: @leo What do we do here exactly? 
         *  The interface doesn't allow us to signal that an entry should be skipped. 
         */    
        System.err.println("Cannot extract HTML in URI: " + url);          
      }
    } else {
      /*
       *  TODO: @leo What do we do here exactly? 
       *  The interface doesn't allow us to signal that an entry should be skipped. 
       */    
      System.err.println("Ignoring schema in URI: " + url);  
    }

    addItem();

    return docData;
  }

  @Override
  public void resetInputs() throws IOException {
    synchronized (lock) {
      super.resetInputs();
      close();
      nextFile = 0;
      iteration = 0;
    }
  }

  @Override
  public void setConfig(Config config) {
    super.setConfig(config);
    
    // dirs
    File workDir = new File(config.get("work.dir", "work"));
    String d = config.get("docs.dir", "trec");
    dataDir = new File(d);
    if (!dataDir.isAbsolute()) {
      dataDir = new File(workDir, d);
    }

    try {
      // files: accept only WARC files
      ArrayList<Path> tmpp = new ArrayList<Path>();
      collectFiles(dataDir.toPath(), tmpp);
      
      ArrayList<File> tmpf = new ArrayList<File>();
      for (Path p : tmpp) 
      if (p.endsWith("warc.gz")) {
        tmpf.add(p.toFile());
      }
      NameFileComparator c = new NameFileComparator();
      tmpf.sort(c);
      for (File f : tmpf) inputFiles.add(f.toPath());
      
      if (inputFiles.size() == 0) {
        throw new IllegalArgumentException("No files in dataDir: " + dataDir);
      }

      // html parser      
      
      String htmlParserClassName = config.get("html.parser",
          "org.apache.lucene.benchmark.byTask.feeds.DemoHTMLParser");
      htmlParser = Class.forName(htmlParserClassName).asSubclass(HTMLParser.class).newInstance();
    } catch (Exception e) {
      // Should not get here. Throw runtime exception.
      throw new RuntimeException(e);
    }
    
    verbose = true;
  }

}
