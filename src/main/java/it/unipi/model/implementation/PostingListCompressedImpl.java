package it.unipi.model.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.encoding.IntegerListBlock;
import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class PostingListCompressedImpl extends PostingList {

    private byte[] compressedDocIds;
    private byte[] compressedTermFrequencies;

    private List<Integer> docIdsDecompressedList;           // ELIAS FANO    - DOC IDS
    private List<Integer> termFrequenciesDecompressedList;  // SIMPLE9/UNARY - TERM FREQUENCIES
    private int blockPointer;
    private int docIdsBlockPointer;                    // This represents the offset of the next docIdsBlock
    private int termFrequenciesBlockPointer;           // This represents the index of the actual block of term frequencies

    private static final Encoder docIdsEncoder = Encoder.getDocIdsEncoder();
    private static final Encoder termFreqEncoder = Encoder.getTermFrequenciesEncoder();

    public PostingListCompressedImpl(VocabularyEntry entry) {
        super(entry);

        docIdsDecompressedList = new ArrayList<>();
        termFrequenciesDecompressedList = new ArrayList<>();

        docIdsBlockPointer = termFrequenciesBlockPointer = 0;
        blockPointer = -1;
    }

    @Override
    public int docId() {
        return docIdsDecompressedList.get(blockPointer);
    }

    @Override
    public int termFrequency() {
        return termFrequenciesDecompressedList.get(blockPointer);
    }

    @Override
    public boolean hasNext() {
        long docIdsEndOffset = rootEntry.getDocIdsLength();
        return (blockPointer + 1 < docIdsDecompressedList.size()) || (docIdsBlockPointer < docIdsEndOffset);
    }

    @Override
    public void next() {
        long docIdsLength = rootEntry.getDocIdsLength();
        if (docIdsBlockPointer == docIdsLength &&
                blockPointer == docIdsDecompressedList.size() - 1)
            throw new NoSuchElementException();

        if (blockPointer + 1 < docIdsDecompressedList.size())
            blockPointer++;
        else {
            IntegerListBlock docIdsBlock = docIdsEncoder.getNextBlock(compressedDocIds, docIdsBlockPointer),
                termFreqBlock = termFreqEncoder.getNextBlock(compressedTermFrequencies, termFrequenciesBlockPointer);

            decompressBlock(docIdsBlock, termFreqBlock);
            blockPointer = 0;
        }
    }

    @Override
    public void nextGEQ(int docId) {
        // If we are in the correct block advance the pointer to the right place
        if (docIdsDecompressedList.get(docIdsDecompressedList.size() - 1) >= docId) {
            for (int i = (blockPointer!=-1) ? blockPointer : 0; i < docIdsDecompressedList.size(); i++) {
                if (docIdsDecompressedList.get(i) >= docId) {
                    blockPointer = i;
                    return;
                }
            }
        }

        // if we're in the last block, and it doesn't contain the docId
        long docIdsLength = rootEntry.getDocIdsLength();
        if (docIdsBlockPointer == docIdsLength &&
                docIdsDecompressedList.get(docIdsDecompressedList.size() - 1) < docId)
            throw new NoSuchElementException();

        // Else get the block containing the right docId
        IntegerListBlock docIdsBlock, termFreqBlock;
        while (true) {
            docIdsBlock = docIdsEncoder.getNextBlock(compressedDocIds, docIdsBlockPointer);
            termFreqBlock = termFreqEncoder.getNextBlock(compressedTermFrequencies, termFrequenciesBlockPointer);

            // If this is the last block, or we are at the correct block, exit the loop
            if (docIdsBlockPointer + docIdsBlock.length() == docIdsLength)
                break;
            if (docIdsBlock.upperbound() < docId)
                break;

            docIdsBlockPointer += docIdsBlock.length();
            termFrequenciesBlockPointer += termFreqBlock.length();
        }

        decompressBlock(docIdsBlock, termFreqBlock);
        blockPointer = 0;

        // Once the block has been decompressed, call again the nextGEQ to advance the pointer to the correct posting
        nextGEQ(docId);
    }

    @Override
    public void reset() {
        docIdsBlockPointer = termFrequenciesBlockPointer = 0;
        blockPointer = -1;

        loadFirstBlock();
    }

    @Override
    public boolean addPosting(int docId, int termFreq) {
        if (docIdsDecompressedList.isEmpty() || docIdsDecompressedList.get(docIdsDecompressedList.size()-1) != docId){
            docIdsDecompressedList.add(docId);
            termFrequenciesDecompressedList.add(termFreq);
            return true;
        }

        // if docId is already present in the postingList (as last entry), increase the termFreq
        termFrequenciesDecompressedList.set(termFrequenciesDecompressedList.size() - 1, termFrequenciesDecompressedList.get(termFrequenciesDecompressedList.size() - 1) + termFreq);
        return false;
    }

    public void loadFirstBlock() {
        IntegerListBlock docIdsBlock = docIdsEncoder.getNextBlock(compressedDocIds, 0);
        IntegerListBlock termFreqBlock = termFreqEncoder.getNextBlock(compressedTermFrequencies, 0);

        decompressBlock(docIdsBlock, termFreqBlock);
    }

    private void decompressBlock(IntegerListBlock docIdsBlock, IntegerListBlock termFreqBlock) {
        docIdsDecompressedList = docIdsEncoder.decode(
                ByteUtils.subArray(compressedDocIds, docIdsBlockPointer, docIdsBlock.length())
        );
        docIdsBlockPointer += docIdsBlock.length();

        termFrequenciesDecompressedList = termFreqEncoder.decode(
                ByteUtils.subArray(compressedTermFrequencies, termFrequenciesBlockPointer, termFreqBlock.length())
        );
        termFrequenciesBlockPointer += termFreqBlock.length();

        // Remove fictitious 0 frequencies
        int first0Index = docIdsDecompressedList.size();
        termFrequenciesDecompressedList.subList(
                first0Index,
                termFrequenciesDecompressedList.size()
        ).clear();
    }

    @Override
    public List<Integer> getTermFrequenciesList() {
        return termFrequenciesDecompressedList;
    }

    @Override
    public List<Integer> getDocIdsList() {
        return docIdsDecompressedList;
    }

    public void setCompressedDocIds(byte[] compressedDocIds) {
        this.compressedDocIds = compressedDocIds;
    }

    public void setCompressedTermFrequencies(byte[] compressedTermFrequencies) {
        this.compressedTermFrequencies = compressedTermFrequencies;
    }

    public String toString() {
        return "DocIdList: " + docIdsDecompressedList + " TermFrequencyList: " + termFrequenciesDecompressedList;
    }

}
