package it.unipi.model.implementation;

import it.unipi.model.PostingList;

import java.util.List;

public class PostingListCompressed implements PostingList {
    // ELIAS FANO - DOC ID

    private Integer length;
    private List<Integer> docIdBlockList;
    private byte[] compressedDocIdList;
    private long offsetDocId;
    int pointerDocId;


    // SIMPLE9/UNARY - TERM FREQUENCIES
    private List<Integer> termFrequencyBlockList;



    @Override
    public int docId() {
        return docIdBlockList.get(pointerDocId);
    }

    @Override
    public boolean hasNext() {
        // fattibile
        return false;
    }

    @Override
    public void next() {
        // Sto ancora nella ArrayList?
        // se si, pointer ++;
        // altrimenti:
        //      if (hasNext())
        //          efs = FetcherCompressed.fetchDocIdSubList(compressedDocIdList, docId);      da capire come trovare docId per andare avanti, oppure implementare un fetcher che va a pescare il prossimo blocco
        //          docIdBlockList = decode(efs)
        //          pointer = 0

    }

    @Override
    public void nextGEQ(int docId) {
        // uguale a sopra
    }

    @Override
    public void addPosting(int docId) {
        // da gestire bene
    }

    @Override
    public void addPosting(int docId, int freq) {
        // da gestire bene
    }

    @Override
    public int mergePosting(PostingList toMerge) {
        // fattibile
        return 0;
    }

    @Override
    public List<Integer> getDocIdList() {
        // qua come si fa?
        return null;
    }

    @Override
    public List<Integer> getTermFrequencyList() {
        return null;
    }

    @Override
    public long getOffsetID() {
        return 0;
    }

    @Override
    public long getOffsetTF() {
        return 0;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public Double getTermIdf() {
        return null;
    }

    @Override
    public double score() {
        return 0;
    }
}
