package it.unipi.model;

public interface Vocabulary {

    public int getDocFrequency(String term);

    public PostingList getPostingList(String term);

    public double getUpperBound();
}
