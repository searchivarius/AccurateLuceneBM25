package parsers;
/*
 * HTML-parser/cleaner used in TREC 19,20,21 adhoc.
 *
 * Boytsov, L., Belova, A., 2011. Evaluating Learning-to-Rank Methods in the Web Track Adhoc Task. 
 * In TREC-20: Proceedings of the Nineteenth Text REtrieval Conference.  
 *
 * Author: Leonid Boytsov
 * Copyright (c) 2013
 *
 * This code is released under the
 * Apache License Version 2.0 http://www.apache.org/licenses/.
 * 
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.htmlparser.Parser;
import org.htmlparser.util.ParserException;
import org.xml.sax.InputSource;

/**
 * Simple HTML Parser extracting title, meta tags, and body text
 * that is based on org.htmlparser.
 */
public class LeoHTMLParser implements HTMLParser {
 
  @Override
  public DocData parse(DocData docData, 
                       String name, 
                       Date date, 
                       Reader reader, 
                       ContentSourceDateUtil trecSrc) throws IOException {

    return parse(docData, name, date, new InputSource(reader), trecSrc);
  }
  
  public DocData parse(DocData docData, 
                       String name, 
                       Date date, 
                       InputSource source, 
                       ContentSourceDateUtil trecSrc) 
                       throws IOException {
    String title = "";
    String bodyText = "";
    
    String baseHref = "http://fake-domain.com";
    String encoding = "utf8";

    /*
     * 
     * This is clearly not the most efficient way to parse,
     * but it is much more stable.
     * 
     */
    StringWriter writer = new StringWriter();
    BufferedReader br = new BufferedReader(source.getCharacterStream());
    
    String line;
    while(null != (line = br.readLine())) {
        writer.append(line);
    }
    br.close();
    
    String html = writer.toString();    
    
    try {
      Parser HtmlParser = Parser.createParser(html, encoding);  

      LeoCleanerUtil res = new LeoCleanerUtil(baseHref);      
      HtmlParser.visitAllNodesWith(res);

      
      title = res.GetTitleText();
      
      bodyText = title + " " +  
                 res.GetDescriptionText() + " " + 
                 res.GetKeywordText() + " " + 
                 res.GetBodyText();
      
    } catch (ParserException e) {      
      System.err.println(" Parser exception: " + e + " trying simple conversion");
      // Plan B!!!
      Pair<String,String> sres = LeoCleanerUtil.SimpleProc(html);
      
      title = sres.getFirst();
      bodyText = title + " " + sres.getSecond();
    }               
    
    docData.clear();
    docData.setName(name);
    docData.setTitle(title);
    docData.setBody(bodyText);
    docData.setProps(new Properties());
    docData.setDate(date);
    
    return docData;
  }

}
