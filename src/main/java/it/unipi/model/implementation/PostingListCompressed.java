package it.unipi.model.implementation;

import it.unipi.model.Encoder;
import it.unipi.model.PostingList;
import it.unipi.utils.ByteUtils;
import it.unipi.utils.EliasFanoStruct;
import it.unipi.utils.FetcherCompressed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PostingListCompressed extends PostingList {

    public static class ByteBlock {

        private byte[] bytes;
        private long offset;

        public ByteBlock(byte[] bytes, long offset) {
            this.bytes = bytes;
            this.offset = offset;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public long getOffset() {
            return offset;
        }
    }

    private byte[] compressedDocIds;
    private byte[] compressedTermFrequencies;

    private List<Integer> docIdsBlockList;           // ELIAS FANO    - DOC IDS
    private List<Integer> termFrequenciesBlockList;  // SIMPLE9/UNARY - TERM FREQUENCIES
    private int blockPointer;
    private long docIdsBlockPointer;                    // This represents the offset of the next docIdsBlock
    private long termFrequenciesBlockPointer;           // This represents the index of the actual block of term frequencies

    private final Encoder docIdsEncoder = new EliasFano();
    private final Encoder termFrequenciesEncoder = new Simple9(true);
    FetcherCompressed fetcher = new FetcherCompressed();

    public PostingListCompressed(long docIdsOffset, long termFreqOffset, int docIdsLength, int termFreqLength, double idf) {
        super(docIdsOffset, termFreqOffset, docIdsLength, termFreqLength, idf);
        docIdsBlockList = new ArrayList<>();
        termFrequenciesBlockList = new ArrayList<>();
        
        // Load the blocks corresponding to these offset provided to the constructor
        String path = "data/";
        fetcher.start(path);
        loadPosting();
    }

    public PostingListCompressed() {
        docIdsBlockList = new ArrayList<>();
        termFrequenciesBlockList = new ArrayList<>();
    }

    @Override
    public int docId() {
        return docIdsBlockList.get(blockPointer);
    }

    @Override
    public double score() {
        // TODO - To implement
        return 0;
    }

    @Override
    public boolean hasNext() {
        long docIdsEndOffset = getDocIdsOffset() + getDocIdsLength();
        return (blockPointer + 1 < docIdsBlockList.size()) || (docIdsBlockPointer < docIdsEndOffset);
    }

    @Override
    public void next() {
        if (blockPointer + 1 < docIdsBlockList.size())
            blockPointer++;
        else
            loadNextBlock();
    }

    @Override
    public void nextGEQ(int docId) {
        while (true) {
            // If we are in the correct block advance the pointer to the right place
            if (docIdsBlockList.get(docIdsBlockList.size() - 1) > docId) {
                for (int i = blockPointer; i < docIdsBlockList.size(); i++) {
                    if (docIdsBlockList.get(i) >= docId) {
                        blockPointer = i;
                        return;
                    }
                }
            }

            // Else get the next block
            loadNextBlock();
        }
    }

    @Override
    public boolean addPosting(int docId, int termFreq) {
        if (docIdsBlockList.isEmpty() || docIdsBlockList.get(docIdsBlockList.size()-1)!=docId){
            docIdsBlockList.add(docId);
            termFrequenciesBlockList.add(termFreq);
            return true;
        }

        // se il docId è già presente come ultima entry, aumenta solo la term freq
        termFrequenciesBlockList.set(termFrequenciesBlockList.size() - 1, termFrequenciesBlockList.get(termFrequenciesBlockList.size() - 1) + termFreq);
        return false;
    }

    @Override
    public boolean loadPosting(String docIdsFilename, String termFreqFilename) {
        try {
            compressedDocIds = fetcher.fetchCompressedDocIds(getDocIdsOffset(), getDocIdsOffset() + getDocIdsLength());
            compressedTermFrequencies = fetcher.fetchCompressedTermFrequencies(getTermFreqOffset(), getTermFreqOffset() + getTermFreqLength());

            docIdsBlockPointer = termFrequenciesBlockPointer = 0;
            loadNextBlock();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadNextBlock() {
        ByteBlock docIdsBlock = fetcher.fetchDocIdsBlock(compressedDocIds, 0, docIdsBlockPointer);  // Read the first block
        docIdsBlockPointer = docIdsBlock.getOffset();
        docIdsBlockList = docIdsEncoder.decode(docIdsBlock.getBytes());

        ByteBlock termFrequenciesBlock = fetcher.fetchNextTermFrequenciesBlock(compressedTermFrequencies, termFrequenciesBlockPointer);
        termFrequenciesBlockPointer = termFrequenciesBlock.getOffset();
        termFrequenciesBlockList = termFrequenciesEncoder.decode(termFrequenciesBlock.getBytes());

        blockPointer = 0;
    }

    /*      Abbiamo detto che verranno mergiate solo postingLists non compresse
    @Override
    public int mergePosting(PostingList toMerge) {
        if (!(toMerge instanceof PostingListImpl))
            throw new RuntimeException("Cannot merge PostingLists with different implementations");

        PostingListCompressed other = (PostingListCompressed) toMerge;

        // chiamato solo in fase di indexing
        length += other.getLength();
        // devo concatenare i 2 array di bytes
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(compressedDocIds);
            outputStream.write(other.compressedDocIds);
            compressedDocIds = outputStream.toByteArray();
            return compressedDocIds.length;
        } catch (IOException e){
            System.err.println("Error in mergePosting");
            e.printStackTrace();
        }
        return 0;
    }
     */

}
