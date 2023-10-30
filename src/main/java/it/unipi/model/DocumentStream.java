package it.unipi.model;


import java.util.Map;

public interface DocumentStream {
    /*
        Represent a stream of documents
     */

    // Returns next (docid, content)
    public String[] nextDoc();

    // Returns if the stream as another doc
    public boolean hasNext();


}
