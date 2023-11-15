package it.unipi.model;


import it.unipi.model.implementation.Document;

public interface DocumentStream {
    /*
        Represent a stream of documents
     */

    // Returns next (docid, content)
    public Document nextDoc();
}