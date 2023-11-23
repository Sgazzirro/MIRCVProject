package it.unipi.utils;

import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.PostingListCompressed;
import it.unipi.model.implementation.PostingListImpl;

import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class FetcherBinary implements Fetcher{
    FileInputStream vocabularyReader;
    FileInputStream docIdsReader;
    FileInputStream termFreqReader;
    FileInputStream documentIndexReader;
    boolean opened = false;

    private int vocabularySize;
    @Override
    public boolean start(String path) {
        try {
            vocabularyReader = new FileInputStream(path + Constants.VOCABULARY_FILENAME);
            docIdsReader = new FileInputStream(path + Constants.DOC_IDS_POSTING_FILENAME);
            termFreqReader = new FileInputStream(path + Constants.TF_POSTING_FILENAME);
            documentIndexReader = new FileInputStream(path + Constants.DOCUMENT_INDEX_FILENAME);

            // Read number of entries of the vocabulary
            vocabularySize = ByteUtils.bytesToInt(vocabularyReader.readNBytes(Integer.BYTES));
            opened = true;

        } catch (IOException ie) {
            ie.printStackTrace();
            opened = false;
        }
        return opened;
    }



    @Override
    public void loadPosting(PostingList list){
        // questo secondo me non va usato, dobbiamo implementare l'interfaccia con quello sotto.
        // termFreqLength non la uso ma sicuro nella compressa servirà
    }
    public void loadPosting(PostingList list, long docIdsOffset, int docIdsLength, long termFreqOffset, int termFreqLength) {
        try {
            DataInputStream disDocId = new DataInputStream(docIdsReader);
            if(disDocId.skip(docIdsOffset)!=docIdsOffset){
                throw new IOException();
            }

            DataInputStream disTermFreq = new DataInputStream(termFreqReader);
            if(disTermFreq.skip(termFreqOffset)!=termFreqOffset){
                throw new IOException();
            }

            // load docIds and termFreq
            for (int len = 0; len < docIdsLength; len += Integer.BYTES) {
                int docId = disDocId.readInt();
                int termFreq = disTermFreq.readInt();
                list.addPosting(docId, termFreq);
            }
            disDocId.close();
            disTermFreq.close();
        } catch (IOException ie){
            ie.printStackTrace();
        }
    }

    @Override
    public VocabularyEntry loadVocEntry(String term) {
        try{
            if(!opened){
                throw new IOException();
            }
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

            while (start < end) {
                middle = (end + start) / 2;
                vocabularyReader.getChannel().position(Integer.BYTES + (long) middle * Constants.VOCABULARY_ENTRY_BYTES_SIZE);
                vocabularyReader.read(vocabularyEntryBytes, 0, Constants.VOCABULARY_ENTRY_BYTES_SIZE);

                int comparison = truncatedTerm.compareTo(ByteUtils.bytesToString(vocabularyEntryBytes, 0, Constants.BYTES_STORED_STRING));
                if (comparison > 0)         // This means term > entry
                    start = middle;
                else if (comparison < 0)    // This means term < entry
                    end = middle;
                else                        // This means term = entry
                    return bytesToVocabularyEntry(vocabularyEntryBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private VocabularyEntry bytesToVocabularyEntry(byte[] byteList) {
        ByteBuffer bytes = ByteBuffer.wrap(byteList);
        bytes.position(Constants.BYTES_STORED_STRING);
        int documentFrequency = bytes.getInt();
        double upperBound = bytes.getDouble();
        double idf = bytes.getDouble();
        long docIdsOffset = bytes.getLong();
        int docIdsLength = bytes.getInt();
        long termFreqOffset = bytes.getLong();
        int termFreqLength = bytes.getInt();

        VocabularyEntry entry = new VocabularyEntry();
        entry.setDocumentFrequency(documentFrequency);
        entry.setUpperBound(upperBound);

        PostingList postingList = new PostingListImpl();
        loadPosting(postingList, docIdsOffset, docIdsLength, termFreqOffset, termFreqLength);
        return entry;
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() {
        VocabularyEntry vocabularyEntry = null;
        String term = null;
        try{
            if(!opened){
                throw new IOException();
            }
            // remember the integer at the start
            if(vocabularyReader.getChannel().position()==0){
                vocabularyReader.skip(Integer.BYTES);
            }
            byte [] termByte = new byte[Constants.BYTES_STORED_STRING];
            if(vocabularyReader.read(termByte)!=Constants.BYTES_STORED_STRING) throw new IOException();
            term = ByteUtils.bytesToString(termByte, 0, Constants.BYTES_STORED_STRING);

            byte[] vocabularyEntryBytes = new byte[Constants.VOCABULARY_ENTRY_BYTES_SIZE];
            if(vocabularyReader.read(vocabularyEntryBytes)!=Constants.VOCABULARY_ENTRY_BYTES_SIZE) throw new IOException();
            vocabularyEntry = bytesToVocabularyEntry(vocabularyEntryBytes);
        } catch (IOException ie){
            ie.printStackTrace();
        }
        return Map.entry(term, vocabularyEntry);
    }

    @Override
    public DocumentIndexEntry loadDocEntry(long docId) {
        return null;
    }

    @Override
    public Map.Entry<Integer, DocumentIndexEntry> loadDocEntry() {
        DocumentIndexEntry documentIndexEntry = null;
        int docId = 0;
        try {
            if (!opened) {
                throw new IOException();
            }
            // remember the 2 integers at the start
            if(documentIndexReader.getChannel().position()==0){
                documentIndexReader.skip(2*Integer.BYTES);
            }
            DataInputStream disDocIndex = new DataInputStream(documentIndexReader);

            docId = disDocIndex.readInt();
            int length = disDocIndex.readInt();
            documentIndexEntry= new DocumentIndexEntry();
            documentIndexEntry.setDocumentLength(length);
        } catch(IOException ie){
            ie.printStackTrace();
        }
        return Map.entry(docId, documentIndexEntry);
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
}
