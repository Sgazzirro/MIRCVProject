package it.unipi.model;

public interface VocabularyInteface {

    public int getDocFrequency(String term);

    public PostingList getPostingList(String term);

    public double getUpperBound(String term);
}
