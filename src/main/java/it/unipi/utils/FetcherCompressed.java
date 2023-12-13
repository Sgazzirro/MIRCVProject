package it.unipi.utils;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.PostingListCompressed;

import java.io.*;

public class FetcherCompressed extends FetcherBinary {

    public FetcherCompressed() {
        compression = CompressionType.COMPRESSED;
    }

    private byte[] fetchBytes(FileInputStream stream, long startOffset, int length) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        byte[] bytes = new byte[length];
        stream.getChannel().position(startOffset);

        if (stream.read(bytes) != length)
            throw new IOException("Could not fetch posting list");

        return bytes;
    }

    public byte[] fetchCompressedDocIds(long startOffset, int length) throws IOException {
        return fetchBytes(docIdsReader, startOffset, length);
    }

    public byte[] fetchCompressedTermFrequencies(long startOffset, int length) throws IOException {
        return fetchBytes(termFreqReader, startOffset, length);
    }

    public PostingListCompressed.ByteBlock fetchDocIdsBlock(byte[] compressedDocIds, int docId, long docIdsBlockOffset) {
        // skips unnecessary lists
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedDocIds);
                DataInputStream dis = new DataInputStream(bais)
        ) {
            if (docIdsBlockOffset != dis.skip(docIdsBlockOffset))
                throw new IOException();
            while (true) {
                // Read integers from the byte array
                int U = dis.readInt();
                U = (U == 0) ? 1 : U;
                int n = dis.readInt();
                docIdsBlockOffset += (2*Integer.BYTES);
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
                    if (numHighBytes!=0) docIdsBlockOffset += dis.read(byteArray, 8, numHighBytes);
                    if (numLowBytes!=0) docIdsBlockOffset += dis.read(byteArray, 8+numHighBytes, numLowBytes);
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
                DataInputStream dis = new DataInputStream(bais)
        ) {
            if (termFrequenciesBlockOffset != dis.skip(termFrequenciesBlockOffset))
                throw new IOException();

            // Read length of the block
            int length = dis.readInt();

            byte[] byteArray = new byte[4 + length];
            ByteUtils.intToBytes(length, byteArray, 0);
            termFrequenciesBlockOffset += dis.read(byteArray, 4, length);
            termFrequenciesBlockOffset += 4;            // Consider also the first 4 bytes describing the block length
            dis.close();

            return new PostingListCompressed.ByteBlock(byteArray, termFrequenciesBlockOffset);

        } catch (EOFException e) {
            // end of file reached
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void loadPosting(PostingList postingList) {
        if (!(postingList instanceof PostingListCompressed))
            throw new RuntimeException("Unsupported operation");

        PostingListCompressed pList = (PostingListCompressed) postingList;

        try {
            pList.setCompressedDocIds(
                    fetchCompressedDocIds(postingList.getDocIdsOffset(), postingList.getDocIdsLength())
            );
            pList.setCompressedTermFrequencies(
                    fetchCompressedTermFrequencies(postingList.getTermFreqOffset(), postingList.getTermFreqLength())
            );

            pList.loadNextBlock();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}