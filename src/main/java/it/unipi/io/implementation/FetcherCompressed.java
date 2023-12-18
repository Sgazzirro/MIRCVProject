package it.unipi.io.implementation;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.PostingListCompressedImpl;
import it.unipi.encoding.CompressionType;

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


    @Override
    public void loadPosting(PostingList postingList) {
        if (!(postingList instanceof PostingListCompressedImpl pList))
            throw new RuntimeException("Unsupported operation");

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