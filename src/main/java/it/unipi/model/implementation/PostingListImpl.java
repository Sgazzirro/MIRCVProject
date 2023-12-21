package it.unipi.model.implementation;

import it.unipi.model.Posting;
import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;

import java.io.*;
import java.util.*;


/**
 * Posting list object that implements {@link PostingList}.
 * Demanded to the load/write of postings from/into the secondary memory.
 */
public class PostingListImpl extends PostingList {

    private List<Integer> docIdsDecompressedList;
    private List<Integer> termFrequenciesDecompressedList;
    private int pointer = -1;

    // Used when building the index
    public PostingListImpl(VocabularyEntry entry) {
        super(entry);

        this.docIdsDecompressedList = new ArrayList<>();
        this.termFrequenciesDecompressedList = new ArrayList<>();
    }

    /**
     * Concatenate the passed list to the current one, assuming that no sorting is required
     * @param toMerge the posting list we have to merge to the current
     * @return the length of the new list
     */
    public int mergePosting(PostingList toMerge) {
        //if (!(toMerge instanceof PostingListImpl))
        //    throw new RuntimeException("Cannot merge PostingLists with different implementations");

        docIdsDecompressedList.addAll(toMerge.getDocIdsDecompressedList());
        termFrequenciesDecompressedList.addAll(toMerge.getTermFrequenciesDecompressedList());
        return docIdsDecompressedList.size();
    }

    public List<Integer> getTermFrequenciesDecompressedList() {
        return termFrequenciesDecompressedList;
    }

    public List<Integer> getDocIdsDecompressedList() {
        return docIdsDecompressedList;
    }
/*
    @Override
    public boolean equals(Object o) {
        System.out.println("DENTRO LA EQUALS DELLA IMPL");
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostingListImpl that = (PostingListImpl) o;
        try {
            while (hasNext()) {
                next();
                that.next();
                if (docId() != that.docId() || termFrequency() != that.termFrequency())
                    return false;
            }
        } catch (EOFException eofException){
            eofException.printStackTrace();
        }
        return true;
    }
*/
    @Override
    public int docId() {
        return docIdsDecompressedList.get(pointer);
    }

    @Override
    public int termFrequency() {
        return termFrequenciesDecompressedList.get(pointer);
    }

    @Override
    public Posting next() {
        if (!hasNext())
            throw new NoSuchElementException();

        pointer++;
        return new Posting(termFrequency(), docId());
    }

    public boolean hasNext() {
        return pointer < docIdsDecompressedList.size() - 1;
    }

    @Override
    public void nextGEQ(int docId) throws EOFException{
        do {
            next();
        } while (this.docId() < docId);

    }

    @Override
    public void reset() {
        pointer = -1;
    }

    @Override
    public boolean addPosting(int docId, int termFrequency) {
        // Documents are supposed to be read sequentially with respect to docId
        if (docIdsDecompressedList == null)
            docIdsDecompressedList = new ArrayList<>();
        if (termFrequenciesDecompressedList == null)
            termFrequenciesDecompressedList = new ArrayList<>();

        int lastIndex = docIdsDecompressedList.size()-1;

        if (docIdsDecompressedList.isEmpty() || docIdsDecompressedList.get(lastIndex) != docId) {
            docIdsDecompressedList.add(docId);
            termFrequenciesDecompressedList.add(termFrequency);
            return true;
        } else {
            termFrequenciesDecompressedList.set(lastIndex, termFrequenciesDecompressedList.get(lastIndex) + termFrequency);
            return false;
        }
    }

    @Override
    public String toString() {
        return "DocIdList: " + docIdsDecompressedList + " TermFrequencyList: " + termFrequenciesDecompressedList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(docIdsDecompressedList, termFrequenciesDecompressedList);
    }
}
