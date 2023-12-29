package it.unipi.model;

import it.unipi.io.Fetcher;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class DocumentIndex {
    private int totalLength;
    private int numDocuments;
    private final NavigableMap<Integer, DocumentIndexEntry> table;
    private final Fetcher fetcher;
    private static final Logger logger = LoggerFactory.getLogger(DocumentIndex.class);

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

    public int getDocNo(int docId){
        if (!table.containsKey(docId)) {
            String errorMessage = "Could not fetch docId " + docId + " from document index";

            try {
                fetcher.start(Constants.getPath());
                DocumentIndexEntry entry = fetcher.loadDocEntry(docId);
                fetcher.close();

                if (entry != null) {
                    table.put(docId, entry);
                    return entry.getDocNo();
                }
            } catch (Exception e) {
                logger.error(errorMessage, e);
            }

            throw new RuntimeException(errorMessage);
        }

        return table.get(docId).getDocNo();
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

    public void addDocument(int docId, int docno, int docLength) {
        if (table.containsKey(docId))
            return;

        DocumentIndexEntry die = new DocumentIndexEntry();
        die.setDocumentLength(docLength);
        die.setDocNo(docno);
        table.put(docId, die);
        numDocuments += 1;
        totalLength += docLength;
    }

    public Iterable<Map.Entry<Integer, DocumentIndexEntry>> getEntries() {
        return table.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentIndex that = (DocumentIndex) o;
        return totalLength == that.totalLength && numDocuments == that.numDocuments && Objects.equals(table, that.table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalLength, numDocuments, table);
    }

    public void chargeInMemory(){
        // used to ENTIRELY charge in memory the document index, for speed purposes
        try {
            fetcher.start(Constants.getPath());
            int[] stats = fetcher.getDocumentIndexStats();
            numDocuments=stats[0];
            totalLength=stats[1];
            Constants.N=numDocuments;
            Map.Entry<Integer, DocumentIndexEntry> entry;
            while((entry = fetcher.loadDocEntry())!=null){
                table.put(entry.getKey(), entry.getValue());
            }
            fetcher.close();
        } catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    public void chargeHeader(){
        // used to charge the header in mememory
        try{
            fetcher.start(Constants.getPath());
            int[] stats = fetcher.getDocumentIndexStats();
            numDocuments = stats[0];
            totalLength = stats[1];
            Constants.N=numDocuments;
            fetcher.close();
        } catch (Exception e){
            logger.error(e.getMessage());
        }
    }


}
