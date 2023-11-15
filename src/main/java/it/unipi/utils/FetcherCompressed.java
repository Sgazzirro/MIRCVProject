package it.unipi.utils;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.VocabularyEntry;

import java.io.*;
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

    }

    // questa non posso implementarla, intanto la aggiungo qua
    public byte[] fetchDocIdListCompressed(String filename, long byteOffsetStart, long byteOffsetEnd){
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

    public EliasFanoStruct fetchDocIdSubList(byte[] compressedDocIds, int docId){
        // skips unnecessary lists
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedDocIds);
                DataInputStream dis = new DataInputStream(bais);
        ) {
            while(true){
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
                    // i have to read the next block
                    if (bytesToSkip != bais.skip(bytesToSkip)) {
                        throw new IOException();
                    }
                }
                else{
                    byte [] lowBytes = new byte[(int) Math.ceil((float) nTotLowBits/8)];
                    byte [] highBytes = new byte[(int) Math.ceil((float) nTotHighBits/8)];
                    dis.read(highBytes);
                    dis.read(lowBytes);
                    return new EliasFanoStruct(U, n, lowBytes, highBytes);
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
