package it.unipi.utils;

import it.unipi.model.DocumentIndex;
import it.unipi.model.Encoder;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.*;
import it.unipi.model.VocabularyEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class DumperCompressed implements Dumper {

    // Vocabulary writer
    private FileOutputStream vocabularyStream;
    private DataOutputStream vocabularyWriter;
    // Doc Ids writer
    private FileOutputStream docIdsStream;
    private DataOutputStream docIdsWriter;
    // Term frequencies writer
    private FileOutputStream termFreqStream;
    private DataOutputStream termFreqWriter;
    // Document index writer
    // private FileOutputStream documentIndexStream;
    // private DataOutputStream documentIndexWriter;

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

            vocabularyStream = new FileOutputStream(path + Constants.VOCABULARY_FILENAME, true);
            vocabularyWriter = new DataOutputStream(vocabularyStream);
            docIdsStream = new FileOutputStream(path + Constants.DOC_IDS_POSTING_FILENAME, true);
            docIdsWriter = new DataOutputStream(docIdsStream);
            termFreqStream = new FileOutputStream(path + Constants.TF_POSTING_FILENAME, true);
            termFreqWriter = new DataOutputStream(termFreqStream);
            // documentIndexStream = new FileOutputStream(path + Constants.DOCUMENT_INDEX_FILENAME, true);
            // documentIndexWriter = new DataOutputStream(documentIndexStream);
            opened = true;
            docIdsOffset = termFreqOffset = 0;

        } catch(IOException ie) {
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
        double upperBound = vocabularyEntry.getUpperBound();

        PostingList pList = vocabularyEntry.getPostingList();
        if (!(pList instanceof PostingListImpl))
            throw new RuntimeException("Cannot dump a compressed posting list, it must be full in memory");

        PostingListImpl postingList = (PostingListImpl) pList;
        double idf = postingList.getIdf();
        List<Integer> docIdList = postingList.getDocIds();
        List<Integer> tfList = postingList.getTermFrequencies();

        int docIdsLength = 0, termFreqLength = 0;
        if (!opened)
            throw new IOException();

        // Dump doc ids
        int blockSize = Constants.BLOCK_SIZE;
        for (int i = 0; i < docIdList.size(); i += blockSize) {
            List<Integer> blockDocIdList = docIdList.subList(i, Math.min(docIdList.size(), i + blockSize));
            byte[] byteList = docIdsEncoder.encode(blockDocIdList);

            docIdsWriter.write(byteList);
            docIdsLength += byteList.length;
        }

        // Dump term frequencies
        for (int i = 0; i < docIdList.size(); i += blockSize) {
            List<Integer> blockTFList = tfList.subList(i, Math.min(docIdList.size(), i + blockSize));
            byte[] byteList = tfEncoder.encode(blockTFList);

            termFreqWriter.write(byteList);
            termFreqLength += byteList.length;
        }

        // Dump vocabulary entry
        byte[] stringBytes = term.getBytes(StandardCharsets.UTF_8);
        byte[] stringTruncatedBytes = new byte[Constants.BYTES_STORED_STRING]; // Truncate string to store a fixed number of bytes
        System.arraycopy(stringBytes, 0, stringTruncatedBytes, 0, Math.min(stringBytes.length, Constants.BYTES_STORED_STRING));
        for (int i = Math.min(stringBytes.length, Constants.BYTES_STORED_STRING); i < Constants.BYTES_STORED_STRING; i++)
            stringTruncatedBytes[i] = '\0';
        vocabularyWriter.write(stringTruncatedBytes);
        vocabularyWriter.writeInt(documentFrequency);
        vocabularyWriter.writeDouble(upperBound);
        vocabularyWriter.writeDouble(idf);
        vocabularyWriter.writeLong(docIdsOffset);
        vocabularyWriter.writeInt(docIdsLength);
        vocabularyWriter.writeLong(termFreqOffset);
        vocabularyWriter.writeInt(termFreqLength);

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

    }

    @Override
    public void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry) {

    }

    @Override
    public void dumpVocabulary(Vocabulary vocabulary) {
        try {
            // Keep space at the beginning of file to store the number of entries
            vocabularyWriter.writeInt(0);
            int numEntries = 0;
            for (Map.Entry<String, VocabularyEntry> entry : vocabulary.getEntries()) {
                dumpVocabularyEntry(entry);
                numEntries++;
            }
            // Set position to start and write the number of entries
            vocabularyStream.getChannel().position(0);
            vocabularyWriter.writeInt(numEntries);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean end() {
        try {
            if (opened) {
                vocabularyStream.close();
                vocabularyWriter.close();
                docIdsStream.close();
                docIdsWriter.close();
                termFreqStream.close();
                termFreqWriter.close();
                opened = false;
            } else
                throw new IOException();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return !opened;
    }
}