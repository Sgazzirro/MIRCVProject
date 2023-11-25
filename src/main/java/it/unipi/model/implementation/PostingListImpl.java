package it.unipi.model.implementation;

import it.unipi.model.PostingList;
import it.unipi.utils.Constants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


/**
 * Posting list object that implements {@link PostingList}.
 * Demanded to the load/write of postings from/into the secondary memory.
 */
public class PostingListImpl extends PostingList {

    private List<Integer> docIdList;
    private List<Integer> termFrequencyList;
    private int pointer;

    // Used when building the index
    public PostingListImpl() {
        super();
        this.docIdList = new ArrayList<>();
        this.termFrequencyList = new ArrayList<>();
    }

    // Used when reading the index
    public PostingListImpl(long docIdsOffset, long termFreqOffset, int docIdsLength, int termFreqLength, double idf) {
        super(docIdsOffset, termFreqOffset, docIdsLength, termFreqLength, idf);
        this.docIdList = new ArrayList<>();
        this.termFrequencyList = new ArrayList<>();
    }


    /**
     * Concatenate the passed list to the current one, assuming that no sorting is required
     * @param toMerge the posting list we have to merge to the current
     * @return the length of the new list
     */
    public int mergePosting(PostingList toMerge) {
        //if (!(toMerge instanceof PostingListImpl))
        //    throw new RuntimeException("Cannot merge PostingLists with different implementations");

        PostingListImpl other = (PostingListImpl) toMerge;
        docIdList.addAll(other.getDocIds());
        termFrequencyList.addAll(other.getTermFrequencies());
        return docIdList.size();
    }

    @Override
    public boolean loadPosting(String docIdsFilename, String termFreqFilename) {
        // Method that loads the posting list in memory if not present
        if (docIdList == null) {
            docIdList = new ArrayList<>();
            termFrequencyList = new ArrayList<>();

            try (
                    BufferedReader docIdsReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(docIdsFilename)), StandardCharsets.UTF_8));
                    BufferedReader termFreqReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(termFreqFilename)), StandardCharsets.UTF_8))
            ) {
                // Skip lines to reach the start of the posting list
                long docIdsOffset = getDocIdsOffset();
                long termFreqOffset = getTermFreqOffset();
                int length = getDocIdsLength();     // It should be the same as termFrequenciesLength
                docIdsReader.skip(docIdsOffset);
                termFreqReader.skip(termFreqOffset);
                int count = 0;

                String docIdsLine, termFreqLine;
                while (count < length) {
                    docIdsLine = docIdsReader.readLine();
                    termFreqLine = termFreqReader.readLine();
                    if (docIdsLine == null || termFreqLine == null)
                        throw new IOException("There has been an error loading the posting list: length is too high, EOF reached");

                    // Add posting
                    addPosting(Integer.parseInt(docIdsLine), Integer.parseInt(termFreqLine));
                    count++;
                }

            } catch (IOException e) {
                docIdList = null;
                termFrequencyList = null;

                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public int docId() {
        loadPosting();

        return docIdList.get(pointer);
    }

    @Override
    public double score() {
        loadPosting();

        int tf = termFrequencyList.get(pointer);
        return (1 + Math.log10(tf)) * getIdf();
    }

    @Override
    public void next() {
        loadPosting();

        if (!hasNext())
            throw new NoSuchElementException();

        pointer++;
    }

    public boolean hasNext() {
        return pointer < docIdList.size() - 1;
    }

    @Override
    public void nextGEQ(int docId) {
        loadPosting();

        while (this.docId() < docId)
            next();
    }

    @Override
    public boolean addPosting(int docId, int termFrequency) {
        // Documents are supposed to be read sequentially with respect to docId
        if (docIdList == null)
            docIdList = new ArrayList<>();
        if (termFrequencyList == null)
            termFrequencyList = new ArrayList<>();

        int lastIndex = docIdList.size()-1;

        if (docIdList.isEmpty() || docIdList.get(lastIndex) != docId) {
            docIdList.add(docId);
            termFrequencyList.add(termFrequency);
            return true;
        } else {
            termFrequencyList.set(lastIndex, termFrequencyList.get(lastIndex) + termFrequency);
            return false;
        }
    }

    public List<Integer> getDocIds() {
        return docIdList;
    }

    public List<Integer> getTermFrequencies() {
        return termFrequencyList;
    }

    @Override
    public String toString() {
        return "DocIdList: " + docIdList + " TermFrequencyList: " + termFrequencyList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostingListImpl that = (PostingListImpl) o;
       // this.loadPosting();
       // that.loadPosting();
        return Objects.equals(docIdList, that.docIdList) && Objects.equals(termFrequencyList, that.termFrequencyList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docIdList, termFrequencyList);
    }
}
