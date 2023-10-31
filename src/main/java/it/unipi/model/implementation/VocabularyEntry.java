package it.unipi.model.implementation;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class VocabularyEntry implements Serializable {
    @JsonProperty("frequency")
    private Integer frequency;
    @JsonProperty("postingList")
    private PostingList postingList;
    @JsonProperty("upperBound")
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

    @Override
    public String toString(){
        return "Frequency: "+frequency+" UpperBound: "+upperBound+" PostingList: "+postingList.toString();
    }
}
