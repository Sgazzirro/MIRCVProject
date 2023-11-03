package it.unipi.index;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.model.implementation.*;
import it.unipi.model.*;
import it.unipi.utils.Constants;
import opennlp.tools.parser.Cons;
import opennlp.tools.stemmer.PorterStemmer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.zip.GZIPInputStream;

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

    public void buildIndex(){
        Document document;

        List<String> stopwords = loadStopwords(Constants.STOPWORDS_FILE);

        // Use Porter stemmer
        PorterStemmer stemmer = new PorterStemmer();

        while((document = documentStreamInterface.nextDoc())!=null){
            List<String> tokenized = tokenizerInterface.tokenizeBySpace(document.getText());
            tokenized.removeAll(stopwords);     // Remove all stopwords

            for (String token : tokenized)
                vocabulary.addEntry(stemmer.stem(token), document.getId());

            docIndex.addDocument(document.getId(), tokenized.size());
        }

        // TODO - Improve the following methods by periodically dumping the memory instead of dumping it all at once
        dumpVocabulary();
        dumpDocumentIndex();
    }

    private void dumpVocabulary() {

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

            int termFrequency = vocEntry.getFrequency();
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
                PrintWriter vocabularyWriter = new PrintWriter(Constants.VOCABULARY_FILE);
                PrintWriter docIdsWriter = new PrintWriter(Constants.DOC_IDS_POSTING_FILE);
                PrintWriter termFrequenciesWriter = new PrintWriter(Constants.TF_POSTING_FILE)
        ) {
            vocabularyWriter.print(vocabulary);
            docIdsWriter.print(docIds);
            termFrequenciesWriter.print(termFrequencies);

        } catch (FileNotFoundException e) {
            System.err.println("There has been an error in writing the vocabulary to file");
            e.printStackTrace();
        }
    }

    private void dumpDocumentIndex() {

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
                PrintWriter documentIndexWriter = new PrintWriter(Constants.DOCUMENT_INDEX_FILE)
        ) {
            documentIndexWriter.print(documentIndex);

        } catch (FileNotFoundException e) {
            System.err.println("There has been an error in writing the document index to file");
            e.printStackTrace();
        }
    }

    private List<String> loadStopwords(String filename) {
        try {
            return Files.readAllLines(new File(filename).toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error loading stopwords");
            return List.of();
        }
    }
}
