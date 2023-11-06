package it.unipi.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.unipi.model.PostingListInterface;
import it.unipi.utils.Constants;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PostingList implements PostingListInterface {

    private Integer offset;
    private Integer length;
    private List<Integer> docIdList;
    private List<Integer> termFrequencyList;

    private Double termIdf;
    private int pointer;

    // Used when building the index
    public PostingList() {
        this.docIdList = new ArrayList<>();
        this.termFrequencyList = new ArrayList<>();
        this.length = 0;
        this.pointer = 0;
    }

    // Used when reading the index
    public PostingList(int offset, int length) {
        this.offset = offset;
        this.length = length;
        this.pointer = 0;
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLength() {
        return length;
    }

    public List<Integer> getDocIdList() {
        return docIdList;
    }

    public List<Integer> getTermFrequencyList() {
        return termFrequencyList;
    }

    public void setTermIdf(Double termIdf) {
        this.termIdf = termIdf;
    }

    public void mergePosting(PostingList p2){
        docIdList.addAll(p2.getDocIdList());
        termFrequencyList.addAll(p2.getTermFrequencyList());
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
                int count = 0;
                while (count < this.offset) {
                    if (docIdsReader.readLine() == null || termFreqReader.readLine() == null)
                        throw new IOException("There has been an error loading the posting list: offset is too high, EOF reached");

                    count++;
                }

                count = 0;
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
        int lastIndex = docIdList.size()-1;

        if (docIdList.isEmpty() || docIdList.get(lastIndex) != docId) {
            docIdList.add(docId);
            termFrequencyList.add(termFrequency);
        } else
            termFrequencyList.set(lastIndex, termFrequencyList.get(lastIndex)+termFrequency);

        this.length++;
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
        PostingList that = (PostingList) o;
        this.loadPosting();
        that.loadPosting();
        return Objects.equals(docIdList, that.docIdList) && Objects.equals(termFrequencyList, that.termFrequencyList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, docIdList, termFrequencyList);
    }
}
