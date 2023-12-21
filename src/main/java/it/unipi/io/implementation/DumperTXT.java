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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class DumperTXT implements Dumper {

    private static final Logger logger = LoggerFactory.getLogger(DumperTXT.class);

    private BufferedWriter writerVOC;
    private BufferedWriter writerDIX;
    private BufferedWriter writerDIDS;
    private BufferedWriter writerTF;

    private boolean opened = false;
    private long writtenTF;
    private long writtenDIDS;

    @Override
    public boolean start(Path path) {
        if (!opened) {
            try {
                IOUtils.createDirectory(path);

                writerVOC  = new BufferedWriter(new FileWriter(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
                writerDIX  = new BufferedWriter(new FileWriter(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));
                writerDIDS = new BufferedWriter(new FileWriter(path.resolve(Constants.DOC_IDS_POSTING_FILENAME).toFile()));
                writerTF   = new BufferedWriter(new FileWriter(path.resolve(Constants.TF_POSTING_FILENAME).toFile()));
                writtenTF = 0;
                writtenDIDS = 0;

                opened = true;
                logger.info("Dumper correctly initialized at path: " + path);

            } catch (IOException ie) {
                logger.error("Could not start dumper", ie);
                opened = false;
            }
        } else {
            logger.warn("Dumper was already opened");
        }

        return opened;
    }

    @Override
    public void close() {
        if (!opened) {
            logger.warn("Could not close fetcher: it was not started");
            return;
        }

        try {
            writerVOC.close();
            writerDIDS.close();
            writerDIX.close();
            writerTF.close();

            logger.info("Dumper correctly closed");

        } catch (IOException exception) {
            logger.warn("Error in closing the dumper", exception);
        }
    }

    @Override
    public void dumpVocabulary(Vocabulary vocabulary) throws IOException {
        for (Map.Entry<String, VocabularyEntry> entry : vocabulary.getEntries())
            dumpVocabularyEntry(entry);
    }

    @Override
    public void dumpVocabularyEntry(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        String term = entry.getKey();
        VocabularyEntry vocEntry = entry.getValue();

        int termFrequency = vocEntry.getDocumentFrequency();
        double upperBound = vocEntry.getUpperBound();

        PostingListImpl postingListImpl = (PostingListImpl) vocEntry.getPostingList();
        long[] offsets = dumpPostings(postingListImpl);
        int length = postingListImpl.getDocIdsDecompressedList().size();

        String result = term + "," +
                termFrequency + "," +
                upperBound + "," +
                offsets[0] + "," +
                offsets[1] + "," +
                length + "\n";

        writerVOC.write(result);
    }

    private long[] dumpPostings(PostingList postingList) throws IOException {
        long offsetID = writtenDIDS;
        long offsetTF = writtenTF;
        int length = postingList.getDocIdsDecompressedList().size();

        StringBuilder bufferID = new StringBuilder();
        StringBuilder bufferTF = new StringBuilder();
        for (int i = 0; i < length; i++) {
             bufferID.append(postingList.getDocIdsDecompressedList().get(i)).append("\n");
             bufferTF.append(postingList.getTermFrequenciesDecompressedList().get(i)).append("\n");
        }

        writerDIDS.write(String.valueOf(bufferID));
        writerTF.write(String.valueOf(bufferTF));
        writtenDIDS += bufferID.toString().getBytes().length;
        writtenTF += bufferTF.toString().getBytes().length;

        return new long[] {offsetID, offsetTF};
    }

    @Override
    public void dumpDocumentIndex(DocumentIndex docIndex) throws IOException{
        System.out.println("DOC INDEX L : " + docIndex.getTotalLength());
        System.out.println("DOC INDEX N : " + docIndex.getNumDocuments());
        writerDIX.write(docIndex.getTotalLength() + "\n");
        writerDIX.write(docIndex.getNumDocuments() + "\n");

        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, DocumentIndexEntry> entry : docIndex.getEntries()) {
            int docId = entry.getKey();
            DocumentIndexEntry docEntry = entry.getValue();

            int docLength = docEntry.getDocumentLength();
            result.append(docId).append(",").append(docLength).append("\n");
        }

        writerDIX.write(String.valueOf(result));
    }

    @Override
    public void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry) throws IOException {
        writerDIX.write(entry.getKey().toString() + "," + entry.getValue().getDocumentLength() + "\n");
    }
}

