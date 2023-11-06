package it.unipi.model.implementation;


import java.util.List;
import java.util.Objects;

public class VocabularyEntry {

    private Integer documentFrequency;
    private Double upperBound;
    private PostingList postingList;

    public Integer getDocumentFrequency() {
        return documentFrequency;
    }

    public VocabularyEntry() {
    }

    public VocabularyEntry(Integer documentFrequency, Double upperBound, PostingList postingList) {
        this.documentFrequency = documentFrequency;
        this.upperBound = upperBound;
        this.postingList = postingList;
    }

    public VocabularyEntry(String[] lineParam){
        this(Integer.parseInt(lineParam[1]),
                Double.parseDouble(lineParam[2]),
                new PostingList(Double.parseDouble(lineParam[3]),
                        Integer.parseInt(lineParam[4]),
                        Integer.parseInt(lineParam[5]),
                        Integer.parseInt(lineParam[6])));
    }

    public void addPosting(int docId) {
        // If the docId is already been happened don't increase the document frequency
        List<Integer> docIdList = postingList.getDocIdList();

        if (docIdList.isEmpty() || docIdList.get(docIdList.size() - 1) < docId)
            documentFrequency++;

        postingList.addPosting(docId);
    }
    public void setDocumentFrequency(Integer documentFrequency) {
        this.documentFrequency = documentFrequency;
    }

    public Double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Double upperBound) {
        this.upperBound = upperBound;
    }

    public PostingList getPostingList() {
        return postingList;
    }

    public void setPostingList(PostingList postingList) {
        this.postingList = postingList;
    }

    @Override
    public String toString() {
        return "VocabularyEntry{" +
                "documentFrequency=" + documentFrequency +
                ", upperBound=" + upperBound +
                ", postingList=" + postingList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VocabularyEntry that = (VocabularyEntry) o;
        return Objects.equals(documentFrequency, that.documentFrequency) && Objects.equals(upperBound, that.upperBound) && Objects.equals(postingList, that.postingList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentFrequency, upperBound, postingList);
    }

}
