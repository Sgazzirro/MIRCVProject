package it.unipi.utils;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.EliasFano;
import it.unipi.model.implementation.VocabularyEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class FetcherCompressed implements Fetcher{
    FileInputStream fisDocIds;
    boolean opened = false;

    @Override
    public boolean start(String filename) {
        try{
            fisDocIds = new FileInputStream(filename);

            opened = true;
            return true;
        }
        catch (IOException ie){
            ie.printStackTrace();
            opened = false;
        }
        return false;
    }

    @Override
    public void setScope(String filename) {

    }

    @Override
    public void loadPosting(PostingList list) {
        int docId = 0;
        long byteOffsetStart = 0;
        long byteOffsetEnd = 0;
        String filename="";

        start(filename);
        /*
        byte [] docIdArrayCompressed = fetchDocIdListCompressed(byteOffsetStart, byteOffsetEnd);
        // TO PUT IN CACHE!!!!
        EliasFanoStruct efs = fetchDocIdSubList(docIdArrayCompressed, docId);
        ArrayList<Integer> docIdsDecodedList = EliasFano.decode(efs);
        for (int i=0; i<docIdsDecodedList.size(); i++){
            list.addPosting(docIdsDecodedList.get(i), 0);
        }

        end();

         */
    }

    public byte[] fetchDocIdListCompressed(long byteOffsetStart, long byteOffsetEnd) {
        byte[] docIdArrayCompressed = new byte[(int) (byteOffsetEnd-byteOffsetStart)];
        try{
            if (opened){
                if (fisDocIds.skip(byteOffsetStart)!=byteOffsetStart){
                    throw new IOException();
                }
                if(fisDocIds.read(docIdArrayCompressed)!=byteOffsetEnd-byteOffsetStart){
                    throw new IOException();
                }
                return docIdArrayCompressed;
            }
        } catch (IOException ie){
            ie.printStackTrace();
            return null;
        }
        return null;
    }

    public byte[] fetchDocIdSubList(byte[] compressedDocIds, int docId, Long readOffsetEF){
        // skips unnecessary lists
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedDocIds);
                DataInputStream dis = new DataInputStream(bais);
        ) {
            if(readOffsetEF!=dis.skip(readOffsetEF)){
                throw new IOException();
            }
            while(true) {
                // Read integers from the byte array
                int U = dis.readInt();
                int n = dis.readInt();

                // computing number of bytes to skip
                int lowHalfLength = (int) Math.ceil(Math.log((float) U / n) / Math.log(2));
                int highHalfLength = (int) Math.ceil(Math.log(U) / Math.log(2)) - lowHalfLength;
                int nTotLowBits = lowHalfLength * n;
                int nTotHighBits = (int) (n + Math.pow(2, highHalfLength));
                int bytesToSkip = (int) Math.ceil((float) nTotLowBits / 8) + (int) Math.ceil((float) nTotHighBits / 8);

                if (U<docId) {
                    // I have to read the next block
                    readOffsetEF += bytesToSkip;
                    if (bytesToSkip != bais.skip(bytesToSkip)) {
                        throw new IOException();
                    }
                }
                else {
                    int numLowBytes = (int) Math.ceil((float) nTotLowBits/8);
                    int numHighBytes = (int) Math.ceil((float) nTotHighBits/8);

                    byte[] byteArray = new byte[4 + 4 + numLowBytes + numHighBytes];
                    ByteUtils.intToBytes(U, byteArray, 0);
                    ByteUtils.intToBytes(n, byteArray, 4);
                    readOffsetEF += dis.read(byteArray, 8, numHighBytes);
                    readOffsetEF += dis.read(byteArray, 8+numHighBytes, numLowBytes);
                    dis.close();

                    return byteArray;
                }
            }
        } catch (EOFException e){
            // end of file reached
            return null;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry(String term) {
        return null;
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() {
        return null;
    }

    @Override
    public DocumentIndexEntry loadDocEntry(long docId) {
        return null;
    }

    @Override
    public Map.Entry<Integer, DocumentIndexEntry> loadDocEntry() {
        return null;
    }

    @Override
    public boolean end() {
        try {
            if(opened){
                fisDocIds.close();
                opened = false;
                return true;
            } else throw new IOException();
        } catch (IOException ie){
            ie.printStackTrace();
            return false;
        }
    }
}
