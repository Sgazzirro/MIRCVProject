package it.unipi.io.implementation;

import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.PostingListCompressedImpl;
import it.unipi.encoding.CompressionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FetcherCompressed extends FetcherBinary {

    private static final Logger logger = LoggerFactory.getLogger(FetcherCompressed.class);

    public FetcherCompressed() {
        compression = CompressionType.COMPRESSED;
    }

    private byte[] fetchBytes(FileInputStream stream, int length) throws IOException {
        if (!opened)
            throw new IOException("Fetcher has not been started");

        byte[] bytes = new byte[length];
        if (stream.read(bytes) != length)
            throw new IOException("Could not fetch posting list");

        return bytes;
    }

    @Override
    protected void loadNextPosting(VocabularyEntry entry) throws IOException {
        if (!(entry.getPostingList() instanceof PostingListCompressedImpl postingList)) {
            logger.error("Cannot fetch uncompressed list");
            throw new UnsupportedOperationException("Load of uncompressed list");
        }

        postingList.setCompressedDocIds(
                fetchBytes(docIdsReader, entry.getDocIdsLength())
        );
        postingList.setCompressedTermFrequencies(
                fetchBytes(termFreqReader, entry.getTermFreqLength())
        );

        postingList.loadFirstBlock();
    }
}