package it.unipi.model;


import it.unipi.PostingList;

public class VocabularyEntry {
    private Integer frequency;

    private PostingList postingList;

    private Double upperBound;

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public Double getUpperBound() {
        return upperBound;
    }

    public PostingList getPostingList() {
        return postingList;
    }

    public void setPostingList(PostingList postingList) {
        this.postingList = postingList;
    }

    public void setUpperBound(Double upperBound) {
        this.upperBound = upperBound;
    }
}
