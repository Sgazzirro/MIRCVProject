package it.unipi.io;


import it.unipi.io.implementation.DocumentStreamImpl;
import it.unipi.model.Document;
import it.unipi.utils.Constants;

public interface DocumentStream {
    /*
        Represent a stream of documents
     */

    // Returns next (docid, content)
    Document nextDoc();

    Document getDoc(int docId);

    static DocumentStream getInstance() {
        return new DocumentStreamImpl(Constants.COLLECTION_FILE);
    }
}
