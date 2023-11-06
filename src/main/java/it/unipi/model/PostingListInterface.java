package it.unipi.model;

import it.unipi.model.implementation.PostingList;
import it.unipi.utils.WritingInterface;

import java.io.IOException;

public interface PostingListInterface {

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
    public int mergePosting(PostingList toMerge);

    /**
     * Function demanded to writing posting lists onto the two dedicated file "doc_ids.txt" and
     * "term_frequencies.txt". The function takes two Closeable object (Writers) as argument.
     * Checks for compression using {@link it.unipi.utils.Constants#getCompression() getCompression}
     * @param writerIDS the object demanded to write onto the document id file
     * @param writerTF the object demanded to write onto the term frequencies file
     * @return a long[2] containing the byte offset at which lists have been written
     */
    public long[] dumpPostings(WritingInterface writerIDS, WritingInterface writerTF) throws IOException;


}
