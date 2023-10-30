package it.unipi.index;


import it.unipi.model.*;

import javax.print.Doc;
import java.util.Map;

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

    public DocumentStream documentStream;
    public VocabularyInterface vocabulary;

    public DocumentIndex docIndex;

    public InvertedIndex index;

    public Tokenizer tokenizer;

    public InMemoryIndexing(DocumentStream d, DocumentIndex doc, VocabularyInterface v, InvertedIndex i, Tokenizer tok){
        documentStream = d;
        docIndex = doc;
        vocabulary = v;
        index = i;
        tokenizer = tok;
    }

    public void buildIndex(){

        while(documentStream.hasNext()){
            String[] document = documentStream.nextDoc();
            String docid = document[0];
            String content = document[1];

            for(String token : tokenizer.tokenizeBySpace(content)){

                // Check if the term already exists into the vocabulary
                VocabularyEntry entry;
                if(!vocabulary.isPresent(token))
                    entry = vocabulary.addEntry(token);
                else
                    entry = vocabulary.getEntry(token);

                int location = entry.getLocationPostingList();

                index.addPosting(location, docid);

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
