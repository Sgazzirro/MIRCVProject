package it.unipi.io.implementation;

import it.unipi.io.Fetcher;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

public class FetcherTXT implements Fetcher {

    private static final Logger logger = LoggerFactory.getLogger(FetcherTXT.class);

    BufferedReader globalReaderVOC;
    BufferedReader globalReaderDOC;
    boolean opened = false;
    Path path;

    @Override
    public boolean start(Path path) {
        if (!opened) {
            try {
                globalReaderVOC = new BufferedReader(
                        new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile())
                );
                globalReaderDOC = new BufferedReader(
                        new FileReader(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile())
                );

                opened = true;
                this.path = path;
                logger.info("Fetcher correctly initialized at path: " + path);

            } catch (IOException ie) {
                logger.error("Could not start fetcher", ie);
                opened = false;
            }
        } else {
            logger.warn("Fetcher was already opened");
        }

        return opened;
    }

    @Override
    public void close() {
        if (!opened) {
            logger.warn("Could not close fetcher: it was not started");
            return;
        }

        try {
            globalReaderVOC.close();
            globalReaderDOC.close();

            logger.info("Fetcher correctly closed");
            opened = false;

        } catch (Exception exception) {
            logger.warn("Error in closing the fetcher", exception);
        }
    }

    @Override
    public void loadPosting(VocabularyEntry entry) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        // The loading of a posting list uses two inner buffers

        long[] offsets = new long[]{entry.getDocIdsOffset(), entry.getTermFreqOffset()};
        // In the TXT case, the length is the number of postings
        int len = entry.getDocIdsLength();

        try (
                BufferedReader readerIds = new BufferedReader(new FileReader(path.resolve(Constants.DOC_IDS_POSTING_FILENAME).toFile()));
                BufferedReader readerTF = new BufferedReader(new FileReader(path.resolve(Constants.TF_POSTING_FILENAME).toFile()))
        ) {
            // Set the readers to the correct offsets (computed before)
            if (readerIds.skip(offsets[0]) != offsets[0])
                throw new IOException("Error in reading posting list doc ids");
            if (readerTF.skip(offsets[1]) != offsets[1])
                throw new IOException("Error in reading posting list term frequencies");

            for (int i = 0; i < len; i++)
                entry.getPostingList().addPosting(
                        Integer.parseInt(readerIds.readLine()),
                        Integer.parseInt(readerTF.readLine())
                );
        }
    }

    public VocabularyEntry loadVocEntry(String term) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        // Use the global reader and restart it if EOF reached
        VocabularyEntry result;
        String line;

        // In the TXT version, no binary search has been implemented
        // Anyway, below we propose a slight optimization of the search based on the previous one

        // CASES AT FIRST READ
        // - line = null EOF reached, restart and parse the whole file
        // - line != null and comparison < 0, you have to restart and search until you come back to that term
        // - line != null and comparison > 0, you have to search until EOF (do nothing)
        line = globalReaderVOC.readLine();
        String toReach = null;
        if (line == null) {
            // CASE 1
            globalReaderVOC.close();
            globalReaderVOC = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
            line = globalReaderVOC.readLine();

        } else {
            String termToCompare = line.split(",")[0];
            int comparison = term.compareTo(termToCompare);

            if (comparison < 0) {
                // CASE 2
                toReach = line.split(",")[0];
                globalReaderVOC.close();
                globalReaderVOC = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
                line = globalReaderVOC.readLine();
            }
        }

        do {
            String[] params = line.split(",");

            if (params[0].equals(toReach))
                return null;

            else if (params[0].equals(term)) {
                result = VocabularyEntry.parseTXT(line);
                loadPosting(result);
                return result;
            }
            line = globalReaderVOC.readLine();
        } while (line != null);

        // We have not found this term
        logger.info("Term " + term + " was not found in the vocabulary");
        return null;
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        // The loading of an entry without arguments uses the global reader
        VocabularyEntry result;
        String term;

        // Read next line
        String line = globalReaderVOC.readLine();
        if (line == null)
            return null;

        String[] params = line.split(",");
        term = params[0];
        result = VocabularyEntry.parseTXT(line);
        loadPosting(result);

        return Map.entry(term, result);
    }

    @Override
    public DocumentIndexEntry loadDocEntry(long docId) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        String line;

        // CASES AT FIRST READ
        // - line = null EOF reached, restart and parse the whole file
        // - line != null and comparison < 0, you have to restart and search until you come back to that term
        // - line != null and comparison > 0, you have to search until EOF (do nothing)
        line = globalReaderDOC.readLine();
        Long toReach = null;

        if (line == null) {
            // CASE 1
            globalReaderDOC.close();
            globalReaderDOC = new BufferedReader(new FileReader(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));
            line = globalReaderDOC.readLine();

        } else {
            boolean comparison = docId < Long.parseLong(line.split(",")[0]);
            if (comparison) {
                // CASE 2
                toReach = Long.parseLong(line.split(",")[0]);
                globalReaderDOC.close();
                globalReaderDOC = new BufferedReader(new FileReader(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));
                line = globalReaderDOC.readLine();
            }
        }

        do {
            String[] params = line.split(",");

            if(toReach != null && Long.parseLong(params[0]) == toReach)
                return null;

            if (Long.parseLong(params[0]) == docId) {
                return DocumentIndexEntry.parseTXT(line);
            }
            line = globalReaderDOC.readLine();
        } while (line != null);

        // We have not found this docId in the document index
        logger.info("Doc id " + docId + " was not found in the document index");
        return null;
    }

    @Override
    public Map.Entry<Integer, DocumentIndexEntry> loadDocEntry() throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        // The loading of an entry without arguments uses the global reader
        DocumentIndexEntry result;
        int docId;

        // Read next line
        String line = globalReaderDOC.readLine();
        if (line == null)
            return null;

        String[] params = line.split(",");
        docId = Integer.parseInt(params[0]);
        result = DocumentIndexEntry.parseTXT(line);

        return Map.entry(docId, result);
    }

    @Override
    public int[] getDocumentIndexStats() throws IOException {
        int l = Integer.parseInt(globalReaderDOC.readLine());
        int N = Integer.parseInt(globalReaderDOC.readLine());

        return new int[] {N, l};
    }
}
