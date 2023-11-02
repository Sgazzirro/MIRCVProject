package it.unipi.index;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.model.implementation.Document;
import it.unipi.model.*;
import it.unipi.model.implementation.Vocabulary;
import opennlp.tools.stemmer.PorterStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

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
        writeStructuresJSON();
    }

    public void writeStructuresJSON(){
        ObjectMapper objectMapper = new ObjectMapper();
        try(
                PrintWriter writerVoc = new PrintWriter("data/vocabulary.json");
                PrintWriter writerDocIndex = new PrintWriter("data/documentIndex.json");
                ){
            String vocabularyJSON = objectMapper.writeValueAsString(vocabulary);
            String docIndexJSON = objectMapper.writeValueAsString(docIndex);
            writerVoc.println(vocabularyJSON);
            writerDocIndex.println(docIndexJSON);
        } catch (JsonProcessingException | FileNotFoundException e) {
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
