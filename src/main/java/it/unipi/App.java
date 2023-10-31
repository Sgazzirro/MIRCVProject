package it.unipi;

import it.unipi.index.InMemoryIndexing;
import it.unipi.model.implementation.*;

import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DocumentStream ds = new DocumentStream("data/reduced_collection.tar.gz");

        DocumentIndex documentIndex = new DocumentIndex();
        Vocabulary vocabulary = new Vocabulary();
        Tokenizer tokenizer = new Tokenizer();

        InMemoryIndexing inMemoryIndexing = new InMemoryIndexing(ds, documentIndex, vocabulary, tokenizer);
        inMemoryIndexing.buildIndex();
    }
}
