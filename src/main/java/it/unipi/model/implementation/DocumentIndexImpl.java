package it.unipi.model.implementation;

import it.unipi.io.Fetcher;
import it.unipi.model.DocumentIndex;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class DocumentIndexImpl implements DocumentIndex, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndex.class);

    private int totalLength;
    private int numDocuments;

    private final NavigableMap<Integer, DocumentIndexEntry> table;
    private final Fetcher fetcher;

    public DocumentIndexImpl() {
        table = new TreeMap<>();
        fetcher = Fetcher.getFetcher(Constants.getCompression());

        numDocuments = 0;
        totalLength = 0;
    }

    @Override
    public int getTotalLength() {
        return totalLength;
    }

    @Override
    public int getNumDocuments() {
        return numDocuments;
    }

    public void setTotalLength(int totalLength) {
        this.totalLength = totalLength;
    }

    public void setNumDocuments(int numDocuments) {
        this.numDocuments = numDocuments;
    }

    @Override
    public int getLength(int docId) {
        if (!table.containsKey(docId)) {
            String errorMessage = "Could not fetch docId " + docId + " from document index";

            try {
                fetcher.start(Constants.getPath());
                DocumentIndexEntry entry = fetcher.loadDocEntry(docId);
                fetcher.close();

                if (entry != null) {
                    table.put(docId, entry);
                    return entry.getDocumentLength();
                }
            } catch (Exception e) {
                logger.error(errorMessage, e);
            }

            throw new RuntimeException(errorMessage);
        }

        return table.get(docId).getDocumentLength();
    }

    @Override
    public double getAverageLength() {
        // Check whether the document index metadata have not been loaded in memory
        if (numDocuments == 0 && !table.isEmpty()) {
            try {
                fetcher.start();
                int[] stats = fetcher.getDocumentIndexStats();
                fetcher.close();

                numDocuments = stats[0];
                totalLength  = stats[1];

            } catch (Exception e) {
                logger.error("Could not fetch document index stats", e);
                throw new RuntimeException("Could not fetch document index stats");
            }
        }

        return (double) totalLength / numDocuments;
    }

    @Override
    public boolean addDocument(int docId, int docLength) {
        if (table.containsKey(docId)){
            return false;
        }
        else{
            DocumentIndexEntry die = new DocumentIndexEntry();
            die.setDocumentLength(docLength);
            table.put(docId, die);
            numDocuments += 1;
            totalLength += docLength;
            return true;
        }
    }

    public Iterable<Map.Entry<Integer, DocumentIndexEntry>> getEntries() {
        return table.entrySet();
    }

    @Override
    public String toString(){
        return "TotalLength: "+totalLength+" NumDocuments: "+numDocuments+" DocumentIndexTable: "+table.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentIndexImpl that = (DocumentIndexImpl) o;
        return totalLength == that.totalLength && numDocuments == that.numDocuments && Objects.equals(table.entrySet(), that.table.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalLength, numDocuments, table);
    }
}
