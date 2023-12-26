package it.unipi.io.implementation;

import it.unipi.encoding.CompressionType;
import it.unipi.io.Fetcher;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

public class FetcherTXT implements Fetcher {

    private static final Logger logger = LoggerFactory.getLogger(FetcherTXT.class);

    private BufferedReader vocabularyReader;
    private BufferedReader documentIndexReader;

    private BufferedReader docIdsReader;
    private BufferedReader termFreqReader;

    private boolean opened = false;
    private Path path;

    @Override
    public boolean start(Path path) {
        if (!opened) {
            try {
                vocabularyReader = new BufferedReader(
                        new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile())
                );
                documentIndexReader = new BufferedReader(
                        new FileReader(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile())
                );

                docIdsReader = new BufferedReader(
                        new FileReader(path.resolve(Constants.DOC_IDS_FILENAME).toFile())
                );
                termFreqReader = new BufferedReader(
                        new FileReader(path.resolve(Constants.TERM_FREQ_FILENAME).toFile())
                );

                opened = true;
                this.path = path;
                logger.trace("Fetcher correctly initialized at path " + path);

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
            vocabularyReader.close();
            documentIndexReader.close();
            docIdsReader.close();
            termFreqReader.close();

            logger.trace("Fetcher correctly closed");
            opened = false;

        } catch (Exception exception) {
            logger.warn("Error in closing the fetcher", exception);
        }
    }

    protected void loadPosting(VocabularyEntry entry) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        long docIdsOffset = entry.getDocIdsOffset();
        long termFreqOffset = entry.getTermFreqOffset();

        docIdsReader = new BufferedReader(new FileReader(path.resolve(Constants.DOC_IDS_FILENAME).toFile()));
        termFreqReader = new BufferedReader(new FileReader(path.resolve(Constants.TERM_FREQ_FILENAME).toFile()));

        // Set the readers to the correct offsets (computed before)
        if (docIdsReader.skip(docIdsOffset) != docIdsOffset)
            throw new IOException("Error in reading posting list doc ids");
        if (termFreqReader.skip(termFreqOffset) != termFreqOffset)
            throw new IOException("Error in reading posting list term frequencies");

        loadNextPosting(entry);
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
        line = vocabularyReader.readLine();
        String toReach = null;
        if (line == null) {
            // CASE 1
            vocabularyReader.close();
            vocabularyReader = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
            line = vocabularyReader.readLine();

        } else {
            String termToCompare = line.split(",")[0];
            int comparison = term.compareTo(termToCompare);

            if (comparison < 0) {
                // CASE 2
                toReach = line.split(",")[0];
                vocabularyReader.close();
                vocabularyReader = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
                line = vocabularyReader.readLine();
            }
        }

        do {
            String[] params = line.split(",");

            if (params[0].equals(toReach))
                return null;

            else if (params[0].equals(term)) {
                result = parseVocabularyEntry(line);
                loadPosting(result);
                return result;
            }
            line = vocabularyReader.readLine();
        } while (line != null);

        // We have not found this term
        logger.debug("Term " + term + " was not found in the vocabulary");
        return null;
    }

    private void loadNextPosting(VocabularyEntry entry) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        // In the TXT case, the length is the number of postings
        int len = entry.getDocIdsLength();

        // Don't move readers: beginning of new posting list is right after end of the previous one
        for (int i = 0; i < len; i++) {
            entry.getPostingList().addPosting(
                    Integer.parseInt(docIdsReader.readLine()),
                    Integer.parseInt(termFreqReader.readLine())
            );
        }
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        VocabularyEntry result;
        String term;

        // Read next line
        String line = vocabularyReader.readLine();
        if (line == null)
            return null;

        String[] params = line.split(",");
        term = params[0];
        result = parseVocabularyEntry(line);
        loadNextPosting(result);

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
        line = documentIndexReader.readLine();
        Long toReach = null;

        if (line == null) {
            // CASE 1
            documentIndexReader.close();
            documentIndexReader = new BufferedReader(new FileReader(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));
            line = documentIndexReader.readLine();

        } else {
            boolean comparison = docId < Long.parseLong(line.split(",")[0]);
            if (comparison) {
                // CASE 2
                toReach = Long.parseLong(line.split(",")[0]);
                documentIndexReader.close();
                documentIndexReader = new BufferedReader(new FileReader(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));
                line = documentIndexReader.readLine();
            }
        }

        do {
            String[] params = line.split(",");

            if(toReach != null && Long.parseLong(params[0]) == toReach)
                return null;

            if (Long.parseLong(params[0]) == docId) {
                return DocumentIndexEntry.parseTXT(line);
            }
            line = documentIndexReader.readLine();
        } while (line != null);

        // We have not found this docId in the document index
        logger.debug("Doc id " + docId + " was not found in the document index");
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
        String line = documentIndexReader.readLine();
        if (line == null)
            return null;

        String[] params = line.split(",");
        docId = Integer.parseInt(params[0]);
        result = DocumentIndexEntry.parseTXT(line);

        return Map.entry(docId, result);
    }

    @Override
    public int[] getDocumentIndexStats() throws IOException {
        int l = Integer.parseInt(documentIndexReader.readLine());
        int N = Integer.parseInt(documentIndexReader.readLine());

        return new int[] {N, l};
    }

    private static VocabularyEntry parseVocabularyEntry(String line) {
        VocabularyEntry entry = new VocabularyEntry();
        String[] params = line.split(",");

        int documentFrequency = Integer.parseInt(params[1]);
        double upperBound = Double.parseDouble(params[2]);

        PostingList postingList = PostingList.getInstance(CompressionType.DEBUG, entry);
        entry.setDocIdsOffset(Long.parseLong(params[3]));
        entry.setTermFreqOffset(Long.parseLong(params[4]));
        int length = Integer.parseInt(params[5]);
        entry.setDocIdsLength(length);
        entry.setTermFreqLength(length);

        entry.setPostingList(postingList);
        entry.setUpperBound(upperBound);
        entry.setDocumentFrequency(documentFrequency);
        return entry;
    }
}
