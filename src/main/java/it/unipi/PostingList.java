package it.unipi;

import it.unipi.model.PostingListInterface;

public class PostingList implements PostingListInterface {
    @Override
    public int docId() {
        return 0;
    }

    @Override
    public double score() {
        return 0;
    }

    @Override
    public void next() {

    }

    @Override
    public void nextGEQ(int docId) {

    }

    @Override
    public void addPosting(int value){

    }
}
