package it.unipi.model;

import it.unipi.model.implementation.PostingListCompressed;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.utils.CompressionType;
import it.unipi.utils.Fetcher;

import java.io.EOFException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class PostingList{

    private long docIdsOffset;
    private long termFreqOffset;
    private int docIdsLength;
    private int termFreqLength;
    private double idf;


    // A default constructor used when building the list
    public PostingList() {
    }

    // A constructor used when we fetch information from vocabulary entry
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

    public abstract int termFrequency();
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
    public abstract void next() throws EOFException;

    /**
     * Moves the iterator toward the next posting
     * with a document ID which is greater or equal than the specified one
     * @param docId the ID of the document we would like to reach
     */
    public abstract void nextGEQ(int docId) throws EOFException;

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


    public abstract boolean loadPosting();

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        PostingList that = (PostingList) o;
        try {
            while (hasNext()) {
                next();
                that.next();
                if (docId() != that.docId() || termFrequency() != that.termFrequency()) {
                    System.out.println("THIS DOCID : " + docId() + " AND THAT DOCID : "+ that.docId());
                    System.out.println("THIS FREQUENCY : " + termFrequency() + " AND THAT FREQUENCY : "+ that.termFrequency());
                    return false;
                }
            }
        } catch (EOFException eofException){
            eofException.printStackTrace();
        }
        return true;
    };

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

    public static PostingList getInstance(CompressionType compression) {
        switch (compression) {
            case DEBUG:
            case BINARY: return new PostingListImpl();

            case COMPRESSED: return new PostingListCompressed();
        }
        throw new RuntimeException("Unsupported compression type: " + compression);
    }
}
