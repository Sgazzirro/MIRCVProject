package it.unipi.model;


public interface DocumentStreamInterface {
    /*
        Represent a stream of documents
     */

    // Returns next (docid, content)
    public String[] nextDoc();

    // Returns if the stream as another doc
    public boolean hasNext();


}
