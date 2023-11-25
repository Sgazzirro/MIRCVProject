package it.unipi.utils;

import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.PostingListImpl;

import javax.xml.crypto.Data;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class DumperBinary implements Dumper{
    Boolean opened = false;

    // vocabulary writer
    private FileOutputStream vocabularyStream;
    private DataOutputStream vocabularyWriter;
    // Doc Ids writer
    private FileOutputStream docIdsStream;
    private DataOutputStream docIdsWriter;
    // Term frequencies writer
    private FileOutputStream termFreqStream;
    private DataOutputStream termFreqWriter;

    // documentIndexWriter
    private FileOutputStream documentIndexStream;
    private DataOutputStream documentIndexWriter;
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
            documentIndexStream = new FileOutputStream(path + Constants.DOCUMENT_INDEX_FILENAME, true);
            documentIndexWriter = new DataOutputStream(documentIndexStream);
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
        Double upperBound = vocabularyEntry.getUpperBound();
        PostingList pList = vocabularyEntry.getPostingList();
        if(!(pList instanceof PostingListImpl))
            throw new RuntimeException("Cannot dump a compressed posting list, it must be full in memory");

        PostingListImpl postingList = (PostingListImpl) pList;
        double idf = postingList.getIdf();
        List<Integer> docIdList = postingList.getDocIds();
        List<Integer> termFrequencyList = postingList.getTermFrequencies();

        // dump docIds
        for(Integer docId: docIdList){
            docIdsWriter.writeInt(docId);
        }
        int docIdsLength = docIdList.size()*Integer.BYTES;

        // dump term frequencies
        for(Integer termFreq: termFrequencyList){
            termFreqWriter.writeInt(termFreq);
        }
        int termFreqLength = termFrequencyList.size()*Integer.BYTES;

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

    @Override
    public void dumpDocumentIndex(DocumentIndex docIndex) {
        try{
            documentIndexWriter.writeInt(docIndex.getTotalLength());
            documentIndexWriter.writeInt(docIndex.getNumDocuments());
        } catch (IOException ie){
            ie.printStackTrace();
        }
        for (Map.Entry<Integer, DocumentIndexEntry> entry : docIndex.getEntries()) {

            dumpDocumentIndexEntry(entry);
        }
    }

    @Override
    public void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry) {
        int docId = entry.getKey();
        DocumentIndexEntry documentIndexEntry = entry.getValue();
        try{
            documentIndexWriter.writeInt(docId);
            documentIndexWriter.writeInt(documentIndexEntry.getDocumentLength());
        } catch (IOException ie){
            ie.printStackTrace();
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
                vocabularyStream.close();
                vocabularyWriter.close();
                docIdsStream.close();
                docIdsWriter.close();
                termFreqStream.close();
                termFreqWriter.close();
                documentIndexStream.close();
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
