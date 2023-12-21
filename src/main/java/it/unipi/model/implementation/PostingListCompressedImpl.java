package it.unipi.model.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.encoding.implementation.EliasFano;
import it.unipi.encoding.implementation.Simple9;
import it.unipi.model.Posting;
import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class PostingListCompressedImpl extends PostingList {

    private byte[] compressedDocIds;
    private byte[] compressedTermFrequencies;

    private List<Integer> docIdsDecompressedList;           // ELIAS FANO    - DOC IDS
    private List<Integer> termFrequenciesDecompressedList;  // SIMPLE9/UNARY - TERM FREQUENCIES
    private int blockPointer;
    private long docIdsBlockPointer;                    // This represents the offset of the next docIdsBlock
    private long termFrequenciesBlockPointer;           // This represents the index of the actual block of term frequencies

    private final Encoder docIdsEncoder = new EliasFano();
    private final Encoder termFrequenciesEncoder = new Simple9(true);

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
        //long docIdsEndOffset = getDocIdsOffset() + getDocIdsLength();
        long docIdsEndOffset = rootEntry.getDocIdsLength();
        return (blockPointer + 1 < docIdsDecompressedList.size()) || (docIdsBlockPointer < docIdsEndOffset);
    }

    @Override
    public Posting next() {
        long docIdsLength = rootEntry.getDocIdsLength();
        if (docIdsBlockPointer == docIdsLength &&
                blockPointer == docIdsDecompressedList.size() - 1)
            throw new NoSuchElementException();

        if (blockPointer + 1 < docIdsDecompressedList.size())
            blockPointer++;
        else {
            loadNextBlock();
            blockPointer = 0;
        }

        return new Posting(termFrequency(), docId());
    }

    @Override
    public void nextGEQ(int docId) throws EOFException {
        while (true) {
            // If we are in the correct block advance the pointer to the right place
            if (docIdsDecompressedList.get(docIdsDecompressedList.size() - 1) >= docId) {
                for (int i = (blockPointer!=-1) ? blockPointer : 0; i < docIdsDecompressedList.size(); i++) {
                    if (docIdsDecompressedList.get(i) >= docId) {
                        blockPointer = i;
                        return;
                    }
                }
            }

            // if we're in the last block and it doesn't contain the docId
            // long docIdsEndOffset = getDocIdsOffset() + getDocIdsLength();
            long docIdsEndOffset = rootEntry.getDocIdsLength();
            if (docIdsBlockPointer == docIdsEndOffset &&
                    docIdsDecompressedList.get(docIdsDecompressedList.size() - 1) < docId)
                throw new EOFException();

            // Else get the next block
            loadNextBlock();
            blockPointer = 0;
        }
    }

    @Override
    public void reset() {
        docIdsBlockPointer = termFrequenciesBlockPointer = 0;
        blockPointer = -1;

        loadNextBlock();
    }

    @Override
    public boolean addPosting(int docId, int termFreq) {
        if (docIdsDecompressedList.isEmpty() || docIdsDecompressedList.get(docIdsDecompressedList.size()-1)!=docId){
            docIdsDecompressedList.add(docId);
            termFrequenciesDecompressedList.add(termFreq);
            return true;
        }

        // se il docId è già presente come ultima entry, aumenta solo la term freq
        termFrequenciesDecompressedList.set(termFrequenciesDecompressedList.size() - 1, termFrequenciesDecompressedList.get(termFrequenciesDecompressedList.size() - 1) + termFreq);
        return false;
    }

    public void loadNextBlock() {
        ByteBlock docIdsBlock = ByteUtils.fetchDocIdsBlock(compressedDocIds, 0, docIdsBlockPointer);
        docIdsBlockPointer = docIdsBlock.getOffset();
        docIdsDecompressedList = docIdsEncoder.decode(docIdsBlock.getBytes());

        ByteBlock termFrequenciesBlock = ByteUtils.fetchNextTermFrequenciesBlock(compressedTermFrequencies, termFrequenciesBlockPointer);
        termFrequenciesBlockPointer = termFrequenciesBlock.getOffset();
        termFrequenciesDecompressedList = termFrequenciesEncoder.decode(termFrequenciesBlock.getBytes());

        // Remove fictitious 0 frequencies
        int first0Index = Math.min(Constants.BLOCK_SIZE, termFrequenciesDecompressedList.size());
        first0Index = Math.min(first0Index, rootEntry.getDocumentFrequency());
        termFrequenciesDecompressedList.subList(
                first0Index,
                termFrequenciesDecompressedList.size()
        ).clear();
    }

    @Override
    public List<Integer> getTermFrequenciesDecompressedList() {
        return termFrequenciesDecompressedList;
    }

    @Override
    public List<Integer> getDocIdsDecompressedList() {
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
