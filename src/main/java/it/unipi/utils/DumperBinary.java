package it.unipi.utils;

import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.model.implementation.DocumentIndexEntry;

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
    protected FileChannel  vocabularyWriter;
    // Doc Ids writer
    protected FileChannel docIdsWriter;
    // Term frequencies writer
    protected FileChannel termFreqWriter;
    // documentIndexWriter
    protected FileChannel documentIndexWriter;

    protected long docIdsOffset;
    protected long termFreqOffset;
    protected boolean opened;

    private ByteBuffer docIndexBuffer;
    private static final int docIndexBufferCapacity = 100000;

    @Override
    public boolean start(Path path) {
        try {
            if (opened)
                throw new IOException();

            IOUtils.createDirectory(path);

            vocabularyWriter    = FileChannel.open(path.resolve(Constants.VOCABULARY_FILENAME),      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            documentIndexWriter = FileChannel.open(path.resolve(Constants.DOCUMENT_INDEX_FILENAME),  StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            docIdsWriter        = FileChannel.open(path.resolve(Constants.DOC_IDS_POSTING_FILENAME), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            termFreqWriter      = FileChannel.open(path.resolve(Constants.TF_POSTING_FILENAME),      StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            opened = true;
            docIdsOffset = termFreqOffset = 0;

        } catch(IOException ie) {
            System.out.println("Error in opening the file");
            opened = false;
        }
        return opened;
    }

    @Override
    public void dumpVocabulary(Vocabulary vocabulary) {
        try {
            for (Map.Entry<String, VocabularyEntry> entry : vocabulary.getEntries())
                dumpVocabularyEntry(entry);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dumpVocabularyEntry(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        String term = entry.getKey();
        VocabularyEntry vocabularyEntry = entry.getValue();
        
        int documentFrequency = vocabularyEntry.getDocumentFrequency();
        double upperBound = vocabularyEntry.getUpperBound();
        
        PostingList postingList = vocabularyEntry.getPostingList();

        double idf = postingList.getIdf();
        List<Integer> docIdList = postingList.getDocIdsDecompressedList();
        List<Integer> termFrequencyList = postingList.getTermFrequenciesDecompressedList();
        
        if (!opened)
            throw new IOException("Dumper must be opened");
        
        // dump docIds
        int docIdsLength = dumpDocIds(docIdList);

        // dump term frequencies
        int termFreqLength = dumpTermFrequencies(termFrequencyList);

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
    
    protected int dumpDocIds(List<Integer> docIdList) throws IOException {
        int docIdsLength = docIdList.size() * Integer.BYTES;
        ByteBuffer docIdsBuffer = ByteBuffer.allocate(docIdsLength);
        for (Integer docId : docIdList)
            docIdsBuffer.putInt(docId);
        
        return docIdsWriter.write(docIdsBuffer.flip());
    }
    
    protected int dumpTermFrequencies(List<Integer> termFrequencyList) throws IOException {
        int termFreqLength = termFrequencyList.size() * Integer.BYTES;
        ByteBuffer termFreqBuffer = ByteBuffer.allocate(termFreqLength);
        for (Integer termFreq : termFrequencyList)
            termFreqBuffer.putInt(termFreq);
        
        return termFreqWriter.write(termFreqBuffer.flip());
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
            documentIndexWriter.write(buffer.flip());
        } catch (IOException ie) {
            ie.printStackTrace();
            return;
        }

        // Dump all entries
        try {
            docIndexBuffer = ByteBuffer.allocate(numEntries * 2 * Integer.BYTES);    // For each entry we have 2 integers
            for (Map.Entry<Integer, DocumentIndexEntry> entry : docIndex.getEntries())
                dumpDocumentIndexEntry(entry, docIndexBuffer);
            documentIndexWriter.write(docIndexBuffer.flip());

            // Clear the buffer
            docIndexBuffer = null;

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
        if (docIndexBuffer == null)
            docIndexBuffer = ByteBuffer.allocate(docIndexBufferCapacity);
        else if (!docIndexBuffer.hasRemaining()) {
            try {
                documentIndexWriter.write(docIndexBuffer.flip());
                docIndexBuffer = ByteBuffer.allocate(docIndexBufferCapacity);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        dumpDocumentIndexEntry(entry, docIndexBuffer);
    }

    @Override
    public boolean end() {
        try {
            if (opened) {
                vocabularyWriter.close();
                docIdsWriter.close();
                termFreqWriter.close();

                if (docIndexBuffer != null) {
                    documentIndexWriter.write(docIndexBuffer.flip());
                    docIndexBuffer = null;
                }
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
