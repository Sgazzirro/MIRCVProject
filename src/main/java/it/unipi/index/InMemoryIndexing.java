package it.unipi.index;

import it.unipi.encoding.Tokenizer;
import it.unipi.io.DocumentStream;
import it.unipi.model.*;
import it.unipi.io.Dumper;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static java.lang.System.exit;

public class InMemoryIndexing {
    private Vocabulary vocabulary;
    private final Dumper dumper;
    private final Tokenizer tokenizer;
    private DocumentIndex docIndex;

    public InMemoryIndexing(Vocabulary voc, Dumper d, DocumentIndex di){
        vocabulary = voc;
        dumper = d;
        tokenizer = Tokenizer.getInstance(false, false);
        docIndex = di;
    }

    boolean setup(Path filename) {
        return dumper.start(filename);
    }

    boolean close(){
        return dumper.end();
    }

    // FIXME: This function is only used when you write the fully index in memory
    public void buildIndex(DocumentStream tokenStream, Path filePath) {
        Optional<Document> document;

        if (!setup(filePath)) {
            System.err.println("Something strange in opening the file");
            exit(1);
        }

        while((document = Optional.ofNullable(tokenStream.nextDoc())).isPresent())
            processDocument(document.get());

        dumpVocabulary();
        dumpDocumentIndex();

        if (!close()) {
            System.err.println("Something strange in closing the file");
            exit(1);
        }
    }

    public boolean processDocument(Document document) {
        if(document == null || document.getText().isEmpty())
            return false;
        List<String> tokenized = tokenizer.tokenizeBySpace(document.getText());

        for (String token : tokenized) {
            vocabulary.addEntry(token, document.getId());
        }

        docIndex.addDocument(document.getId(), tokenized.size());
        return true;
    }

    void dumpVocabulary(){
        dumper.dumpVocabulary(vocabulary);
        // Flush
        vocabulary = Vocabulary.getInstance();
    }

    void dumpVocabularyLine(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        // Onto the vocabulary
        // Term | DF | UpperBound | OffsetID | OffsetTF | DocLen | TFLen
        dumper.dumpVocabularyEntry(entry);
    }

    void dumpDocumentIndex(){
        dumper.dumpDocumentIndex(docIndex);
        // Flush
        docIndex = DocumentIndex.getInstance();
    }

    void dumpDocumentIndexLine(Map.Entry<Integer, DocumentIndexEntry> entry){
        dumper.dumpDocumentIndexEntry(entry);
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public DocumentIndex getDocIndex() {
        return docIndex;
    }

    public void computePartialTermUB() {
        // TODO: With BM25 serve l'average length, quindi stiamo attenti a come calcolare questo partial score perché forse in realtà non si può
        for(Map.Entry<String, VocabularyEntry> entry: vocabulary.getEntries()){
            VocabularyEntry vocabularyEntry = entry.getValue();
            List<Integer> termFreqList = vocabularyEntry.getPostingList().getTermFrequenciesDecompressedList();
            int maxTermFreq = Collections.max(termFreqList);

            // partial term upper bound = (1+log(tf))
            double partialTermUB = 1+Math.log10(maxTermFreq);
            vocabularyEntry.setUpperBound(partialTermUB);
        }
    }
}
