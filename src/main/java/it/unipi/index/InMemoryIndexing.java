package it.unipi.index;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.model.implementation.Document;
import it.unipi.model.*;
import it.unipi.model.implementation.Vocabulary;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
        while((document = documentStreamInterface.nextDoc())!=null){
            List<String> tokenized = tokenizerInterface.tokenizeBySpace(document.getText());
            for(String token : tokenized){
                vocabulary.addEntry(token, document.getId());
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
}
