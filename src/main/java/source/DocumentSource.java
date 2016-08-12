package source;

public interface DocumentSource {
  /**
   * 
   * Returns the next document entry or null, if reached the end of the document source.
   * 
   */
  DocumentEntry next();
}
