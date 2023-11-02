package it.unipi.index;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.model.implementation.Document;
import it.unipi.model.*;
import it.unipi.model.implementation.PostingList;
import it.unipi.model.implementation.Vocabulary;
import it.unipi.model.implementation.VocabularyEntry;
import opennlp.tools.stemmer.PorterStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    public void buildIndex(){
        Document document;

        // Stopwords downloaded from https://raw.githubusercontent.com/stopwords-iso/stopwords-en/master/stopwords-en.txt
        List<String> stopwords = loadStopwords("data/stopwords-en.txt");

        while((document = documentStreamInterface.nextDoc())!=null){
            List<String> tokenized = tokenizerInterface.tokenizeBySpace(document.getText());
            tokenized.removeAll(stopwords);     // Remove all stopwords

            // Use Porter stemmer
            PorterStemmer stemmer = new PorterStemmer();

            for (String token : tokenized) {
                vocabulary.addEntry(stemmer.stem(token), document.getId());
            }
            docIndex.addDocument(document.getId(), tokenized.size());
        }

        // TODO - Improve the following methods by periodically dump the memory instead of dumping altogether
        dumpVocabulary();
        dumpDocumentIndex();
    }

    private void dumpVocabulary() {
        String vocabularyFile = "data/vocabulary.csv";
        String docIdsFile = "data/doc_ids.txt";
        String termFrequenciesFile = "data/term_frequencies.txt";

        // Dump vocabulary as csv with structure
        //  term | frequency | upper bound | offset in postings
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

            vocabulary.append(term).append(",").append(termFrequency).append(",").append(upperBound).append(",").append(offset).append("\n");

            PostingList postingList = vocEntry.getPostingList();
            offset += postingList.dumpPostings(docIds, termFrequencies);
        }

        // Write everything to file
        try (
                PrintWriter vocabularyWriter = new PrintWriter(vocabularyFile);
                PrintWriter docIdsWriter = new PrintWriter(docIdsFile);
                PrintWriter termFrequenciesWriter = new PrintWriter(termFrequenciesFile)
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
        String documentIndexFile = "data/document_index.json";

        // Dump document index as Json
        ObjectMapper objectMapper = new ObjectMapper();
        try(
                PrintWriter documentIndexWriter = new PrintWriter(documentIndexFile)
        ) {
            String documentIndexJSON = objectMapper.writeValueAsString(docIndex);
            documentIndexWriter.println(documentIndexJSON);

        } catch (JsonProcessingException | FileNotFoundException e) {
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
