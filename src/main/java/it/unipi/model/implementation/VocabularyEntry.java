package it.unipi.model.implementation;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class VocabularyEntry {

    private Integer frequency;
    private PostingList postingList;
    private Double upperBound;

    public VocabularyEntry(){}

    public VocabularyEntry(Integer freq, Double upperBound, PostingList postingList) {
        frequency = freq;
        this.upperBound = upperBound;
        this.postingList = postingList;
    }

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

    @Override
    public String toString(){
        return "Frequency: "+frequency+" UpperBound: "+upperBound+" PostingList: "+postingList.toString();
    }
}
