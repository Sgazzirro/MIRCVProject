package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.index.InMemoryIndexing;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.Dumper;
import it.unipi.model.DocumentIndex;
import it.unipi.io.DocumentStream;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.DocumentIndexImpl;
import it.unipi.io.implementation.DocumentStreamImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        DocumentStream ds = DocumentStream.getInstance();
        DocumentIndex di = new DocumentIndexImpl();
        Vocabulary v = new VocabularyImpl();
        Dumper d = Dumper.getInstance(CompressionType.COMPRESSED);

        InMemoryIndexing memoryIndexing = new InMemoryIndexing(v, d, di);
        Constants.setCompression(CompressionType.COMPRESSED);
        SPIMIIndex spimi = new SPIMIIndex(CompressionType.COMPRESSED, ds, memoryIndexing);
        Path indexPath = Paths.get("./data");
        Constants.setPath(indexPath);
        spimi.buildIndexSPIMI(indexPath);

    }

}
