package it.unipi.model.implementation;


import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.PostingListImpl;

import java.util.Objects;

public class VocabularyEntryImpl implements VocabularyEntry {

    private int documentFrequency;
    private double upperBound;
    private PostingList postingList;

    public VocabularyEntryImpl() {
    }

    public VocabularyEntryImpl(int documentFrequency, double upperBound, PostingList postingList) {
        this.documentFrequency = documentFrequency;
        this.upperBound = upperBound;
        this.postingList = postingList;
    }

    public void addPosting(int docId) {
        boolean newPosting = postingList.addPosting(docId);

        if (newPosting)
            documentFrequency++;
    }

    public int getDocumentFrequency() {
        return documentFrequency;
    }

    public void setDocumentFrequency(int documentFrequency) {
        this.documentFrequency = documentFrequency;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
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
        return Objects.equals(documentFrequency, that.getDocumentFrequency()) &&
                //Objects.equals(upperBound, that.upperBound) &&
                Objects.equals(postingList, that.getPostingList());
                //Objects.equals(postingList.getDocIdsDecompressedList(), that.postingList.getDocIdsDecompressedList()) &&
                //Objects.equals(postingList.getTermFrequenciesDecompressedList().subList(0, postingList.getDocIdsDecompressedList().size()),
                //        that.postingList.getTermFrequenciesDecompressedList().subList(0, that.postingList.getDocIdsDecompressedList().size()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentFrequency, upperBound, postingList);
    }

}
