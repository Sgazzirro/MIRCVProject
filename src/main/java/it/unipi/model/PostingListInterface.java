package it.unipi.model;

public interface PostingListInterface {

    // Return the docId of the current posting
    public int docId();

    // Return the score of the current posting
    // TERM FREQUENCY
    public double score();

    // Moves sequentially the iterator to the next posting
    public void next();

    // Advances the iterator forward to the next posting
    // with a document identifier >= docId
    public void nextGEQ(int docId);

    public void addPosting(int value);
}
