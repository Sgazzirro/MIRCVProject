package it.unipi.model;

public interface DocumentIndex {

    // Get the length of a specific document
    public int getLength(int docId);

    // Get the average length of the documents in the collection
    public double getAverageLength();

}
