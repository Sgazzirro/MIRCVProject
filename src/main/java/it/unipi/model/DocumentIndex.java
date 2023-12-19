package it.unipi.model;

import it.unipi.model.implementation.DocumentIndexImpl;

import java.util.Map;

public interface DocumentIndex {

    int getTotalLength();
    int getNumDocuments();

    // Get the length of a specific document
    int getLength(int docId);

    // Get the average length of the documents in the collection
    double getAverageLength();

    boolean addDocument(int docId, int docLength);

    void setTotalLength(int L);
    void setNumDocuments(int N);
    Iterable<? extends Map.Entry<Integer, DocumentIndexEntry>> getEntries();

    static DocumentIndex getInstance() {
        return new DocumentIndexImpl();
    }
}
