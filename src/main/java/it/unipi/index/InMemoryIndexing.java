package it.unipi.index;


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

    public Tokenizer tokenizer;

    public InMemoryIndexing(DocumentStreamInterface d, DocumentIndex doc, VocabularyInterface v, InvertedIndex i, Tokenizer tok){
        documentStreamInterface = d;
        docIndex = doc;
        vocabulary = v;
        index = i;
        tokenizer = tok;
    }

    public void buildIndex(){

        while(documentStreamInterface.hasNext()){
            String[] document = documentStreamInterface.nextDoc();
            int docid = Integer.parseInt(document[0]);
            String content = document[1];

            for(String token : tokenizer.tokenizeBySpace(content)){
                vocabulary.addEntry(token, docid);

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
