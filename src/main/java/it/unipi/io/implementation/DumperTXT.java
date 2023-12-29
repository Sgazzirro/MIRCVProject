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

    private BufferedWriter vocabularyWriter;
    private BufferedWriter documentIndexWriter;
    private BufferedWriter docIdsWriter;
    private BufferedWriter termFreqWriter;

    private boolean opened = false;
    private long termFreqOffset;
    private long docIdsOffset;

    @Override
    public boolean start(Path path) {
        if (!opened) {
            try {
                IOUtils.createDirectory(path);

                vocabularyWriter = new BufferedWriter(new FileWriter(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
                documentIndexWriter = new BufferedWriter(new FileWriter(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));
                docIdsWriter = new BufferedWriter(new FileWriter(path.resolve(Constants.DOC_IDS_FILENAME).toFile()));
                termFreqWriter = new BufferedWriter(new FileWriter(path.resolve(Constants.TERM_FREQ_FILENAME).toFile()));
                termFreqOffset = docIdsOffset = 0;

                opened = true;
                logger.trace("Dumper correctly initialized at path " + path);

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
            vocabularyWriter.close();
            docIdsWriter.close();
            documentIndexWriter.close();
            termFreqWriter.close();

            logger.trace("Dumper correctly closed");
            opened = false;

        } catch (IOException exception) {
            logger.warn("Error in closing the dumper", exception);
        }
    }

    @Override
    public void dumpVocabulary(Vocabulary vocabulary) throws IOException {
        for (Map.Entry<String, VocabularyEntry> entry : vocabulary.getMapping().entrySet())
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
        int length = postingListImpl.getDocIdsList().size();

        String result = term + "," +
                termFrequency + "," +
                upperBound + "," +
                offsets[0] + "," +
                offsets[1] + "," +
                length + "\n";

        vocabularyWriter.write(result);
    }

    private long[] dumpPostings(PostingList postingList) throws IOException {
        long offsetID = docIdsOffset;
        long offsetTF = termFreqOffset;
        int length = postingList.getDocIdsList().size();

        StringBuilder bufferID = new StringBuilder();
        StringBuilder bufferTF = new StringBuilder();
        for (int i = 0; i < length; i++) {
             bufferID.append(postingList.getDocIdsList().get(i)).append("\n");
             bufferTF.append(postingList.getTermFrequenciesList().get(i)).append("\n");
        }

        docIdsWriter.write(String.valueOf(bufferID));
        termFreqWriter.write(String.valueOf(bufferTF));
        docIdsOffset += bufferID.toString().getBytes().length;
        termFreqOffset += bufferTF.toString().getBytes().length;

        return new long[] {offsetID, offsetTF};
    }

    @Override
    public void dumpDocumentIndex(DocumentIndex docIndex) throws IOException {
        documentIndexWriter.write(docIndex.getTotalLength() + "\n");
        documentIndexWriter.write(docIndex.getNumDocuments() + "\n");

        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, DocumentIndexEntry> entry : docIndex.getEntries()) {
            int docId = entry.getKey();
            DocumentIndexEntry docEntry = entry.getValue();

            int docNo = docEntry.getDocNo();
            int docLength = docEntry.getDocumentLength();

            result.append(docId).append(",").append(docNo).append(",").append(docLength).append("\n");
        }

        documentIndexWriter.write(String.valueOf(result));
    }

    @Override
    public void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry) throws IOException {
        documentIndexWriter.write(entry.getKey().toString() + "," + entry.getValue().getDocNo()+ "," + entry.getValue().getDocumentLength() + "\n");
    }

    public void flushDocumentIndexBuffer(){
        try {
            documentIndexWriter.close();
        }catch(IOException ie){
            logger.error(ie.getMessage());
        }
    }
}

