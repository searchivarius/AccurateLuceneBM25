package source;

public class SourceFactory {
  public static String DOC_SOURCE_YAHOO_ANSWERS = "yahoo_answers";
  
  // Document sources
  public static DocumentSource createDocumentSource(String sourceName, String fileName) throws Exception {
    if (sourceName.equalsIgnoreCase(DOC_SOURCE_YAHOO_ANSWERS)) {
      return new YahooAnswersDocumentSource(fileName);
    }
    throw new Exception("Uncrecognized document source: " + sourceName);
  }

  public static String [] getDocSourceList() {
    return new String[] { DOC_SOURCE_YAHOO_ANSWERS };
  }
  // Query sources
  public static String QUERY_SOURCE_YAHOO_ANSWERS = "yahoo_answers";
  
  public static QuerySource createQuerySource(String sourceName, String fileName) throws Exception {
    if (sourceName.equalsIgnoreCase(DOC_SOURCE_YAHOO_ANSWERS)) {
      return new YahooAnswersQuerySource(fileName);
    }
    throw new Exception("Uncrecognized query source: " + sourceName);
  }
  
  public static String [] getQuerySourceList() {
    return new String[] { QUERY_SOURCE_YAHOO_ANSWERS };
  }
}
