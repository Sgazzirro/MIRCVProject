package it.unipi.utils;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.model.implementation.PostingListCompressed;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FetcherCompressed implements Fetcher {

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
            Path vocpath = Paths.get(path + Constants.VOCABULARY_FILENAME);
            vocabularySize =(int) Files.size(vocpath)/Constants.VOCABULARY_ENTRY_BYTES_SIZE;
            opened = true;

        } catch (IOException ie) {
            ie.printStackTrace();
            opened = false;
        }
        return opened;
    }

    @Override
    public void loadPosting(PostingList list) {

    }

    private byte[] fetchBytes(FileInputStream stream, long startOffset, long endOffset) throws IOException {
        byte[] bytes = new byte[(int) (endOffset - startOffset)];
        if (opened) {
            stream.getChannel().position(startOffset);

            if (stream.read(bytes) != endOffset - startOffset)
                throw new IOException();

            return bytes;
        }

        throw new IOException("Fetcher not correctly opened");
    }

    public byte[] fetchCompressedDocIds(long startOffset, long endOffset) throws IOException {
        return fetchBytes(docIdsReader, startOffset, endOffset);
    }

    public byte[] fetchCompressedTermFrequencies(long startOffset, long endOffset) throws IOException {
        return fetchBytes(termFreqReader, startOffset, endOffset);
    }

    public PostingListCompressed.ByteBlock fetchDocIdsBlock(byte[] compressedDocIds, int docId, long docIdsBlockOffset) {
        // skips unnecessary lists
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedDocIds);
                DataInputStream dis = new DataInputStream(bais);
        ) {
            if (docIdsBlockOffset != dis.skip(docIdsBlockOffset))
                throw new IOException();
            while (true) {
                // Read integers from the byte array
                int U = dis.readInt();
                int n = dis.readInt();
                docIdsBlockOffset+=(2*Integer.BYTES);
                // computing number of bytes to skip
                int lowHalfLength = (int) Math.ceil(Math.log((float) U / n) / Math.log(2));
                int highHalfLength = (int) Math.ceil(Math.log(U) / Math.log(2)) - lowHalfLength;
                int nTotLowBits = lowHalfLength * n;
                int nTotHighBits = (int) (n + Math.pow(2, highHalfLength));
                int bytesToSkip = (int) Math.ceil((float) nTotLowBits / 8) + (int) Math.ceil((float) nTotHighBits / 8);

                if (U < docId) {
                    // I have to read the next block
                    docIdsBlockOffset += bytesToSkip;
                    if (bytesToSkip != bais.skip(bytesToSkip))
                        throw new IOException();

                } else {
                    int numLowBytes = (int) Math.ceil((float) nTotLowBits/8);
                    int numHighBytes = (int) Math.ceil((float) nTotHighBits/8);

                    byte[] byteArray = new byte[4 + 4 + numLowBytes + numHighBytes];
                    ByteUtils.intToBytes(U, byteArray, 0);
                    ByteUtils.intToBytes(n, byteArray, 4);
                    docIdsBlockOffset += dis.read(byteArray, 8, numHighBytes);
                    docIdsBlockOffset += dis.read(byteArray, 8+numHighBytes, numLowBytes);
                    dis.close();

                    return new PostingListCompressed.ByteBlock(byteArray, docIdsBlockOffset);
                }
            }
        } catch (EOFException e) {
            // end of file reached
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public PostingListCompressed.ByteBlock fetchNextTermFrequenciesBlock(byte[] compressedTermFrequencies, long termFrequenciesBlockOffset) {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedTermFrequencies);
                DataInputStream dis = new DataInputStream(bais);
        ) {
            if (termFrequenciesBlockOffset != dis.skip(termFrequenciesBlockOffset))
                throw new IOException();

            while (true) {
                // Read length of the block
                int length = dis.readInt();

                byte[] byteArray = new byte[4 + length];
                ByteUtils.intToBytes(length, byteArray, 0);
                termFrequenciesBlockOffset += dis.read(byteArray, 4, length);
                termFrequenciesBlockOffset += 4;            // Consider also the first 4 bytes describing the block length
                dis.close();

                return new PostingListCompressed.ByteBlock(byteArray, termFrequenciesBlockOffset);
            }
        } catch (EOFException e) {
            // end of file reached
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public VocabularyEntry loadVocEntry(String term) {
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

        try {
            while (start < end) {
                middle = (end + start) / 2;
                vocabularyReader.getChannel().position((long) middle * Constants.VOCABULARY_ENTRY_BYTES_SIZE);
                vocabularyReader.read(vocabularyEntryBytes, 0, Constants.VOCABULARY_ENTRY_BYTES_SIZE);

                int comparison = truncatedTerm.compareTo(ByteUtils.bytesToString(vocabularyEntryBytes, 0, Constants.BYTES_STORED_STRING));
                if (comparison > 0)         // This means term > entry
                    start = middle;
                else if (comparison < 0)    // This means term < entry
                    end = middle;
                else {                        // This means term = entry
                    VocabularyEntry result = ByteUtils.bytesToVocabularyEntry(vocabularyEntryBytes);
                    result.getPostingList().loadPosting("./test/");
                    return result;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() {
        VocabularyEntry vocabularyEntry;
        String term;
        try{
            if(!opened)
                throw new IOException();

           /* byte [] termByte = new byte[Constants.BYTES_STORED_STRING];
            if(vocabularyReader.read(termByte)!=Constants.BYTES_STORED_STRING) throw new IOException();
            term = ByteUtils.bytesToString(termByte, 0, Constants.BYTES_STORED_STRING);
            */
            byte[] vocabularyEntryBytes = new byte[Constants.VOCABULARY_ENTRY_BYTES_SIZE];
            if (vocabularyReader.read(vocabularyEntryBytes) != Constants.VOCABULARY_ENTRY_BYTES_SIZE)
                return null;
            vocabularyEntry = ByteUtils.bytesToVocabularyEntry(vocabularyEntryBytes);
            vocabularyEntry.getPostingList().loadPosting("./test/");
            term = ByteUtils.bytesToString(vocabularyEntryBytes, 0, Constants.BYTES_STORED_STRING);

        } catch (IOException ie) {
            ie.printStackTrace();
            return null;
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
        byte[] docId = new byte[Integer.BYTES];
        try {
            System.out.println("CIAONE PROPRIO");
            if (!opened) {
                throw new IOException();
            }

            int red = documentIndexReader.read(docId);
            if(red < 0)
                return null;

            byte[] length = new byte[Integer.BYTES];
            documentIndexReader.read(length);

            documentIndexEntry= new DocumentIndexEntry();
            documentIndexEntry.setDocumentLength(ByteUtils.bytesToInt(length));
        } catch(IOException ie){
            return null;
        }
        return Map.entry(ByteUtils.bytesToInt(docId), documentIndexEntry);

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

    public int[] getInformations() {
        byte[] N = new byte[Integer.BYTES];
        byte[] l = new byte[Integer.BYTES];
        try {
            documentIndexReader.read(N);
            documentIndexReader.read(l);

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new int[]{ByteUtils.bytesToInt(N), ByteUtils.bytesToInt(l)};
    }
}
