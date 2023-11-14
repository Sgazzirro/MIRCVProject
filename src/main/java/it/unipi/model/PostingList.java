package it.unipi.model;

import it.unipi.model.implementation.PostingListImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface PostingList {

    /**
     * @return the document ID of the current posting
     */
    public int docId();


    /**
     * Function that computes the score (according to the class settings) of that term in that document
     * @return the score of the current posting
     */
    public double score();

    /**
     * @return whether the list has another posting or not
     */
    public boolean hasNext();

    /**
     * Moves sequentially the posting list to the next posting
     */
    public void next();

    /**
     * Moves the iterator toward the next posting
     * with a document ID which is greater or equal than the specified one
     * @param docId the ID of the document we would like to reach
     */
    public void nextGEQ(int docId);

    /**
     * Add a posting to the current list. If the posting already exists, increment the frequency
     * of that term in that document by 1
     * @param docId the ID of the considered document
     */
    public void addPosting(int docId);

    /**
     * Concatenate the passed list to the current one, assuming that no sorting is required
     * @param toMerge the posting list we have to merge to the current
     * @return the length of the new list
     */
    public int mergePosting(PostingListImpl toMerge);


    public List<Integer> getDocIdList();

    public List<Integer> getTermFrequencyList();
}
