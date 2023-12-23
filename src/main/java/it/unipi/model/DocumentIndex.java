package it.unipi.model;

import it.unipi.io.Fetcher;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class DocumentIndex {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndex.class);

    private int totalLength;
    private int numDocuments;

    private final NavigableMap<Integer, DocumentIndexEntry> table;
    private final Fetcher fetcher;

    public DocumentIndex() {
        table = new TreeMap<>();
        fetcher = Fetcher.getFetcher(Constants.getCompression());

        numDocuments = 0;
        totalLength = 0;
    }

    public int getTotalLength() {
        return totalLength;
    }

    public int getNumDocuments() {
        return numDocuments;
    }

    public void setTotalLength(int totalLength) {
        this.totalLength = totalLength;
    }

    public void setNumDocuments(int numDocuments) {
        this.numDocuments = numDocuments;
    }

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

    public void addDocument(int docId, int docLength) {
        if (table.containsKey(docId))
            return;

        DocumentIndexEntry die = new DocumentIndexEntry();
        die.setDocumentLength(docLength);
        table.put(docId, die);
        numDocuments += 1;
        totalLength += docLength;
    }

    public Iterable<Map.Entry<Integer, DocumentIndexEntry>> getEntries() {
        return table.entrySet();
    }
}
