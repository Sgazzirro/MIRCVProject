package it.unipi.index;


import it.unipi.model.implementation.*;
import it.unipi.model.*;
import it.unipi.utils.Constants;
import opennlp.tools.stemmer.PorterStemmer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class InMemoryIndexing {
    /*
        Indexing following in-memory indexing pseudocode.
        Given a stream of documents D:
            - Update its document index entry
            - Tokenize it
            - For all tokens retrieved
                -> eventually update vocabulary entry
                -> update postings
     */

    public DocumentStreamInterface documentStreamInterface;
    public VocabularyInterface vocabulary;

    public DocumentIndexInterface docIndex;

    public TokenizerInterface tokenizerInterface;

    public InMemoryIndexing(DocumentStreamInterface d, DocumentIndexInterface doc, VocabularyInterface v, TokenizerInterface tok){
        documentStreamInterface = d;
        docIndex = doc;
        vocabulary = v;
        tokenizerInterface = tok;
    }

    public void buildIndex() {
        Document document;

        while((document = documentStreamInterface.nextDoc()) != null)
            processDocument(document);

        // Compute idf and upper bound for each term in the vocabulary
        computeScores();

        // TODO - Improve the following methods by periodically dumping the memory instead of dumping it all at once
        dumpVocabulary();
        dumpDocumentIndex();
    }

    private void computeScores() {
        int numDocuments = docIndex.getNumDocuments();

        // Iterate over all terms in the collection
        for (Map.Entry<String, VocabularyEntry> pair : vocabulary.getEntries()) {
            VocabularyEntry entry = pair.getValue();
            int documentFrequency = entry.getDocumentFrequency();

            // Compute IDf for each term
            PostingList postingList = entry.getPostingList();
            double idf = Math.log10((double) numDocuments / documentFrequency);
            postingList.setTermIdf(idf);

            // Compute upper bound for each term
            double upperBound = 0;
            do {
                // The posting list necessarily has at least a posting
                upperBound = Math.max(upperBound, postingList.score());
                postingList.next();
            } while (postingList.hasNext());
            postingList.reset();
        }
    }

    public void processDocument(Document document) {
        List<String> tokenized = tokenizerInterface.tokenizeBySpace(document.getText());

        for (String token : tokenized)
            vocabulary.addEntry(token, document.getId());

        docIndex.addDocument(document.getId(), tokenized.size());
    }

    protected void dumpVocabulary(){
        dumpVocabulary(Constants.VOCABULARY_FILE,
                Constants.DOC_IDS_POSTING_FILE,
                Constants.TF_POSTING_FILE);
    }

    protected void dumpVocabulary(String fvoc, String fids, String ffreq) {

        // Dump vocabulary as csv with structure
        //  term | frequency | upper bound | offset in postings | length of postings
        StringBuilder vocabulary = new StringBuilder();
        StringJoiner docIds = new StringJoiner("\n");
        StringJoiner termFrequencies = new StringJoiner("\n");

        // Keep track of the offset of each term in the posting list
        int offset = 0;

        for (Map.Entry<String, VocabularyEntry> entry : this.vocabulary.getEntries()) {
            String term = entry.getKey();
            VocabularyEntry vocEntry = entry.getValue();

            int termFrequency = vocEntry.getDocumentFrequency();
            double upperBound = vocEntry.getUpperBound();

            PostingList postingList = vocEntry.getPostingList();
            int length = postingList.dumpPostings(docIds, termFrequencies);

            vocabulary.append(term).append(",")
                    .append(termFrequency).append(",")
                    .append(upperBound).append(",")
                    .append(offset).append(",")
                    .append(length).append("\n");
            offset += length;   // Advance the offset by the length of the current posting list
        }

        // Write everything to file
        try (
                PrintWriter vocabularyWriter = new PrintWriter(fvoc);
                PrintWriter docIdsWriter = new PrintWriter(fids);
                PrintWriter termFrequenciesWriter = new PrintWriter(ffreq);
        ) {
            vocabularyWriter.print(vocabulary);
            docIdsWriter.print(docIds);
            termFrequenciesWriter.print(termFrequencies);

        } catch (FileNotFoundException e) {
            System.err.println("There has been an error in writing the vocabulary to file");
            e.printStackTrace();
        }
    }

    protected void dumpDocumentIndex(){
        dumpDocumentIndex(Constants.DOCUMENT_INDEX_FILE);
    }

    protected void dumpDocumentIndex(String filename) {

        // Dump documentIndex as csv with structure
        //  doc id | document length
        StringBuilder documentIndex = new StringBuilder();

        for (Map.Entry<Integer, DocumentIndexEntry> entry : this.docIndex.getEntries()) {
            int docId = entry.getKey();
            DocumentIndexEntry docEntry = entry.getValue();

            int docLength = docEntry.getDocumentLength();
            documentIndex.append(docId).append(",").append(docLength).append("\n");
        }

        // Write everything to file
        try (
                PrintWriter documentIndexWriter = new PrintWriter(filename)
        ) {
            documentIndexWriter.print(documentIndex);

        } catch (FileNotFoundException e) {
            System.err.println("There has been an error in writing the document index to file");
            e.printStackTrace();
        }
    }

    public DocumentStreamInterface getDocumentStreamInterface() {
        return documentStreamInterface;
    }

    public VocabularyInterface getVocabulary() {
        return vocabulary;
    }

    public DocumentIndexInterface getDocIndex() {
        return docIndex;
    }

    public TokenizerInterface getTokenizerInterface() {
        return tokenizerInterface;
    }
}
