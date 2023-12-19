package it.unipi.index;

import it.unipi.encoding.CompressionType;
import it.unipi.io.Dumper;
import it.unipi.io.Fetcher;
import it.unipi.io.implementation.DumperTXT;
import it.unipi.model.DocumentIndex;
import it.unipi.io.DocumentStream;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.Document;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.utils.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
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
    static DocumentStream ds;

    static Vocabulary correctVocabulary;
    static DocumentIndex correctDocumentIndex;
    NavigableMap<String, VocabularyEntry> testOutput = new TreeMap<>();

    @BeforeClass
    public static void createResult() {
        Constants.setCompression(CompressionType.DEBUG);
        correctVocabulary = Vocabulary.getInstance();
        correctDocumentIndex = DocumentIndex.getInstance();

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
    public void setup() {
        Constants.setPath(Constants.testPath);

        IOUtils.deleteDirectory(Constants.testPath);
        IOUtils.createDirectory(Constants.testPath);

        when(ds.nextDoc()).thenReturn(
                new Document("0\tduck duck duck"),
                new Document("1\tbeij dish duck duck"),
                new Document("2\tduck duck rabbit recip"),
                new Document("3\trabbit recip"),
                null
        );
        Constants.setCompression(CompressionType.DEBUG);
        System.out.println("CORRENTE STATO DELLA COMPRESSIONE: " + Constants.getCompression());
    }


    /**
     * In this test, we check if the index is correctly written in the case:
     * - Single Block
     * - DEBUG Version
     */
    @Test
    public void testBuildInSingleBlock_DEBUG_NOTSPIMI(){

        CompressionType compression = CompressionType.DEBUG;

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(Vocabulary.getInstance(), new DumperTXT(), DocumentIndex.getInstance());
        indexerSingleBlock.buildIndex(ds, Constants.testPath);

        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.testPath);
        Map.Entry<String, VocabularyEntry> entry;

        while ( (entry = fetcher.loadVocEntry()) != null )
            testOutput.put(entry.getKey(), entry.getValue());

        DocumentIndex fetchedIndex = DocumentIndex.getInstance();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;

        fetcher.getDocumentIndexStats();      // Skip first two lines
        while((entryDI = fetcher.loadDocEntry()) != null)
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        fetcher.end();

        assertEquals(correctVocabulary.getEntries(), testOutput.entrySet());
        assertEquals(correctDocumentIndex, fetchedIndex);
    }


    @Test
    public void testBuildSingleBlock_DEBUG() {

        CompressionType compression = CompressionType.DEBUG;

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(Vocabulary.getInstance(), Dumper.getInstance(compression), DocumentIndex.getInstance());
        new SPIMIIndex(compression, ds, indexerSingleBlock).buildIndexSPIMI(Constants.testPath);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.testPath);
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }

        DocumentIndex fetchedIndex = DocumentIndex.getInstance();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;

        fetcher.getDocumentIndexStats();      // Skip first two lines
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();

        assertTrue(Objects.equals(correctVocabulary.getEntries(), testOutput.entrySet()) && Objects.equals(correctDocumentIndex, fetchedIndex));
    }

    @Test
    public void testBuildManyBlocks_DEBUG(){

        CompressionType compression = CompressionType.DEBUG;

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(Vocabulary.getInstance(), Dumper.getInstance(compression), DocumentIndex.getInstance());
        SPIMIIndex spimi = new SPIMIIndex(compression, ds, indexerSingleBlock);
        spimi.setLimit(1);
        spimi.buildIndexSPIMI(Constants.testPath);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.testPath);
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }

        DocumentIndex fetchedIndex = DocumentIndex.getInstance();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;

        fetcher.getDocumentIndexStats();      // Skip first two lines
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();
        assertEquals(correctVocabulary.getEntries(), testOutput.entrySet());
        assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @Test
    public void testBuildSingleBlock_BINARY(){

        CompressionType compression = CompressionType.BINARY;

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(Vocabulary.getInstance(), Dumper.getInstance(compression), DocumentIndex.getInstance());
        new SPIMIIndex(compression, ds, indexerSingleBlock).buildIndexSPIMI(Constants.testPath);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.testPath);
        Map.Entry<String, VocabularyEntry> entry;
        while ( (entry = fetcher.loadVocEntry()) != null )
            testOutput.put(entry.getKey(), entry.getValue());

        int[] docIndexInfo = fetcher.getDocumentIndexStats();
        DocumentIndex fetchedIndex = DocumentIndex.getInstance();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while ( (entryDI = fetcher.loadDocEntry()) != null )
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());

        fetcher.end();
        assertEquals(correctVocabulary.getEntries(), testOutput.entrySet());
        assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @Test
    public void testBuildManyBlocks_BINARY(){

        CompressionType compression = CompressionType.BINARY;

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(Vocabulary.getInstance(), Dumper.getInstance(compression), DocumentIndex.getInstance());
        SPIMIIndex spimi = new SPIMIIndex(compression, ds, indexerSingleBlock);
        spimi.setLimit(1);
        spimi.buildIndexSPIMI(Constants.testPath);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.testPath);
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }
        int[] docIndexInfod = fetcher.getDocumentIndexStats();
        DocumentIndex fetchedIndex = DocumentIndex.getInstance();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();

        Assert.assertEquals(correctVocabulary.getEntries(), testOutput.entrySet());
        System.out.println(correctDocumentIndex);
        System.out.println(fetchedIndex);
        Assert.assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @Test
    public void testBuildSingleBlock_COMPRESSED(){

        CompressionType compression = CompressionType.COMPRESSED;
        Constants.setCompression(compression);

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(Vocabulary.getInstance(), Dumper.getInstance(compression), DocumentIndex.getInstance());
        new SPIMIIndex(compression, ds, indexerSingleBlock).buildIndexSPIMI(Constants.testPath);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.testPath);
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }
        fetcher.getDocumentIndexStats();
        DocumentIndex fetchedIndex = DocumentIndex.getInstance();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();

        System.out.println(testOutput.entrySet());
        System.out.println(correctVocabulary.getEntries());
        Assert.assertEquals(correctVocabulary.getEntries(), testOutput.entrySet());
        System.out.println(correctDocumentIndex);
        System.out.println(fetchedIndex);

        Assert.assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @Test
    public void testBuildManyBlocks_COMPRESSED(){

        CompressionType compression = CompressionType.COMPRESSED;
        Constants.setCompression(compression);

        // Dumping
        indexerSingleBlock = new InMemoryIndexing(Vocabulary.getInstance(), Dumper.getInstance(compression), DocumentIndex.getInstance());
        SPIMIIndex spimi = new SPIMIIndex(compression, ds, indexerSingleBlock);
        spimi.setLimit(1);
        spimi.buildIndexSPIMI(Constants.testPath);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.testPath);
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }
        fetcher.getDocumentIndexStats();
        DocumentIndex fetchedIndex = DocumentIndex.getInstance();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.end();

        Assert.assertEquals(correctVocabulary.getEntries(), testOutput.entrySet());
        Assert.assertEquals(correctDocumentIndex, fetchedIndex);
    }




    @After
    public void flush() {
        IOUtils.deleteDirectory(Constants.testPath);
    }
}
