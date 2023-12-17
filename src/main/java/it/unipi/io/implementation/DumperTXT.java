package it.unipi.io.implementation;


import it.unipi.io.Dumper;
import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.utils.Constants;
import it.unipi.utils.IOUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
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
    public boolean start(Path path) {
        // ./data/blocks/_
        try {
            IOUtils.createDirectory(path);

            if (opened)
                throw new IOException();

            writerVOC  = new BufferedWriter(new FileWriter(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
            writerDIX  = new BufferedWriter(new FileWriter(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));
            writerDIDS = new BufferedWriter(new FileWriter(path.resolve(Constants.DOC_IDS_POSTING_FILENAME).toFile()));
            writerTF   = new BufferedWriter(new FileWriter(path.resolve(Constants.TF_POSTING_FILENAME).toFile()));

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
            int length = postingListImpl.getDocIdsDecompressedList().size();

            String result =  new StringBuilder().append(term).append(",")
                    .append(termFrequency).append(",")
                    .append(upperBound).append(",")
                    .append(IDF).append(",")
                    .append(offsets[0]).append(",")
                    .append(offsets[1]).append(",")
                    .append(length).append("\n").toString();

            writerVOC.write(result);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long[] dumpPostings(PostingList postingList) throws IOException {
        long offsetID = writtenDIDS;
        long offsetTF = writtenTF;
        int length = postingList.getDocIdsDecompressedList().size();

        for(int i = 0; i < length; i++) {
            String bufferID = postingList.getDocIdsDecompressedList().get(i) + "\n";
            String bufferTF = postingList.getTermFrequenciesDecompressedList().get(i) + "\n";
            writerDIDS.write(bufferID);
            writerTF.write(bufferTF);
            writtenDIDS += bufferID.getBytes().length;
            writtenTF += bufferTF.getBytes().length;
        }

        return new long[]{offsetID, offsetTF};
    }

    @Override
    public void dumpDocumentIndex(DocumentIndex docIndex) {
        try{
            System.out.println("DOC INDEX L : " + docIndex.getTotalLength());
            System.out.println("DOC INDEX N : " + docIndex.getNumDocuments());
            writerDIX.write(docIndex.getTotalLength() + "\n");
            writerDIX.write(docIndex.getNumDocuments() + "\n");
        } catch (IOException ie){
            ie.printStackTrace();
        }

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

