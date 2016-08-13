package source;

public class SourceFactory {
  public static String DOC_SOURCE_YAHOO_ANSWERS = "yahoo_answers";
  
  // Document sources
  public static DocumentSource createDocumentSource(String sourceName, String locationName) throws Exception {
    if (sourceName.equalsIgnoreCase(DOC_SOURCE_YAHOO_ANSWERS)) {
      return new YahooAnswersDocumentSource(locationName);
    } 
    return new ContentSourceSource(sourceName, locationName);
  }

  public static String [] getDocSourceList() {
    return new String[] { DOC_SOURCE_YAHOO_ANSWERS, 
                          ContentSourceSource.SOURCE_TYPE_WIKIPEDIA,
                          ContentSourceSource.SOURCE_TYPE_GOV2,
                          ContentSourceSource.SOURCE_TYPE_CLUEWEB };
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
