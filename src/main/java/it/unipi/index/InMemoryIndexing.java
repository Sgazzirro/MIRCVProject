package it.unipi.index;


import it.unipi.model.implementation.Document;
import it.unipi.model.*;

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

    public DocumentIndex docIndex;

    public InvertedIndex index;

    public TokenizerInterface tokenizerInterface;

    public InMemoryIndexing(DocumentStreamInterface d, DocumentIndex doc, VocabularyInterface v, InvertedIndex i, TokenizerInterface tok){
        documentStreamInterface = d;
        docIndex = doc;
        vocabulary = v;
        index = i;
        tokenizerInterface = tok;
    }

    public void buildIndex(){
        Document document;
        while((document = documentStreamInterface.nextDoc())!=null){
            for(String token : tokenizerInterface.tokenizeBySpace(document.getText())){
                vocabulary.addEntry(token, document.getId());

                // TODO: Update Vocabulary
                // TODO: Update DocIndex
            }
        }

        writeStructures();
    }

    public void writeStructures(){
        //TODO:
    }
}
