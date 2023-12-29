package it.unipi.model;

import it.unipi.encoding.CompressionType;
import it.unipi.scoring.Scorer;
import it.unipi.utils.Constants;

import java.util.Objects;

public class VocabularyEntry {
    private int documentFrequency;
    private double upperBound;
    private long docIdsOffset;
    private long termFreqOffset;
    private int docIdsLength;
    private int termFreqLength;
    private PostingList postingList;
    private long touches;   // caching purposes

    public void touch() {
        touches++;
    }

    public long getTouches() {
        return touches;
    }

    public VocabularyEntry() {
        this.postingList = PostingList.getInstance(Constants.getCompression(), this);
    }

    public VocabularyEntry(CompressionType compression) {
        this.postingList = PostingList.getInstance(compression, this);
    }

    public void addPosting(int docId, int termFrequency) {
        boolean newPosting = postingList.addPosting(docId, termFrequency);

        if (newPosting)
            documentFrequency++;
    }

    public void addPosting(int docId) {
        addPosting(docId, 1);
    }

    public double idf() {
        return Math.log10( (float) Constants.N / getDocumentFrequency() );
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

    public long getDocIdsOffset() {
        return docIdsOffset;
    }

    public void setDocIdsOffset(long docIdsOffset) {
        this.docIdsOffset = docIdsOffset;
    }

    public long getTermFreqOffset() {
        return termFreqOffset;
    }

    public void setTermFreqOffset(long termFreqOffset) {
        this.termFreqOffset = termFreqOffset;
    }

    public int getDocIdsLength() {
        return docIdsLength;
    }

    public void setDocIdsLength(int docIdsLength) {
        this.docIdsLength = docIdsLength;
    }

    public int getTermFreqLength() {
        return termFreqLength;
    }

    public void setTermFreqLength(int termFreqLength) {
        this.termFreqLength = termFreqLength;
    }

    public PostingList getPostingList() {
        return postingList;
    }

    public void setPostingList(PostingList postingList) {
        this.postingList = postingList;
    }

    public void setTouches(long touches) {
        this.touches = touches;
    }

    public void computeUpperBound(Scorer scorer) {
        upperBound = scorer.computeUpperBound(postingList);
    }

    @Override
    public String toString() {
        return "VocabularyEntry{" +
                "documentFrequency=" + documentFrequency +
                ", upperBound=" + upperBound +
                ", docIdsOffset=" + docIdsOffset +
                ", termFreqOffset=" + termFreqOffset +
                ", docIdsLength=" + docIdsLength +
                ", termFreqLength=" + termFreqLength +
                ", postingList=" + postingList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VocabularyEntry entry = (VocabularyEntry) o;
        return documentFrequency == entry.documentFrequency && Objects.equals(postingList, entry.postingList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentFrequency, postingList);
    }
}
