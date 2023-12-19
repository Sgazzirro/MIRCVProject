package it.unipi.model;

import it.unipi.model.implementation.PostingListCompressedImpl;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.encoding.CompressionType;
import it.unipi.scoring.Scorer;

import java.io.EOFException;
import java.util.List;

public abstract class PostingList {

    protected VocabularyEntry rootEntry;

    public PostingList(VocabularyEntry entry) {
        this.rootEntry = entry;
    }

    public VocabularyEntry vocabularyEntry() {
        return rootEntry;
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
    public double score() {
        return Scorer.score(this);
    }

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

    /**
     * Reset the iterator on the posting list to the beginning
     */
    public abstract void reset();

    public abstract boolean addPosting(int docId, int freq);

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
                "nextDocId=" + docId() +
                '}';
    }

    public static PostingList getInstance(CompressionType compression, VocabularyEntry entry) {
        PostingList postingList = switch (compression) {
            case DEBUG, BINARY -> new PostingListImpl(entry);
            case COMPRESSED -> new PostingListCompressedImpl(entry);
        };

        entry.setPostingList(postingList);
        return postingList;
    }
}
