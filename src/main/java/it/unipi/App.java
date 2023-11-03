package it.unipi;

import it.unipi.index.InMemoryIndexing;
import it.unipi.index.SPIMIIndex;
import it.unipi.model.implementation.*;
import it.unipi.utils.Constants;
import it.unipi.utils.LoadStructures;

import java.util.List;
import java.util.Objects;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DocumentStream ds = new DocumentStream(Constants.COLLECTION_FILE);

        DocumentIndex documentIndex = new DocumentIndex();
        Vocabulary vocabulary = new Vocabulary();
        Tokenizer tokenizer = new Tokenizer();

        InMemoryIndexing inMemoryIndexing = new InMemoryIndexing(ds, documentIndex, vocabulary, tokenizer);
        inMemoryIndexing.buildIndex();

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
