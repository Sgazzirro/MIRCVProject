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
public class PostingListImpl implements PostingList {
    private Double termIdf;
    private Long offsetID;
    private Long offsetTF;
    private Integer length;
    private List<Integer> docIdList;
    private List<Integer> termFrequencyList;


    private int pointer;

    // Used when building the index
    public PostingListImpl() {
        this.docIdList = new ArrayList<>();
        this.termFrequencyList = new ArrayList<>();
        this.length = 0;
        this.termIdf = 0.0;
        this.pointer = 0;
    }

    // Used when reading the index
    public PostingListImpl(Double IDF, Long offsetID, Long offsetTF, Integer len) {
        this.offsetID = offsetID;
        this.offsetTF = offsetTF;
        this.termIdf = IDF;
        this.length = len;
        this.pointer = 0;
    }

    public Double getTermIdf() {
        return termIdf;
    }

    public int getLength() {
        return length;
    }

    public List<Integer> getDocIdList() {
        return docIdList;
    }

    public List<Integer> getTermFrequencyList() {
        return termFrequencyList;
    }

    @Override
    public long getOffsetID() {
        return offsetID;
    }

    @Override
    public long getOffsetTF() {
        return offsetTF;
    }

    public void setTermIdf(Double termIdf) {
        this.termIdf = termIdf;
    }

    public int mergePosting(PostingList p2){
        docIdList.addAll(p2.getDocIdList());
        termFrequencyList.addAll(p2.getTermFrequencyList());
        return docIdList.size();
    }


    public boolean loadPosting() {
        return loadPosting(-1);
    }

    public boolean loadPosting(int blockNumber) {
        // Method that loads the posting list in memory if not present
        if (docIdList == null) {
            docIdList = new ArrayList<>();
            termFrequencyList = new ArrayList<>();

            String idsFilename;
            String freqFilename;
            if(blockNumber < 0){
                idsFilename = Constants.DOC_IDS_POSTING_FILE;
                freqFilename = Constants.TF_POSTING_FILE;
            }
            else{
                idsFilename = "./data/blocks/doc_ids" + blockNumber + ".txt";
                freqFilename = "./data/blocks/term_frequencies" + blockNumber + ".txt";
            }
            try (
                    BufferedReader docIdsReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(idsFilename)), StandardCharsets.UTF_8));
                    BufferedReader termFreqReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(freqFilename)), StandardCharsets.UTF_8));
            ) {
                // Skip lines to reach the start of the posting list
                docIdsReader.skip(offsetID);
                termFreqReader.skip(offsetTF);
                int count = 0;

                String docIdsLine, termFreqLine;
                while (count < this.length) {
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
        return (1 + Math.log10(tf)) * termIdf;
    }

    @Override
    public void next() {
        loadPosting();

        if (!hasNext())
            throw new NoSuchElementException();

        pointer++;
    }

    public void reset() {
        pointer = 0;
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
    public void addPosting(int docId) {
        addPosting(docId, 1);
    }

    public void addPosting(int docId, int termFrequency) {
        // Documents are supposed to be read sequentially with respect to docId
        if(docIdList == null)
            docIdList = new ArrayList<>();
        if(termFrequencyList == null)
            termFrequencyList = new ArrayList<>();

        int lastIndex = docIdList.size()-1;

        if (docIdList.isEmpty() || docIdList.get(lastIndex) != docId) {
            docIdList.add(docId);
            termFrequencyList.add(termFrequency);
        } else
            termFrequencyList.set(lastIndex, termFrequencyList.get(lastIndex)+termFrequency);

    }

    public int dumpPostings(StringJoiner docIds, StringJoiner termFrequencies) {
        for (int docId : docIdList)
            docIds.add(Integer.toString(docId));
        for (int tf : termFrequencyList)
            termFrequencies.add(Integer.toString(tf));

        return docIdList.size();
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
        this.loadPosting();
        that.loadPosting();
        return Objects.equals(docIdList, that.docIdList) && Objects.equals(termFrequencyList, that.termFrequencyList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offsetID, docIdList, termFrequencyList);
    }
}
