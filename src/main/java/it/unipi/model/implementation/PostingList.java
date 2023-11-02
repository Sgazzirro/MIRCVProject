package it.unipi.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.unipi.model.PostingListInterface;

import java.util.ArrayList;
import java.util.StringJoiner;

public class PostingList implements PostingListInterface {

    Integer offset;
    ArrayList<Integer> docIdList;
    ArrayList<Integer> termFrequencyList;

    // Used when building the index
    public PostingList() {
        this.docIdList = new ArrayList<>();
        this.termFrequencyList = new ArrayList<>();
    }

    // Used when reading the index
    public PostingList(int offset) {
        this.offset = offset;
    }

    private void loadPosting() {
        // Method that loads the posting list in memory if not present
        if (docIdList == null) {
            // load posting list
        }
    }

    @Override
    public int docId() {
        loadPosting();
        return 0;
    }

    @Override
    public double score() {
        loadPosting();
        return 0;
    }

    @Override
    public void next() {
        loadPosting();
    }

    @Override
    public void nextGEQ(int docId) {
        loadPosting();
    }

    @Override
    public void addPosting(int docId) {
        // Documents are supposed to be read sequentially with respect to docId
        int lastIndex = docIdList.size()-1;

        if (docIdList.isEmpty() || docIdList.get(lastIndex) != docId) {
            docIdList.add(docId);
            termFrequencyList.add(1);
        } else
            termFrequencyList.set(lastIndex, termFrequencyList.get(lastIndex)+1);
    }

    @Override
    public String toString() {
        return "DocIdList: " + docIdList.toString() + " TermFrequencyList: " + termFrequencyList.toString();
    }

    public int dumpPostings(StringJoiner docIds, StringJoiner termFrequencies) {
        for (int docId : docIdList)
            docIds.add(Integer.toString(docId));
        for (int tf : termFrequencyList)
            termFrequencies.add(Integer.toString(tf));

        return docIdList.size();
    }
}
