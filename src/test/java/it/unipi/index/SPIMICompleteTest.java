package it.unipi.index;

import it.unipi.model.DocumentIndex;
import it.unipi.model.DocumentStream;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.Document;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.DocumentIndexImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.DumperTXT;
import it.unipi.utils.Fetcher;
import it.unipi.utils.FetcherTXT;
import it.unipi.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.print.Doc;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SPIMICompleteTest {

    // The test collection is inside "test_collection.tsv"
    // All the test have the same result: a TreeMap representing the vocabulary with all posting lists decompressed
    /*
    0	duck duck duck
    1	beijing dish duck duck
    2	duck duck rabbit recipe
    3   rabbit recipe
     */
    @InjectMocks
    InMemoryIndexing indexerSingleBlock;
    @Mock
    static
    DocumentStream ds;

    static Vocabulary correctVocabulary;
    static DocumentIndex correctDocumentIndex;
    NavigableMap<String, VocabularyEntry> testOutput = new TreeMap<>();
    @BeforeClass
    public static void createResult(){
        correctVocabulary = new VocabularyImpl();
        correctDocumentIndex = new DocumentIndexImpl();

        correctVocabulary.addEntry("duck", 0);
        correctVocabulary.addEntry("duck", 0);
        correctVocabulary.addEntry("duck", 0);
        correctVocabulary.addEntry("duck", 1);
        correctVocabulary.addEntry("duck", 1);
        correctVocabulary.addEntry("duck", 2);
        correctVocabulary.addEntry("duck", 2);

        correctVocabulary.addEntry("beij", 1);

        correctVocabulary.addEntry("dish", 1);

        correctVocabulary.addEntry("rabbit", 2);
        correctVocabulary.addEntry("rabbit", 3);

        correctVocabulary.addEntry("recip", 2);
        correctVocabulary.addEntry("recip", 3);

        correctDocumentIndex.addDocument(0, 3);
        correctDocumentIndex.addDocument(1, 4);
        correctDocumentIndex.addDocument(2, 4);
        correctDocumentIndex.addDocument(3, 2);

    }

    @Before
    public void setup(){
        when(ds.nextDoc()).thenReturn(
                new Document("0\tduck duck duck"),
                new Document("1\tbeij dish duck duck"),
                new Document("2\tduck duck rabbit recip"),
                new Document("3\trabbit recip"),
                null
        );
    }


    /**
     * In this test, we check if the index is correctly written in the case:
     * - Single Block
     * - DEBUG Version
     */
    @Test
    public void testBuildInSingleBlock_DEBUG_NOTSPIMI(){

        String mode = "DEBUG";

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(ds, new VocabularyImpl(), new DumperTXT(), new DocumentIndexImpl());
        indexerSingleBlock.buildIndex("./test/");


        // Fetching
        Fetcher fetcher = new FetcherTXT();
        fetcher.start("./test/");
        Map.Entry<String, VocabularyEntry> entry;

        while((entry = fetcher.loadVocEntry()) != null){
           testOutput.put(entry.getKey(), entry.getValue());
        }

        DocumentIndex fetchedIndex = new DocumentIndexImpl();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();

        assertTrue(Objects.equals(correctVocabulary.getEntries(), testOutput.entrySet()) && Objects.equals(correctDocumentIndex, fetchedIndex));
    }

    @Test
    public void testBuildSingleBlock_DEBUG(){
        String mode = "DEBUG";

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(ds, new VocabularyImpl(), new DumperTXT(), new DocumentIndexImpl());
        new newSPIMI(ds, indexerSingleBlock).buildIndexSPIMI("DEBUG", "./test/");


        // Fetching
        Fetcher fetcher = new FetcherTXT();
        fetcher.start("./test/");
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }

        DocumentIndex fetchedIndex = new DocumentIndexImpl();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();

        assertTrue(Objects.equals(correctVocabulary.getEntries(), testOutput.entrySet()) && Objects.equals(correctDocumentIndex, fetchedIndex));
    }

    @Test
    public void testBuildManyBlocks_DEBUG(){

        String mode = "DEBUG";

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(ds, new VocabularyImpl(), new DumperTXT(), new DocumentIndexImpl());
        newSPIMI spimi = new newSPIMI(ds, indexerSingleBlock);
        spimi.setLimit(1);
        spimi.buildIndexSPIMI("DEBUG", "./test/");


        // Fetching
        Fetcher fetcher = new FetcherTXT();
        fetcher.start("./test/");
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }

        DocumentIndex fetchedIndex = new DocumentIndexImpl();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();
        assertTrue(Objects.equals(correctVocabulary.getEntries(), testOutput.entrySet()) && Objects.equals(correctDocumentIndex, fetchedIndex));
    }
    @After
    public void flush() {
        try{
            FileUtils.deleteDirectory(new File("./test/"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
