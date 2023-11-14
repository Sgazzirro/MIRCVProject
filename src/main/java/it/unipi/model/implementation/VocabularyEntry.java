package it.unipi.model.implementation;


import java.util.List;
import java.util.Objects;

public class VocabularyEntry {

    private Integer documentFrequency;
    private Double upperBound;
    private PostingListImpl postingListImpl;

    public Integer getDocumentFrequency() {
        return documentFrequency;
    }

    public VocabularyEntry() {
    }

    public VocabularyEntry(Integer documentFrequency, Double upperBound, PostingListImpl postingListImpl) {
        this.documentFrequency = documentFrequency;
        this.upperBound = upperBound;
        this.postingListImpl = postingListImpl;
    }

    public VocabularyEntry(String[] lineParam){
        this(Integer.parseInt(lineParam[1]),
                Double.parseDouble(lineParam[2]),
                new PostingListImpl(Double.parseDouble(lineParam[3]),
                        Integer.parseInt(lineParam[4]),
                        Integer.parseInt(lineParam[5]),
                        Integer.parseInt(lineParam[6])));
    }

    public void addPosting(int docId) {
        // If the docId is already been happened don't increase the document frequency
        List<Integer> docIdList = postingListImpl.getDocIdList();

        if (docIdList.isEmpty() || docIdList.get(docIdList.size() - 1) < docId)
            documentFrequency++;

        postingListImpl.addPosting(docId);
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

    public PostingListImpl getPostingList() {
        return postingListImpl;
    }

    public void setPostingList(PostingListImpl postingListImpl) {
        this.postingListImpl = postingListImpl;
    }

    @Override
    public String toString() {
        return "VocabularyEntry{" +
                "documentFrequency=" + documentFrequency +
                ", upperBound=" + upperBound +
                ", postingList=" + postingListImpl +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VocabularyEntry that = (VocabularyEntry) o;
        return Objects.equals(documentFrequency, that.documentFrequency) && Objects.equals(upperBound, that.upperBound) && Objects.equals(postingListImpl, that.postingListImpl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentFrequency, upperBound, postingListImpl);
    }

}
