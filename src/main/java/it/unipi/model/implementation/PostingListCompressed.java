package it.unipi.model.implementation;

import it.unipi.model.Encoder;
import it.unipi.model.PostingList;
import it.unipi.utils.EliasFanoStruct;
import it.unipi.utils.FetcherCompressed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PostingListCompressed implements PostingList {

    // ELIAS FANO - DOC ID
    private int length;
    private long startOffsetEF;
    private long endOffsetEF;
    private byte[] compressedDocIdList;
    long compressedDocIdListOffset;
    private List<Integer> docIdBlockList;
    int pointerDocIdBlockList;

    // SIMPLE9/UNARY - TERM FREQUENCIES
    private List<Integer> termFrequencyBlockList;

    private Encoder encoder = new EliasFano();

    public PostingListCompressed(Integer length, long startOffsetEF, long endOffsetEF) {
        // VERRà UTILIZZATA DURANTE LA FASE DI SCORING
        this.length = length;
        this.startOffsetEF = startOffsetEF;
        this.endOffsetEF = endOffsetEF;

        // da modificare!!
        FetcherCompressed fc = new FetcherCompressed();
        String filename="boh";
        fc.start(filename);
        compressedDocIdList = fc.fetchDocIdListCompressed(startOffsetEF, endOffsetEF);
        fc.end();

        if (compressedDocIdList != null) {
            byte[] eliasFanoBytes = fc.fetchDocIdSubList(compressedDocIdList, 0, 0L);
            docIdBlockList = encoder.decode(eliasFanoBytes);
            compressedDocIdListOffset = eliasFanoBytes.length;
            pointerDocIdBlockList = 0;
        }
    }

    public PostingListCompressed(){
        // VERRà UTILIZZATO DURANTE LA FASE DI INDEXING
        docIdBlockList = new ArrayList<>();
        termFrequencyBlockList = new ArrayList<>();
    }

    @Override
    public int docId() {
        return docIdBlockList.get(pointerDocIdBlockList);
    }

    @Override
    public boolean hasNext() {
        return !(compressedDocIdListOffset==endOffsetEF && pointerDocIdBlockList==docIdBlockList.size()-1);
    }

    @Override
    public void next() {
        if(pointerDocIdBlockList + 1< docIdBlockList.size()){
            pointerDocIdBlockList++;
        }
        else{
            FetcherCompressed fc = new FetcherCompressed();
            byte[] eliasFanoBytes = fc.fetchDocIdSubList(compressedDocIdList, 0, compressedDocIdListOffset); // metto docId 0 di modo che skippando al blocco puntato da compressedDocListOffset, sicuramente prenderò il blocco subito successivo

            docIdBlockList = encoder.decode(eliasFanoBytes);
            pointerDocIdBlockList=0;
            compressedDocIdListOffset = compressedDocIdListOffset + eliasFanoBytes.length;
            // else throw new EOFException (non dovrebbe succedere mai se uno chiama hasNext, ma chissà....
        }
    }

    @Override
    public void nextGEQ(int docId) {
        if (docIdBlockList.get(docIdBlockList.size()-1) > docId){
            for (int i=pointerDocIdBlockList; i<docIdBlockList.size(); i++){
                if(docIdBlockList.get(i)>=docId){
                    pointerDocIdBlockList=i;
                    return;
                }
            }
        }
        // devo scorrere al nuovo blocco
        FetcherCompressed fc = new FetcherCompressed();
        byte[] eliasFanoBytes = fc.fetchDocIdSubList(compressedDocIdList, docId, compressedDocIdListOffset);

        docIdBlockList = encoder.decode(eliasFanoBytes);
        pointerDocIdBlockList = 0;
        compressedDocIdListOffset = compressedDocIdListOffset + eliasFanoBytes.length;
        // else throw new EOFException (non dovrebbe succedere mai se uno chiama hasNext, ma chissà....
    }

    @Override
    public void addPosting(int docId) {
        addPosting(docId,1);
    }

    @Override
    public void addPosting(int docId, int termFreq) {
        if (docIdBlockList.isEmpty() || docIdBlockList.get(docIdBlockList.size()-1)!=docId){
            docIdBlockList.add(docId);
            termFrequencyBlockList.add(termFreq);
        }
        // se il docId è già presente come ultima entry, aumenta solo la term freq
        termFrequencyBlockList.set(termFrequencyBlockList.size()-1,termFrequencyBlockList.get(termFrequencyBlockList.size()-1)+termFreq);
    }

    @Override
    public int mergePosting(PostingList toMerge) {
        // chiamato solo in fase di indexing
        length += toMerge.getLength();
        // devo concatenare i 2 array di bytes
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(compressedDocIdList);
            outputStream.write(toMerge.getCompressedDocIdArray());
            compressedDocIdList = outputStream.toByteArray();
            return compressedDocIdList.length;
        } catch (IOException e){
            System.err.println("Error in mergePosting");
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<Integer> getDocIdList() {
        return docIdBlockList;
    }

    @Override
    public List<Integer> getTermFrequencyList() {
        return termFrequencyBlockList;
    }

    @Override
    public long getOffsetID() {
        // da fare
        return 0;
    }

    @Override
    public long getOffsetTF() {
        // da fare
        return 0;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public Double getTermIdf() {
        // da fare
        return null;
    }
    @Override
    public double score() {
        // da fare
        return 0;
    }

    @Override
    public byte[] getCompressedDocIdArray() {
        return compressedDocIdList;
    }
}
