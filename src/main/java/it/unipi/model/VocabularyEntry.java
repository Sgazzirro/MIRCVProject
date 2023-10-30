package it.unipi.model;


public class VocabularyEntry {
    private Integer frequency;
    private int locationPostingList;
    private Double upperBound;

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public int getLocationPostingList() {
        return locationPostingList;
    }

    public void setLocationPostingList(int location) {
        this.locationPostingList = location;
    }

    public Double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Double upperBound) {
        this.upperBound = upperBound;
    }
}
