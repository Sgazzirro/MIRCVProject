package it.unipi.model.implementation;


import it.unipi.model.PostingList;
import it.unipi.utils.Constants;

import java.util.Objects;

public class VocabularyEntry {

    private Integer documentFrequency;
    private Double upperBound;
    private PostingList postingList;

    public VocabularyEntry() {
    }

    public VocabularyEntry(Integer documentFrequency, Double upperBound, PostingList postingList) {
        this.documentFrequency = documentFrequency;
        this.upperBound = upperBound;
        this.postingList = postingList;
    }

    public void addPosting(int docId) {
        boolean newPosting = postingList.addPosting(docId);

        if (newPosting)
            documentFrequency++;
    }

    public Integer getDocumentFrequency() {
        return documentFrequency;
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

    public static VocabularyEntry parseTXT(String line) {
        String[] params = line.split(",");

        int documentFrequency = Integer.parseInt(params[1]);
        double upperBound = Double.parseDouble(params[2]);
        PostingList postingList;
        if(!Constants.getCompression())
            postingList = new PostingListImpl();
        else
            postingList = new PostingListCompressed();
        postingList.setIdf(Double.parseDouble(params[3]));
        postingList.setDocIdsOffset(Long.parseLong(params[4]));
        postingList.setTermFreqOffset(Long.parseLong(params[5]));
        int length = Integer.parseInt(params[6]);
        postingList.setDocIdsLength(length);
        postingList.setTermFreqLength(length);

        return new VocabularyEntry(documentFrequency, upperBound, postingList);
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
        return Objects.equals(documentFrequency, that.documentFrequency) && Objects.equals(upperBound, that.upperBound) &&
                Objects.equals(postingList.getDocIdsDecompressedList(), that.postingList.getDocIdsDecompressedList()) &&
                Objects.equals(postingList.getTermFrequenciesDecompressedList().subList(0, postingList.getDocIdsDecompressedList().size()),
                        that.postingList.getTermFrequenciesDecompressedList().subList(0, that.postingList.getDocIdsDecompressedList().size()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentFrequency, upperBound, postingList);
    }

}
