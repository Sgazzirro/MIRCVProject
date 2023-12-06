package it.unipi.utils;

import it.unipi.model.DocumentIndex;
import it.unipi.model.Encoder;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.*;
import it.unipi.model.implementation.VocabularyEntry;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DumperCompressed implements Dumper {

    // vocabulary writer
    private FileChannel vocabularyWriter;
    // Doc Ids writer
    private FileChannel docIdsWriter;
    // Term frequencies writer
    private FileChannel termFreqWriter;
    // documentIndexWriter
    private FileChannel documentIndexWriter;

    private final Encoder docIdsEncoder = new EliasFano();
    private final Encoder tfEncoder = new Simple9(true);
    private boolean opened = false;

    private long docIdsOffset;
    private long termFreqOffset;

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
            System.err.println("Error in opening the file");
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
        List<Integer> tfList = postingList.getTermFrequenciesDecompressedList();

        int docIdsLength = 0, termFreqLength = 0;
        if (!opened)
            throw new IOException();

        // Dump doc ids
        int blockSize = Constants.BLOCK_SIZE;
        List<ByteBuffer> docIdsBuffers = new ArrayList<>();
        for (int i = 0; i < docIdList.size(); i += blockSize) {
            List<Integer> blockDocIdList = docIdList.subList(i, Math.min(docIdList.size(), i + blockSize));
            byte[] byteList = docIdsEncoder.encode(blockDocIdList);
            docIdsLength += byteList.length;

            docIdsBuffers.add(ByteBuffer.wrap(byteList));
        }
        docIdsWriter.write(docIdsBuffers.toArray(new ByteBuffer[0]));

        // Dump term frequencies
        List<ByteBuffer> termFreqBuffers = new ArrayList<>();
        for (int i = 0; i < docIdList.size(); i += blockSize) {
            List<Integer> blockTFList = tfList.subList(i, Math.min(docIdList.size(), i + blockSize));
            byte[] byteList = tfEncoder.encode(blockTFList);
            termFreqLength += byteList.length;

            termFreqBuffers.add(ByteBuffer.wrap(byteList));
        }
        termFreqWriter.write(termFreqBuffers.toArray(new ByteBuffer[0]));

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

    /*
    public void dumpMergedEntry(Map.Entry<String, VocabularyEntry> entry){
        // TO REVIEW: QUALCUNO DOVRà PRENDERSI START OFFSET E ENDOFFSET
        String term = entry.getKey();
        VocabularyEntry vocabularyEntry = entry.getValue();

        PostingList postingList = vocabularyEntry.getPostingList();
        byte[] docIdCompressedArray = postingList.getCompressedDocIdArray();
        try {
            if (!opened){
                throw new IOException();
            }
            long startOffset = docIdsStream.getChannel().position();
            docIdsWriter.write(docIdCompressedArray);
            long endOffset = docIdsStream.getChannel().position();
        } catch (IOException ie){
            ie.printStackTrace();
        }
    }
     */

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
