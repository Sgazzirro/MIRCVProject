package it.unipi.index;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.io.DocumentStream;
import it.unipi.io.implementation.DumperBinary;
import it.unipi.model.*;
import it.unipi.io.Dumper;
import it.unipi.scoring.Scorer;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class InMemoryIndex implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryIndex.class);

    private Vocabulary vocabulary;
    private DocumentIndex documentIndex;

    private final Dumper dumper;
    private final Tokenizer tokenizer;
    private int docId;

    public InMemoryIndex(CompressionType compression) {
        vocabulary = new Vocabulary(compression);
        documentIndex = new DocumentIndex();

        dumper = Dumper.getInstance(compression);
        tokenizer = Tokenizer.getInstance();
        docId=0;
    }

    public boolean setup(Path filename) {
        return dumper.start(filename);
    }

    @Override
    public void close() {
        vocabulary = null;
        documentIndex = null;

        try {
            dumper.close();
        } catch (Exception e) {
            logger.warn("Something wrong happened when closing the dumper", e);
        }
    }

    public void buildIndex(DocumentStream tokenStream) {
        try {
            Document document;

            while ((document = tokenStream.nextDoc()) != null) {
                if(processDocument(document, docId))
                    docId++;
            }
            dumpDocumentIndex();
            dumpVocabulary();

        } catch (IOException ioException) {
            logger.error("Fatal error when building the index", ioException);
            System.exit(1);
        }
    }

    public boolean processDocument(Document document, int docid) {
        if (document == null || document.getText().isEmpty())
            return false;
        List<String> tokenized = tokenizer.tokenize(document.getText());
        if(tokenized==null)
            return false;

        for (String token : tokenized)
            vocabulary.addEntry(token, docid);

        documentIndex.addDocument(docid, document.getId(), tokenized.size());
        return true;
    }

    void dumpVocabulary() throws IOException {
        Scorer scorer = Scorer.getScorer(documentIndex);
        Constants.N = documentIndex.getNumDocuments();

        for (Map.Entry<String, VocabularyEntry> entry : vocabulary.getMapping().entrySet())
            entry.getValue().computeUpperBound(scorer);

        dumper.dumpVocabulary(vocabulary);
    }

    public void dumpVocabularyLine(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        // Onto the vocabulary
        // Term | DF | OffsetID | OffsetTF | DocLen | TFLen
        dumper.dumpVocabularyEntry(entry);
    }

    void dumpDocumentIndex() throws IOException {
        dumper.dumpDocumentIndex(documentIndex);
    }

    void flushDocumentIndex() throws IOException {
        // Flush entries in document index buffer if any
        if (dumper instanceof DumperBinary dumperBinary)
            dumperBinary.flushDocumentIndexBuffer();
    }

    void dumpDocumentIndexLine(Map.Entry<Integer, DocumentIndexEntry> entry) throws IOException {
        dumper.dumpDocumentIndexEntry(entry);
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public DocumentIndex getDocumentIndex() {
        return documentIndex;
    }
}
