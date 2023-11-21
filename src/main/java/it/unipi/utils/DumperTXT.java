package it.unipi.utils;


import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.PostingListImpl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class DumperTXT implements Dumper {

    private BufferedWriter writerVOC;
    private BufferedWriter writerDIX;
    private BufferedWriter writerDIDS;
    private BufferedWriter writerTF;
    private boolean opened = false;
    private long writtenTF;
    private long writtenDIDS;

    @Override
    public boolean start(String path) {
        try {
            if (opened)
                throw new IOException();

            writerVOC  = new BufferedWriter(new FileWriter(path + Constants.VOCABULARY_FILENAME));
            writerDIX  = new BufferedWriter(new FileWriter(path + Constants.DOCUMENT_INDEX_FILENAME));
            writerDIDS = new BufferedWriter(new FileWriter(path + Constants.DOC_IDS_POSTING_FILENAME));
            writerTF   = new BufferedWriter(new FileWriter(path + Constants.TF_POSTING_FILENAME));
            opened = true;

        } catch(IOException ie) {
            System.out.println("Error in opening the file");
            return false;
        }
        return true;
    }

    @Override
    public void dumpVocabulary(Vocabulary vocabulary) {
        for (Map.Entry<String, VocabularyEntry> entry : vocabulary.getEntries())
            dumpVocabularyEntry(entry);
    }

    @Override
    public void dumpVocabularyEntry(Map.Entry<String, VocabularyEntry> entry) {
        try {
            String term = entry.getKey();
            VocabularyEntry vocEntry = entry.getValue();

            int termFrequency = vocEntry.getDocumentFrequency();
            double upperBound = vocEntry.getUpperBound();
            double IDF = vocEntry.getPostingList().getIdf();

            PostingListImpl postingListImpl = (PostingListImpl) vocEntry.getPostingList();
            long[] offsets = dumpPostings(postingListImpl);
            int length = postingListImpl.getDocIds().size();

            String result =  new StringBuilder().append(term).append(",")
                    .append(termFrequency).append(",")
                    .append(upperBound).append(",")
                    .append(IDF).append(",")
                    .append(offsets[0]).append(",")
                    .append(offsets[1]).append(",")
                    .append(length).append("\n").toString();

            System.out.println(opened);
            writerVOC.write(result);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long[] dumpPostings(PostingListImpl postingListImpl) throws IOException {
        long offsetID = writtenDIDS;
        long offsetTF = writtenTF;
        int length = postingListImpl.getDocIds().size();

        for(int i = 0; i < length; i++) {
            String bufferID = postingListImpl.getDocIds().get(i) + "\n";
            String bufferTF = postingListImpl.getTermFrequencies().get(i) + "\n";
            writerDIDS.write(bufferID);
            writerTF.write(bufferTF);
            writtenDIDS += bufferID.getBytes().length;
            writtenTF += bufferTF.getBytes().length;
        }

        return new long[]{offsetID, offsetTF};
    }

    @Override
    public void dumpDocumentIndex(DocumentIndex docIndex) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, DocumentIndexEntry> entry : docIndex.getEntries()) {
            int docId = entry.getKey();
            DocumentIndexEntry docEntry = entry.getValue();

            int docLength = docEntry.getDocumentLength();
            result.append(docId).append(",").append(docLength).append("\n");
        }

        try {
            writerDIX.write(String.valueOf(result));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry) {
        try {
            writerDIX.write(entry.getKey().toString() + "," + entry.getValue().getDocumentLength() + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean end() {
        try {
            if (!opened)
                throw new IOException();
            writerVOC.close();
            writerDIDS.close();
            writerDIX.close();
            writerTF.close();
            opened = false;
            writtenTF = 0;
            writtenDIDS = 0;
        } catch(IOException ie) {
            System.out.println("Error in opening the file");
            ie.printStackTrace();
        }

        return !opened;
    }
}

