package it.unipi.utils;

import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.PostingListCompressed;
import it.unipi.model.implementation.PostingListImpl;

import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            Path vocpath = Paths.get(path + Constants.VOCABULARY_FILENAME);
            vocabularySize =(int) Files.size(vocpath)/Constants.VOCABULARY_ENTRY_BYTES_SIZE;
            vocabularyReader = new FileInputStream(path + Constants.VOCABULARY_FILENAME);
            docIdsReader = new FileInputStream(path + Constants.DOC_IDS_POSTING_FILENAME);
            termFreqReader = new FileInputStream(path + Constants.TF_POSTING_FILENAME);
            documentIndexReader = new FileInputStream(path + Constants.DOCUMENT_INDEX_FILENAME);
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
            docIdsReader.getChannel().position(docIdsOffset);
            DataInputStream disDocId = new DataInputStream(docIdsReader);
            /*System.out.println("OFFSET" + docIdsOffset);
            if(disDocId.skip(docIdsOffset)!=docIdsOffset){
                throw new IOException();
            }*/

            termFreqReader.getChannel().position(termFreqOffset);
            DataInputStream disTermFreq = new DataInputStream(termFreqReader);
            /* if(disTermFreq.skip(termFreqOffset)!=termFreqOffset){
                throw new IOException();
            }*/

            // load docIds and termFreq

            for (int len = 0; len < docIdsLength; len += Integer.BYTES) {
                int docId = disDocId.readInt();
                int termFreq = disTermFreq.readInt();

                list.addPosting(docId, termFreq);
            }
            //disDocId.close();
            //disTermFreq.close();
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
                vocabularyReader.getChannel().position((long) middle * Constants.VOCABULARY_ENTRY_BYTES_SIZE);
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
        entry.setPostingList(postingList);
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
           /* byte [] termByte = new byte[Constants.BYTES_STORED_STRING];
            if(vocabularyReader.read(termByte)!=Constants.BYTES_STORED_STRING) throw new IOException();
            term = ByteUtils.bytesToString(termByte, 0, Constants.BYTES_STORED_STRING);
            */
            byte[] vocabularyEntryBytes = new byte[Constants.VOCABULARY_ENTRY_BYTES_SIZE];
            if(vocabularyReader.read(vocabularyEntryBytes)!=Constants.VOCABULARY_ENTRY_BYTES_SIZE) return null;
            vocabularyEntry = bytesToVocabularyEntry(vocabularyEntryBytes);
            term = ByteUtils.bytesToString(vocabularyEntryBytes, 0, Constants.BYTES_STORED_STRING);
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

            DataInputStream disDocIndex = new DataInputStream(documentIndexReader);

            docId = disDocIndex.readInt();
            int length = disDocIndex.readInt();
            documentIndexEntry= new DocumentIndexEntry();
            documentIndexEntry.setDocumentLength(length);
        } catch(IOException ie){
            return null;
        }
        return Map.entry(docId, documentIndexEntry);
    }

    @Override
    public int[] getInformations() {
        int N;
        int l;
        try {
            DataInputStream disDocIndex = new DataInputStream(documentIndexReader);
            l = disDocIndex.readInt();
            N = disDocIndex.readInt();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new int[]{N, l};
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
