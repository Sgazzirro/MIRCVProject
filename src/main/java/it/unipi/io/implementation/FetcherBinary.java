package it.unipi.io.implementation;

import it.unipi.io.Fetcher;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.ByteUtils;
import it.unipi.encoding.CompressionType;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FetcherBinary implements Fetcher {

    protected static final Logger logger = LoggerFactory.getLogger(FetcherBinary.class);

    private   FileInputStream vocabularyReader;
    private   FileInputStream documentIndexReader;
    protected FileInputStream docIdsReader;
    protected FileInputStream termFreqReader;

    protected boolean opened = false;
    protected CompressionType compression;

    protected Path path;
    private int vocabularySize;
    private int documentIndexSize;

    public FetcherBinary() {
        compression = CompressionType.BINARY;
    }

    @Override
    public boolean start(Path path) {
        if (!opened) {
            try {
                Path vocPath = path.resolve(Constants.VOCABULARY_FILENAME);
                Path docPath = path.resolve(Constants.DOCUMENT_INDEX_FILENAME);
                vocabularySize = ((int) Files.size(vocPath) - Constants.VOCABULARY_HEADER_BYTES)
                        / Constants.VOCABULARY_ENTRY_BYTES_SIZE;
                documentIndexSize = ((int) Files.size(docPath) - Constants.DOCUMENT_INDEX_HEADER_BYTES)
                        / Constants.DOCUMENT_INDEX_ENTRY_BYTES_SIZE;

                vocabularyReader    = new FileInputStream(vocPath.toFile());
                docIdsReader        = new FileInputStream(path.resolve(Constants.DOC_IDS_POSTING_FILENAME).toFile());
                termFreqReader      = new FileInputStream(path.resolve(Constants.TF_POSTING_FILENAME).toFile());
                documentIndexReader = new FileInputStream(docPath.toFile());

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
            docIdsReader.close();
            termFreqReader.close();
            documentIndexReader.close();

            logger.trace("Fetcher correctly closed");
            opened = false;

        } catch (Exception exception) {
            logger.warn("Error in closing the fetcher", exception);
        }
    }

    protected void loadPosting(VocabularyEntry entry) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        // Set file readers to the beginning of the posting to read
        long docIdsOffset   = entry.getDocIdsOffset();
        long termFreqOffset = entry.getTermFreqOffset();
        docIdsReader.getChannel().position(docIdsOffset);
        termFreqReader.getChannel().position(termFreqOffset);

        // Read the posting
        loadNextPosting(entry);
    }

    @Override
    public VocabularyEntry loadVocEntry(String term) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        // Binary search to find the term
        // Remember to consider also the int at the beginning of file denoting vocabulary size
        int start = 0, end = vocabularySize;
        int middle;
        byte[] vocabularyEntryBytes = new byte[Constants.VOCABULARY_ENTRY_BYTES_SIZE];

        // Compare terms by truncating the bytes representation of the original term
        byte[] termBytes = new byte[Constants.BYTES_STORED_STRING];
        int termNumBytes = Math.min(term.getBytes().length, Constants.BYTES_STORED_STRING);
        System.arraycopy(term.getBytes(), 0, termBytes, 0, termNumBytes);
        String truncatedTerm = ByteUtils.bytesToString(termBytes, 0, Constants.BYTES_STORED_STRING);

        while (true) {
            middle = (end + start) / 2;
            // Set the file pointer to the entry of index middle
            vocabularyReader.getChannel().position(
                    (long) middle * Constants.VOCABULARY_ENTRY_BYTES_SIZE + Constants.VOCABULARY_HEADER_BYTES);

            if (vocabularyReader.read(vocabularyEntryBytes, 0, Constants.VOCABULARY_ENTRY_BYTES_SIZE) != Constants.VOCABULARY_ENTRY_BYTES_SIZE)
                throw new IOException("Could not read vocabulary entry");

            String currentTerm = ByteUtils.bytesToString(vocabularyEntryBytes, 0, Constants.BYTES_STORED_STRING);
            int comparison = truncatedTerm.compareTo(currentTerm);

            // Stop condition
            if (end - start == 1 && comparison != 0)
                return null;

            if (comparison > 0)         // This means term > entry
                start = middle;
            else if (comparison < 0)    // This means term < entry
                end = middle;
            else {                      // This means term = entry
                VocabularyEntry result = ByteUtils.bytesToVocabularyEntry(vocabularyEntryBytes, compression);
                loadPosting(result);
                return result;
            }
        }
    }

    protected void loadNextPosting(VocabularyEntry entry) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        int docIdsLength = entry.getDocIdsLength();

        DataInputStream disDocId = new DataInputStream(docIdsReader);
        DataInputStream disTermFreq = new DataInputStream(termFreqReader);

        // Don't move readers: beginning of new posting list is right after end of the previous one
        for (int len = 0; len < docIdsLength; len += Integer.BYTES) {
            int docId = disDocId.readInt();
            int termFreq = disTermFreq.readInt();

            entry.getPostingList().addPosting(docId, termFreq);
        }
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        VocabularyEntry vocabularyEntry;
        String term;

        byte[] vocabularyEntryBytes = new byte[Constants.VOCABULARY_ENTRY_BYTES_SIZE];
        if (vocabularyReader.read(vocabularyEntryBytes) != Constants.VOCABULARY_ENTRY_BYTES_SIZE)
            return null;

        vocabularyEntry = ByteUtils.bytesToVocabularyEntry(vocabularyEntryBytes, compression);
        loadNextPosting(vocabularyEntry);

        term = ByteUtils.bytesToString(vocabularyEntryBytes, 0, Constants.BYTES_STORED_STRING);
        return Map.entry(term, vocabularyEntry);
    }

    @Override
    public DocumentIndexEntry loadDocEntry(long docId) throws IOException{
        if (!opened)
            throw new IOException("Fetcher has not been started");

        // Binary search to find the docId
        // Remember to consider also the ints at the beginning of file denoting documentIndex
        int start = 0, end = documentIndexSize;
        int middle;
        byte[] documentIndexEntryBytes = new byte[Constants.DOCUMENT_INDEX_ENTRY_BYTES_SIZE];

        while (true) {
            middle = (end + start) / 2;
            // Set the file pointer to the entry of index middle
            documentIndexReader.getChannel().position(
                    (long) middle * Constants.DOCUMENT_INDEX_ENTRY_BYTES_SIZE + Constants.DOCUMENT_INDEX_HEADER_BYTES);

            if (documentIndexReader.read(documentIndexEntryBytes, 0, Constants.DOCUMENT_INDEX_ENTRY_BYTES_SIZE) != Constants.DOCUMENT_INDEX_ENTRY_BYTES_SIZE)
                throw new IOException("Could not read document index entry");

            int currentDocId = ByteUtils.bytesToInt(documentIndexEntryBytes, 0);

            // Stop condition
            if (end == start + 1 && currentDocId != docId)
                return null;

            if (docId > currentDocId)
                start = middle;
            else if (docId < currentDocId)
                end = middle;
            else {
                return ByteUtils.bytesToDocumentIndexEntry(documentIndexEntryBytes);
            }
        }
    }

    @Override
    public Map.Entry<Integer, DocumentIndexEntry> loadDocEntry() throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        DocumentIndexEntry documentIndexEntry;
        byte[] docId, length;

        docId = new byte[Integer.BYTES];     // Store the 4 bytes of docId
        if (documentIndexReader.read(docId) != Integer.BYTES)
            return null;

        length = new byte[Integer.BYTES];    // Store the 4 bytes of document length
        if (documentIndexReader.read(length) != Integer.BYTES)
            return null;

        documentIndexEntry = new DocumentIndexEntry();
        documentIndexEntry.setDocumentLength(ByteUtils.bytesToInt(length));

        return Map.entry(ByteUtils.bytesToInt(docId), documentIndexEntry);
    }

    @Override
    public int[] getDocumentIndexStats() throws IOException {
        byte[] N = new byte[Integer.BYTES],
                l = new byte[Integer.BYTES];

        if (documentIndexReader.read(l) + documentIndexReader.read(N) != 2 * Integer.BYTES)
            throw new IOException("Document index: error reading metadata");

        return new int[] {ByteUtils.bytesToInt(N), ByteUtils.bytesToInt(l)};
    }
}