package it.unipi.io.implementation;

import it.unipi.io.Fetcher;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.ByteUtils;
import it.unipi.encoding.CompressionType;
import it.unipi.utils.Constants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FetcherBinary implements Fetcher {

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
        this.path = path;

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
            /*documentIndexReader = new BufferedInputStream(
                    new FileInputStream(docPath.toFile())
            );*/
            opened = true;

        } catch (IOException ie) {
            ie.printStackTrace();
            opened = false;
        }
        return opened;
    }

    @Override
    public boolean end() {
        try {
            if (opened) {
                vocabularyReader.close();
                docIdsReader.close();
                termFreqReader.close();
                documentIndexReader.close();
                opened = false;
            } else
                throw new IOException();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return !opened;
    }



    @Override
    public void loadPosting(VocabularyEntry entry) {
        long docIdsOffset = entry.getDocIdsOffset(),
                termFreqOffset = entry.getTermFreqOffset();
        int docIdsLength = entry.getDocIdsLength();

        try {
            docIdsReader.getChannel().position(docIdsOffset);
            DataInputStream disDocId = new DataInputStream(docIdsReader);

            termFreqReader.getChannel().position(termFreqOffset);
            DataInputStream disTermFreq = new DataInputStream(termFreqReader);


            // load docIds and termFreq
            for (int len = 0; len < docIdsLength; len += Integer.BYTES) {
                int docId = disDocId.readInt();
                int termFreq = disTermFreq.readInt();

                entry.getPostingList().addPosting(docId, termFreq);
            }
            //disDocId.close();
            //disTermFreq.close();

        } catch (IOException ie){
            ie.printStackTrace();
        }
    }

    @Override
    public VocabularyEntry loadVocEntry(String term) {
        try {
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
                if (end == start && comparison != 0)
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() {
        VocabularyEntry vocabularyEntry;
        String term;
        try {
            if (!opened)
                throw new IOException("Fetcher has not been started");

            byte[] vocabularyEntryBytes = new byte[Constants.VOCABULARY_ENTRY_BYTES_SIZE];
            if (vocabularyReader.read(vocabularyEntryBytes) != Constants.VOCABULARY_ENTRY_BYTES_SIZE)
                return null;
            vocabularyEntry = ByteUtils.bytesToVocabularyEntry(vocabularyEntryBytes, compression);
            loadPosting(vocabularyEntry);
            //vocabularyEntry.getPostingList().loadPosting(path);

            term = ByteUtils.bytesToString(vocabularyEntryBytes, 0, Constants.BYTES_STORED_STRING);

        } catch (IOException ie) {
            ie.printStackTrace();
            return null;
        }
        return Map.entry(term, vocabularyEntry);
    }

    @Override
    public DocumentIndexEntry loadDocEntry(long docId) {
        try {
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
                if (end == start && currentDocId != docId)
                    return null;

                if (docId > currentDocId)
                    start = middle;
                else if (docId < currentDocId)
                    end = middle;
                else {
                    return ByteUtils.bytesToDocumentIndexEntry(documentIndexEntryBytes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map.Entry<Integer, DocumentIndexEntry> loadDocEntry() {
        DocumentIndexEntry documentIndexEntry;
        byte[] docId, length;

        try {
            if (!opened)
                throw new IOException("Fetcher has not been started");

            docId = new byte[Integer.BYTES];     // Store the 4 bytes of docId
            if (documentIndexReader.read(docId) != Integer.BYTES)
                return null;

            length = new byte[Integer.BYTES];    // Store the 4 bytes of document length
            if (documentIndexReader.read(length) != Integer.BYTES)
                return null;

            documentIndexEntry = new DocumentIndexEntry();
            documentIndexEntry.setDocumentLength(ByteUtils.bytesToInt(length));

        } catch(IOException ie) {
            ie.printStackTrace();
            return null;
        }

        return Map.entry(ByteUtils.bytesToInt(docId), documentIndexEntry);
    }

    @Override
    public int[] getDocumentIndexStats() {
        byte[] N = new byte[Integer.BYTES],
                l = new byte[Integer.BYTES];

        try {
            if (documentIndexReader.read(l) + documentIndexReader.read(N) != 2 * Integer.BYTES)
                throw new IOException("Document index: error reading metadata");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new int[] {ByteUtils.bytesToInt(N), ByteUtils.bytesToInt(l)};
    }
}