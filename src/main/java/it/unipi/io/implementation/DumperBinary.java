package it.unipi.io.implementation;

import it.unipi.io.Dumper;
import it.unipi.model.*;
import it.unipi.utils.Constants;
import it.unipi.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class DumperBinary implements Dumper {

    private static final Logger logger = LoggerFactory.getLogger(DumperBinary.class);

    protected FileChannel  vocabularyWriter;
    protected FileChannel docIdsWriter;
    protected FileChannel termFreqWriter;
    protected FileChannel documentIndexWriter;

    protected long docIdsOffset;
    protected long termFreqOffset;
    protected boolean opened;

    private ByteBuffer docIndexBuffer;
    private int        docIndexBufferSize;
    private static final int DOC_INDEX_BUFFER_CAPACITY = 100000;

    @Override
    public boolean start(Path path) {
        if (!opened) {
            try {
                IOUtils.createDirectory(path);

                vocabularyWriter    = FileChannel.open(path.resolve(Constants.VOCABULARY_FILENAME),      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                documentIndexWriter = FileChannel.open(path.resolve(Constants.DOCUMENT_INDEX_FILENAME),  StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                docIdsWriter        = FileChannel.open(path.resolve(Constants.DOC_IDS_POSTING_FILENAME), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                termFreqWriter      = FileChannel.open(path.resolve(Constants.TF_POSTING_FILENAME),      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                docIdsOffset = termFreqOffset = 0;

                opened = true;
                logger.trace("Dumper correctly initialized at path: " + path);

            } catch (IOException ie) {
                logger.error("Could not start dumper", ie);
                opened = false;
            }
        } else {
            logger.warn("Dumper was already opened");
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
            vocabularyWriter.close();
            docIdsWriter.close();
            termFreqWriter.close();

            // Final flush of entries of document index in buffer
            if (docIndexBuffer != null)
                flushDocumentIndexBuffer();
            documentIndexWriter.close();

            logger.trace("Dumper correctly closed");
            opened = false;

        } catch (IOException exception) {
            logger.warn("Error in closing the dumper", exception);
        }
    }

    @Override
    public void dumpVocabulary(Vocabulary vocabulary) throws IOException {
        for (Map.Entry<String, VocabularyEntry> entry : vocabulary.getMapping().entrySet())
            dumpVocabularyEntry(entry);
    }

    @Override
    public void dumpVocabularyEntry(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        String term = entry.getKey();
        VocabularyEntry vocabularyEntry = entry.getValue();
        
        int documentFrequency = vocabularyEntry.getDocumentFrequency();
        double upperBound = vocabularyEntry.getUpperBound();
        
        PostingList postingList = vocabularyEntry.getPostingList();

        List<Integer> docIdList = postingList.getDocIdsList();
        List<Integer> termFrequencyList = postingList.getTermFrequenciesList();
        
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
                .putLong(docIdsOffset)
                .putInt(docIdsLength)
                .putLong(termFreqOffset)
                .putInt(termFreqLength);

        if (vocabularyWriter.write(vocBuffer.flip()) != Constants.VOCABULARY_ENTRY_BYTES_SIZE)
            throw new IOException("Could not dump vocabulary entry");

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
    public void dumpDocumentIndex(DocumentIndex docIndex) throws IOException {
        // Write document index info (length and number of documents
        ByteBuffer buffer = ByteBuffer.allocate(2 * Integer.BYTES);

        buffer.putInt(docIndex.getTotalLength());
        int numEntries = docIndex.getNumDocuments();
        buffer.putInt(numEntries);

        if (documentIndexWriter.write(buffer.flip()) != 2 * Integer.BYTES) {
            logger.error("Fatal error in dumping document index metadata: " +
                    "document index has not been correctly written to file");
            System.exit(1);
        }

        // Dump all entries
        docIndexBuffer = ByteBuffer.allocate(numEntries * Constants.DOCUMENT_INDEX_ENTRY_BYTES_SIZE);
        for (Map.Entry<Integer, DocumentIndexEntry> entry : docIndex.getEntries())
            dumpDocumentIndexEntry(entry, docIndexBuffer);

        // Dump left over entries in buffer
        flushDocumentIndexBuffer();
    }

    private void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry, ByteBuffer buffer) {
        int docId = entry.getKey();
        DocumentIndexEntry documentIndexEntry = entry.getValue();

        buffer.putInt(docId);
        buffer.putInt(documentIndexEntry.getDocumentLength());
        docIndexBufferSize += 2;
    }

    @Override
    public void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry) throws IOException {
        if (docIndexBuffer == null)
            docIndexBuffer = ByteBuffer.allocate(DOC_INDEX_BUFFER_CAPACITY);
        else if (!docIndexBuffer.hasRemaining()) {
            flushDocumentIndexBuffer();
            docIndexBuffer = ByteBuffer.allocate(DOC_INDEX_BUFFER_CAPACITY);
        }

        dumpDocumentIndexEntry(entry, docIndexBuffer);
    }

    public void flushDocumentIndexBuffer() throws IOException {
        if (docIndexBuffer == null)
            return;

        // Dump left over entries in buffer
        if (documentIndexWriter.write(docIndexBuffer.flip()) != docIndexBufferSize * Integer.BYTES) {
            logger.error("Fatal error in dumping document index: " +
                    "document index has not been correctly written to file");
            System.exit(1);
        }

        // Clear the buffer
        docIndexBuffer = null;
        docIndexBufferSize = 0;
    }
}
