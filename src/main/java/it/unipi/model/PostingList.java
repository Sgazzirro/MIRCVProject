package it.unipi.model;

import it.unipi.model.implementation.PostingListCompressedImpl;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.encoding.CompressionType;

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
     *
     * @param docId the ID of the considered document
     */
    public void addPosting(int docId) {
        addPosting(docId, 1);
    }

    /**
     * Reset the iterator on the posting list to the beginning
     */
    public abstract void reset();

    public abstract boolean addPosting(int docId, int freq);

    public abstract List<Integer> getTermFrequenciesList();

    public abstract List<Integer> getDocIdsList();

    public static PostingList getInstance(CompressionType compression, VocabularyEntry entry) {
        PostingList postingList = switch (compression) {
            case DEBUG, BINARY -> new PostingListImpl(entry);
            case COMPRESSED -> new PostingListCompressedImpl(entry);
        };

        entry.setPostingList(postingList);
        return postingList;
    }
}
