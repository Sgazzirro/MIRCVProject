package it.unipi.model;
import java.util.HashMap;
public class Vocabulary implements VocabularyInteface{
    private HashMap<String, VocabularyEntry> table;

    public Vocabulary(){
        table = new HashMap<String, VocabularyEntry>();
    }
    public int getDocFrequency(String term) {
        if(!table.containsKey(term))
            // Può succedere? Boh
            return -1;
        return table.get(term).getFrequency();
    }

    public PostingList getPostingList(String term) {
        if(!table.containsKey(term))
            // Può succedere? Boh
            return null;
        return table.get(term).getPostingList();
    }

    public double getUpperBound(String term) {
        if(!table.containsKey(term))
            // Può succedere? Boh
            return -1;
        return table.get(term).getUpperBound();
    }

    private class VocabularyEntry {
        private Integer frequency;
        private PostingList postingList;
        private Double upperBound;

        public Integer getFrequency() {
            return frequency;
        }

        public void setFrequency(Integer frequency) {
            this.frequency = frequency;
        }

        public PostingList getPostingList() {
            return postingList;
        }

        public void setPostingList(PostingList postingList) {
            this.postingList = postingList;
        }

        public Double getUpperBound() {
            return upperBound;
        }

        public void setUpperBound(Double upperBound) {
            this.upperBound = upperBound;
        }
    }
}
