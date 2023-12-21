package it.unipi.index;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.io.DocumentStream;
import it.unipi.model.*;
import it.unipi.io.Dumper;
import it.unipi.scoring.Scorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class InMemoryIndexing implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryIndexing.class);

    private Vocabulary vocabulary;
    private DocumentIndex documentIndex;

    private final Dumper dumper;
    private final Tokenizer tokenizer;

    public InMemoryIndexing(CompressionType compression) {
        vocabulary = Vocabulary.getInstance();
        documentIndex = DocumentIndex.getInstance();

        dumper = Dumper.getInstance(compression);
        tokenizer = Tokenizer.getInstance();
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

            while ((document = tokenStream.nextDoc()) != null)
                processDocument(document);

            dumpVocabulary();
            dumpDocumentIndex();

        } catch (IOException ioException) {
            logger.error("Fatal error when building the index", ioException);
            System.exit(1);
        }
    }

    public void processDocument(Document document) {
        if (document == null || document.getText().isEmpty())
            return;
        List<String> tokenized = tokenizer.tokenizeBySpace(document.getText());

        for (String token : tokenized) {
            vocabulary.addEntry(token, document.getId());
        }

        documentIndex.addDocument(document.getId(), tokenized.size());
    }

    void dumpVocabulary() throws IOException {
        computeTermsPartialUpperBound();

        dumper.dumpVocabulary(vocabulary);
    }

    void dumpVocabularyLine(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        // Onto the vocabulary
        // Term | DF | UpperBound | OffsetID | OffsetTF | DocLen | TFLen
        dumper.dumpVocabularyEntry(entry);
    }

    void dumpDocumentIndex() throws IOException {
        dumper.dumpDocumentIndex(documentIndex);
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

    private void computeTermsPartialUpperBound() {

        for (Map.Entry<String, VocabularyEntry> entry: vocabulary.getEntries()) {
            VocabularyEntry vocabularyEntry = entry.getValue();
            Posting bestPosting = getMaxPosting(vocabularyEntry.getPostingList());

            double partialTermUB = Scorer.partialTF(bestPosting);
            vocabularyEntry.setUpperBound(partialTermUB);
        }
    }

    private Posting getMaxPosting(PostingList postingList) {
        postingList.reset();

        Posting bestPosting = postingList.next();
        while (postingList.hasNext()) {
            Posting next = postingList.next();
            if (bestPosting.compareTo(next) < 0)
                bestPosting = next;
        }

        return bestPosting;
    }
}
