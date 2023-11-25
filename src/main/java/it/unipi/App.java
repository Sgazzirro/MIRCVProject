package it.unipi;

import it.unipi.index.InMemoryIndexing;
import it.unipi.index.newSPIMI;
import it.unipi.model.*;
import it.unipi.model.implementation.DocumentIndexImpl;
import it.unipi.model.implementation.DocumentStreamImpl;
import it.unipi.model.implementation.TokenizerImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.*;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        /*
        DocumentStreamImpl ds = new DocumentStreamImpl(Constants.COLLECTION_FILE);

        DocumentIndexImpl documentIndexImpl = new DocumentIndexImpl();
        VocabularyImpl vocabularyImpl = new VocabularyImpl();
        TokenizerImpl tokenizerImpl = new TokenizerImpl();

        InMemoryIndexing inMemoryIndexing = new InMemoryIndexing(ds, documentIndex, vocabulary, tokenizer);
        inMemoryIndexing.assignWriters(Constants.VOCABULARY_FILE, Constants.DOC_IDS_POSTING_FILE, Constants.TF_POSTING_FILE,0);
        inMemoryIndexing.buildIndex();


       SPIMIIndex index = new SPIMIIndex(ds, tokenizer);
       index.buildIndexSPIMI();

        VocabularyImpl vocabularyImplLoaded = LoadStructures.loadVocabulary(Constants.VOCABULARY_FILE);
        DocumentIndexImpl documentIndexImplLoaded = LoadStructures.loadDocumentIndex(Constants.DOCUMENT_INDEX_FILE);

        if (!Objects.equals(vocabularyImpl, vocabularyImplLoaded)) {
            System.err.println("Error loading the vocabulary");
            System.out.println(vocabularyImpl);
            System.out.println();
            System.out.println(vocabularyImplLoaded);
        }
        if (!Objects.equals(documentIndexImpl, documentIndexImplLoaded)) {
            System.err.println("Error loading the document index");
            System.out.println(documentIndexImpl);
            System.out.println();
            System.out.println(documentIndexImplLoaded);
        }

*/
        /**
         * NEW TESTS OF 14/11/2023
         */
        DocumentStream ds = new DocumentStreamImpl(Constants.COLLECTION_FILE);
        Tokenizer t = new TokenizerImpl();
        DocumentIndex di = new DocumentIndexImpl();
        Vocabulary v = new VocabularyImpl();
        Dumper d =  new DumperBinary();

        InMemoryIndexing memoryIndexing = new InMemoryIndexing(ds, v, d, t, di);
        // memoryIndexing.buildIndex("data/");

        newSPIMI spimi = new newSPIMI(ds, memoryIndexing);
        spimi.buildIndexSPIMI("NOT_COMPRESSED");

    }


}
