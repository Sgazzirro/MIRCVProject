package it.unipi;

import it.unipi.index.InMemoryIndexing;
import it.unipi.index.SPIMIIndex;
import it.unipi.model.implementation.*;
import it.unipi.utils.Constants;
import it.unipi.utils.LoadStructures;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        DocumentStream ds = new DocumentStream(Constants.COLLECTION_FILE);

        DocumentIndex documentIndex = new DocumentIndex();
        Vocabulary vocabulary = new Vocabulary();
        Tokenizer tokenizer = new Tokenizer();
/*
        InMemoryIndexing inMemoryIndexing = new InMemoryIndexing(ds, documentIndex, vocabulary, tokenizer);
        inMemoryIndexing.assignWriters(Constants.VOCABULARY_FILE, Constants.DOC_IDS_POSTING_FILE, Constants.TF_POSTING_FILE,0);
        inMemoryIndexing.buildIndex();
*/
/*
       SPIMIIndex index = new SPIMIIndex(ds, tokenizer);
       index.buildIndexSPIMI();
*/
        Vocabulary vocabularyLoaded = LoadStructures.loadVocabulary(Constants.VOCABULARY_FILE);
        DocumentIndex documentIndexLoaded = LoadStructures.loadDocumentIndex(Constants.DOCUMENT_INDEX_FILE);

        if (!Objects.equals(vocabulary, vocabularyLoaded)) {
            System.err.println("Error loading the vocabulary");
            System.out.println(vocabulary);
            System.out.println();
            System.out.println(vocabularyLoaded);
        }
        if (!Objects.equals(documentIndex, documentIndexLoaded)) {
            System.err.println("Error loading the document index");
            System.out.println(documentIndex);
            System.out.println();
            System.out.println(documentIndexLoaded);
        }



    }


}
