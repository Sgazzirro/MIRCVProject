package it.unipi.model.implementation;

import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;

import java.util.*;


/**
 * Posting list object that implements {@link PostingList}.
 * Demanded to the load/write of postings from/into the secondary memory.
 */
public class PostingListImpl extends PostingList {

    private List<Integer> docIdsList;
    private List<Integer> termFrequenciesList;
    private int pointer = -1;

    // Used when building the index
    public PostingListImpl(VocabularyEntry entry) {
        super(entry);

        this.docIdsList = new ArrayList<>();
        this.termFrequenciesList = new ArrayList<>();
    }

    /**
     * Concatenate the passed list to the current one, assuming that no sorting is required
     *
     * @param toMerge the posting list we have to merge to the current
     */
    public void mergePosting(PostingList toMerge) {
        docIdsList.addAll(toMerge.getDocIdsList());
        termFrequenciesList.addAll(toMerge.getTermFrequenciesList());
    }

    public List<Integer> getTermFrequenciesList() {
        return termFrequenciesList;
    }

    public List<Integer> getDocIdsList() {
        return docIdsList;
    }

    @Override
    public int docId() {
        return docIdsList.get(pointer);
    }

    @Override
    public int termFrequency() {
        return termFrequenciesList.get(pointer);
    }

    @Override
    public void next() {
        if (!hasNext())
            throw new NoSuchElementException();

        pointer++;
    }

    public boolean hasNext() {
        return pointer < docIdsList.size() - 1;
    }

    @Override
    public void nextGEQ(int docId) {
        if (pointer == -1 && hasNext())
            next();

        while (this.docId() < docId)
            next();
    }

    @Override
    public void reset() {
        pointer = -1;
    }

    @Override
    public boolean addPosting(int docId, int termFrequency) {
        // Documents are supposed to be read sequentially with respect to docId
        if (docIdsList == null)
            docIdsList = new ArrayList<>();
        if (termFrequenciesList == null)
            termFrequenciesList = new ArrayList<>();

        int lastIndex = docIdsList.size()-1;

        if (docIdsList.isEmpty() || docIdsList.get(lastIndex) != docId) {
            docIdsList.add(docId);
            termFrequenciesList.add(termFrequency);
            return true;
        } else {
            termFrequenciesList.set(lastIndex, termFrequenciesList.get(lastIndex) + termFrequency);
            return false;
        }
    }

    @Override
    public String toString() {
        return "DocIdList: " + docIdsList + " TermFrequencyList: " + termFrequenciesList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(docIdsList, termFrequenciesList);
    }
}
