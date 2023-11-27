package it.unipi.model;

import it.unipi.model.implementation.PostingListCompressed;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.utils.Constants;
import opennlp.tools.parser.Cons;

import java.util.List;
import java.util.Objects;

public abstract class PostingList {

    private long docIdsOffset;
    private long termFreqOffset;
    private int docIdsLength;
    private int termFreqLength;
    private double idf;

    public PostingList() {
    }

    public PostingList(long docIdsOffset, long termFreqOffset, int docIdsLength, int termFreqLength, double idf) {
        this.docIdsOffset = docIdsOffset;
        this.termFreqOffset = termFreqOffset;
        this.docIdsLength = docIdsLength;
        this.termFreqLength = termFreqLength;
        this.idf = idf;
    }

    /**
     * @return the document ID of the current posting
     */
    public abstract int docId();


    /**
     * Function that computes the score (according to the class settings) of that term in that document
     * @return the score of the current posting
     */
    public abstract double score();

    /**
     * @return whether the list has another posting or not
     */
    public abstract boolean hasNext();

    /**
     * Moves sequentially the posting list to the next posting
     */
    public abstract void next();

    /**
     * Moves the iterator toward the next posting
     * with a document ID which is greater or equal than the specified one
     * @param docId the ID of the document we would like to reach
     */
    public abstract void nextGEQ(int docId);

    /**
     * Add a posting to the current list. If the posting already exists, increment the frequency
     * of that term in that document by 1
     * @param docId the ID of the considered document
     * @return true if a new posting has been added, false if only the term frequency has been increased
     */
    public boolean addPosting(int docId) {
        return addPosting(docId, 1);
    }

    public abstract boolean addPosting(int docId, int freq);


    public boolean loadPosting() {
        return loadPosting(Constants.DOC_IDS_POSTING_FILENAME, Constants.TF_POSTING_FILENAME);
    }


    public abstract boolean loadPosting(String docIdsFilename, String termFreqFilename);

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

    public double getIdf() {
        return idf;
    }

    public void setIdf(double idf) {
        this.idf = idf;
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

    public abstract List<Integer> getTermFrequenciesDecompressedList();

    public abstract List<Integer> getDocIdsDecompressedList();
    @Override
    public abstract boolean equals(Object o);

    @Override
    public String toString() {
        return "PostingList{" +
                "docIdsOffset=" + docIdsOffset +
                ", termFreqOffset=" + termFreqOffset +
                ", docIdsLength=" + docIdsLength +
                ", termFreqLength=" + termFreqLength +
                ", nextDocId=" + docId() +
                '}';
    }
}
