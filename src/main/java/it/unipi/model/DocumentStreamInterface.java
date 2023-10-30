package it.unipi.model;


import it.unipi.Document;

public interface DocumentStreamInterface {
    /*
        Represent a stream of documents
     */

    // Returns next (docid, content)
    public Document nextDoc();
}
