package it.unipi.model;

import it.unipi.model.implementation.Document;

public interface DocumentIndexInterface {

    // Get the length of a specific document
    public int getLength(int docId);

    // Get the average length of the documents in the collection
    public double getAverageLength();

    public boolean addDocument(int docId, int docLength);

}