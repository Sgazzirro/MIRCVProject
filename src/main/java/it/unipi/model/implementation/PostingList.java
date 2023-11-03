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
import java.util.ArrayList;
import java.util.Objects;
import java.util.StringJoiner;

public class PostingList implements PostingListInterface {

    Integer offset;
    Integer length;
    ArrayList<Integer> docIdList;
    ArrayList<Integer> termFrequencyList;

    // Used when building the index
    public PostingList() {
        this.docIdList = new ArrayList<>();
        this.termFrequencyList = new ArrayList<>();
    }

    // Used when reading the index
    public PostingList(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    private boolean loadPosting() {
        // Method that loads the posting list in memory if not present
        if (docIdList == null) {
            docIdList = new ArrayList<>();
            termFrequencyList = new ArrayList<>();

            try (
                    BufferedReader docIdsReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(Constants.DOC_IDS_POSTING_FILE)), StandardCharsets.UTF_8));
                    BufferedReader termFreqReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(Constants.TF_POSTING_FILE)), StandardCharsets.UTF_8));
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
        if (!loadPosting()) {
            // Do something
        }
        return 0;
    }

    @Override
    public double score() {
        if (!loadPosting()) {
            // Do something
        }
        return 0;
    }

    @Override
    public void next() {
        if (!loadPosting()) {
            // Do something
        }
    }

    @Override
    public void nextGEQ(int docId) {
        if (!loadPosting()) {
            // Do something
        }
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
