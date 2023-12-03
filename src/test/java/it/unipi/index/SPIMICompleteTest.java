package it.unipi.index;

import it.unipi.model.DocumentIndex;
import it.unipi.model.DocumentStream;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.model.implementation.Document;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.DocumentIndexImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.*;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
        Constants.setCompression(false);
        System.out.println("CORRENTE STATO DELLA COMPRESSIONE: " + Constants.getCompression());
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
        new newSPIMI("DEBUG", ds, indexerSingleBlock).buildIndexSPIMI("./test/");


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
        newSPIMI spimi = new newSPIMI("DEBUG", ds, indexerSingleBlock);
        spimi.setLimit(1);
        spimi.buildIndexSPIMI("./test/");


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
    public void testBuildSingleBlock_BINARY(){
        String mode = "DEBUG";

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(ds, new VocabularyImpl(), new DumperBinary(), new DocumentIndexImpl());
        new newSPIMI("NOT_COMPRESSED", ds, indexerSingleBlock).buildIndexSPIMI("./test/");


        // Fetching
        Fetcher fetcher = new FetcherBinary();
        fetcher.start("./test/");
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }
        int[] docIndexInfo = fetcher.getInformations();
        DocumentIndex fetchedIndex = new DocumentIndexImpl();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();
        Assert.assertEquals(correctVocabulary.getEntries(), testOutput.entrySet());
        assertTrue( Objects.equals(correctDocumentIndex, fetchedIndex));
    }

    @Test
    public void testBuildManyBlocks_BINARY(){

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(ds, new VocabularyImpl(), new DumperBinary(), new DocumentIndexImpl());
        newSPIMI spimi = new newSPIMI("NOT_COMPRESSED", ds, indexerSingleBlock);
        spimi.setLimit(1);
        spimi.buildIndexSPIMI("./test/");


        // Fetching
        Fetcher fetcher = new FetcherBinary();
        fetcher.start("./test/");
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }
        int[] docIndexInfod = fetcher.getInformations();
        DocumentIndex fetchedIndex = new DocumentIndexImpl();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();
        assertTrue(Objects.equals(correctVocabulary.getEntries(), testOutput.entrySet()));
        System.out.println(correctDocumentIndex);
        System.out.println(fetchedIndex);
        assertTrue( Objects.equals(correctDocumentIndex, fetchedIndex));
    }

    @Test
    public void testBuildSingleBlock_COMPRESSED(){
        Constants.setCompression(true);
        String mode = "DEBUG";
        // Dumping
        indexerSingleBlock = new InMemoryIndexing(ds, new VocabularyImpl(), new DumperCompressed(), new DocumentIndexImpl());
        new newSPIMI("COMPRESSED", ds, indexerSingleBlock).buildIndexSPIMI("./test/");


        // Fetching
        Fetcher fetcher = new FetcherCompressed();
        fetcher.start("./test/");
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }
        fetcher.getInformations();
        DocumentIndex fetchedIndex = new DocumentIndexImpl();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();
        System.out.println(testOutput.entrySet());
        System.out.println(correctVocabulary.getEntries());
        assertTrue(Objects.equals(correctVocabulary.getEntries(), testOutput.entrySet()));
        System.out.println(correctDocumentIndex);
        System.out.println(fetchedIndex);

        assertTrue( Objects.equals(correctDocumentIndex, fetchedIndex));
        Constants.setCompression(false);
    }

    @Test
    public void testBuildManyBlocks_COMPRESSED(){
        Constants.setCompression(true);
        // Dumping
        indexerSingleBlock = new InMemoryIndexing(ds, new VocabularyImpl(), new DumperCompressed(), new DocumentIndexImpl());
        newSPIMI spimi = new newSPIMI("COMPRESSED", ds, indexerSingleBlock);
        spimi.setLimit(1);
        spimi.buildIndexSPIMI("./test/");


        // Fetching
        Fetcher fetcher = new FetcherCompressed();
        fetcher.start("./test/");
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }
        fetcher.getInformations();
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
