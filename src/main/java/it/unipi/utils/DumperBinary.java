package it.unipi.utils;

import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.PostingListImpl;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class DumperBinary implements Dumper {

    // vocabulary writer
    private FileChannel  vocabularyWriter;
    // Doc Ids writer
    private FileChannel docIdsWriter;
    // Term frequencies writer
    private FileChannel termFreqWriter;
    // documentIndexWriter
    private FileChannel documentIndexWriter;

    private long docIdsOffset;
    private long termFreqOffset;
    private boolean opened;

    @Override
    public boolean start(String path) {
        try {
            if (opened)
                throw new IOException();

            IOUtils.createDirectory(path);

            vocabularyWriter    = FileChannel.open(Paths.get(path, Constants.VOCABULARY_FILENAME),      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            documentIndexWriter = FileChannel.open(Paths.get(path, Constants.DOCUMENT_INDEX_FILENAME),  StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            docIdsWriter        = FileChannel.open(Paths.get(path, Constants.DOC_IDS_POSTING_FILENAME), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            termFreqWriter      = FileChannel.open(Paths.get(path, Constants.TF_POSTING_FILENAME),      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            opened = true;
            docIdsOffset = termFreqOffset = 0;

        } catch(IOException ie) {
            ie.printStackTrace();
            System.out.println("Error in opening the file");
            opened = false;
        }
        return opened;
    }

    @Override
    public void dumpVocabularyEntry(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        String term = entry.getKey();
        VocabularyEntry vocabularyEntry = entry.getValue();

        int documentFrequency = vocabularyEntry.getDocumentFrequency();
        Double upperBound = vocabularyEntry.getUpperBound();
        PostingList pList = vocabularyEntry.getPostingList();
        if(!(pList instanceof PostingListImpl))
            throw new RuntimeException("Cannot dump a compressed posting list, it must be full in memory");

        PostingListImpl postingList = (PostingListImpl) pList;
        double idf = postingList.getIdf();
        List<Integer> docIdList = postingList.getDocIds();
        List<Integer> termFrequencyList = postingList.getTermFrequencies();

        // dump docIds
        int docIdsLength = docIdList.size() * Integer.BYTES;
        ByteBuffer docIdsBuffer = ByteBuffer.allocate(docIdsLength);
        for (Integer docId : docIdList)
            docIdsBuffer.putInt(docId);
        docIdsWriter.write(docIdsBuffer.flip());

        // dump term frequencies
        int termFreqLength = termFrequencyList.size() * Integer.BYTES;
        ByteBuffer termFreqBuffer = ByteBuffer.allocate(termFreqLength);
        for (Integer termFreq : termFrequencyList)
            termFreqBuffer.putInt(termFreq);
        termFreqWriter.write(termFreqBuffer.flip());

        // Dump vocabulary entry
        byte[] stringBytes = term.getBytes(StandardCharsets.UTF_8);
        byte[] stringTruncatedBytes = new byte[Constants.BYTES_STORED_STRING]; // Truncate string to store a fixed number of bytes
        System.arraycopy(stringBytes, 0, stringTruncatedBytes, 0, Math.min(stringBytes.length, Constants.BYTES_STORED_STRING));
        for (int i = Math.min(stringBytes.length, Constants.BYTES_STORED_STRING); i < Constants.BYTES_STORED_STRING; i++)
            stringTruncatedBytes[i] = '\0';

        // Write to buffer and then dump
        ByteBuffer vocBuffer = ByteBuffer.allocate(Constants.VOCABULARY_ENTRY_BYTES_SIZE);
        vocBuffer.put(stringTruncatedBytes);
        vocBuffer.putInt(documentFrequency)
                .putDouble(upperBound)
                .putDouble(idf)
                .putLong(docIdsOffset)
                .putInt(docIdsLength)
                .putLong(termFreqOffset)
                .putInt(termFreqLength);
        vocabularyWriter.write(vocBuffer.flip());

        docIdsOffset += docIdsLength;
        termFreqOffset += termFreqLength;
    }

    @Override
    public void dumpDocumentIndex(DocumentIndex docIndex) {
        int numEntries;
        // Write document index info (length and number of documents
        try {
            ByteBuffer buffer = ByteBuffer.allocate(2 * Integer.BYTES);
            buffer.putInt(docIndex.getTotalLength());
            numEntries = docIndex.getNumDocuments();
            buffer.putInt(numEntries);
            documentIndexWriter.write(buffer);
        } catch (IOException ie) {
            ie.printStackTrace();
            return;
        }

        // Dump all entries
        try {
            ByteBuffer docIndexBuffer = ByteBuffer.allocate(numEntries * 2 * Integer.BYTES);    // For each entry we have 2 integers
            for (Map.Entry<Integer, DocumentIndexEntry> entry : docIndex.getEntries())
                dumpDocumentIndexEntry(entry, docIndexBuffer);
            documentIndexWriter.write(docIndexBuffer.flip());
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    private void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry, ByteBuffer buffer) {
        int docId = entry.getKey();
        DocumentIndexEntry documentIndexEntry = entry.getValue();

        buffer.putInt(docId);
        buffer.putInt(documentIndexEntry.getDocumentLength());
    }

    @Override
    public void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry) {
        ByteBuffer buffer = ByteBuffer.allocate(2 * Integer.BYTES);
        dumpDocumentIndexEntry(entry, buffer);
        try {
            documentIndexWriter.write(buffer.flip());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dumpVocabulary(Vocabulary vocabulary) {
        try {
            // Keep space at the beginning of file to store the number of entries
            for (Map.Entry<String, VocabularyEntry> entry : vocabulary.getEntries()) {
                dumpVocabularyEntry(entry);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean end() {
        try {
            if (opened) {
                vocabularyWriter.close();
                docIdsWriter.close();
                termFreqWriter.close();
                documentIndexWriter.close();
                opened = false;
            } else
                throw new IOException();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return !opened;
    }
}
