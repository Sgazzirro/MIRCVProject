package it.unipi.model;

// DEPRECATED

public interface InvertedIndex {

    /*
        Logical Structure of an InvertedIndex
     */

    // Returns the posting list refered by a specific integer
    //  i -> index for in-memory indexing
    //    -> file offset otherwise
    public PostingList get(int location);

    public void addPosting(int location, String docId);

}
