package it.unipi.model;

import it.unipi.model.implementation.DocumentIndexEntry;

import java.util.Map;

public interface DocumentIndex {

    public int getTotalLength();
    public int getNumDocuments();

    // Get the length of a specific document
    public int getLength(int docId);

    // Get the average length of the documents in the collection
    public double getAverageLength();

    public boolean addDocument(int docId, int docLength);

    public void setTotalLength(int L);
    public void setNumDocuments(int N);
    Iterable<? extends Map.Entry<Integer, DocumentIndexEntry>> getEntries();
}
