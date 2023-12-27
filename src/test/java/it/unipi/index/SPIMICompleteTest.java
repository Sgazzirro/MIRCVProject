package it.unipi.index;

import it.unipi.encoding.CompressionType;
import it.unipi.io.Fetcher;
import it.unipi.model.DocumentIndex;
import it.unipi.io.DocumentStream;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.Document;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.utils.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
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

    @Mock
    private static DocumentStream ds;

    private static Vocabulary correctVocabulary;
    private static DocumentIndex correctDocumentIndex;
    private final NavigableMap<String, VocabularyEntry> testOutput = new TreeMap<>();

    @BeforeClass
    public static void createResult() {
        Constants.setCompression(CompressionType.DEBUG);
        correctVocabulary = new Vocabulary();
        correctDocumentIndex = new DocumentIndex();

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
    public void setup() throws IOException {
        Constants.setPath(Constants.TEST_PATH);

        IOUtils.deleteDirectory(Constants.TEST_PATH);
        IOUtils.createDirectory(Constants.TEST_PATH);

        Constants.setCompression(CompressionType.DEBUG);
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
    public void testBuildInSingleBlock_DEBUG_NOTSPIMI() throws Exception {

        CompressionType compression = CompressionType.DEBUG;

        // Dumping
        InMemoryIndex indexerSingleBlock = new InMemoryIndex(compression);

        indexerSingleBlock.setup(Constants.TEST_PATH);
        indexerSingleBlock.buildIndex(ds);
        indexerSingleBlock.close();

        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.TEST_PATH);
        Map.Entry<String, VocabularyEntry> entry;

        while ( (entry = fetcher.loadVocEntry()) != null )
            testOutput.put(entry.getKey(), entry.getValue());

        DocumentIndex fetchedIndex = new DocumentIndex();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;

        fetcher.getDocumentIndexStats();      // Skip first two lines
        while((entryDI = fetcher.loadDocEntry()) != null)
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        fetcher.close();

        assertEquals(correctVocabulary.getMapping(), testOutput);
        assertEquals(correctDocumentIndex, fetchedIndex);
    }


    @Test
    public void testBuildSingleBlock_DEBUG() throws Exception {

        CompressionType compression = CompressionType.DEBUG;

        // Dumping
        new SPIMIIndex(compression, ds).buildIndex(Constants.TEST_PATH);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.TEST_PATH);
        Map.Entry<String, VocabularyEntry> entry;
        while ( (entry = fetcher.loadVocEntry()) != null )
            testOutput.put(entry.getKey(), entry.getValue());


        DocumentIndex fetchedIndex = new DocumentIndex();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;

        fetcher.getDocumentIndexStats();      // Skip first two lines
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.close();

        assertTrue(Objects.equals(correctVocabulary.getMapping(), testOutput) && Objects.equals(correctDocumentIndex, fetchedIndex));
    }

    @Test
    public void testBuildManyBlocks_DEBUG() throws Exception {

        CompressionType compression = CompressionType.DEBUG;

        // Dumping
        SPIMIIndex spimi = new SPIMIIndex(compression, ds);
        spimi.setLimit(0.00001);
        spimi.buildIndex(Constants.TEST_PATH);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.TEST_PATH);
        Map.Entry<String, VocabularyEntry> entry;
        while ( (entry = fetcher.loadVocEntry()) != null )
            testOutput.put(entry.getKey(), entry.getValue());

        DocumentIndex fetchedIndex = new DocumentIndex();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;

        fetcher.getDocumentIndexStats();      // Skip first two lines
        while ( (entryDI = fetcher.loadDocEntry()) != null )
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());

        fetcher.close();
        assertEquals(correctVocabulary.getMapping(), testOutput);
        assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @Test
    public void testBuildSingleBlock_BINARY() throws Exception {

        CompressionType compression = CompressionType.BINARY;

        // Dumping
        new SPIMIIndex(compression, ds).buildIndex(Constants.TEST_PATH);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.TEST_PATH);
        Map.Entry<String, VocabularyEntry> entry;
        while ( (entry = fetcher.loadVocEntry()) != null )
            testOutput.put(entry.getKey(), entry.getValue());

        fetcher.getDocumentIndexStats();
        DocumentIndex fetchedIndex = new DocumentIndex();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while ( (entryDI = fetcher.loadDocEntry()) != null )
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());

        fetcher.close();
        assertEquals(correctVocabulary.getMapping(), testOutput);
        assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @Test
    public void testBuildManyBlocks_BINARY() throws Exception {

        CompressionType compression = CompressionType.BINARY;

        // Dumping
        SPIMIIndex spimi = new SPIMIIndex(compression, ds);
        spimi.setLimit(1);
        spimi.buildIndex(Constants.TEST_PATH);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.TEST_PATH);
        Map.Entry<String, VocabularyEntry> entry;
        while((entry = fetcher.loadVocEntry()) != null){
            testOutput.put(entry.getKey(), entry.getValue());
        }

        fetcher.getDocumentIndexStats();
        DocumentIndex fetchedIndex = new DocumentIndex();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while((entryDI = fetcher.loadDocEntry()) != null){
            fetchedIndex.addDocument(entryDI.getKey(),entryDI.getValue().getDocumentLength());
        }
        fetcher.close();

        Assert.assertEquals(correctVocabulary.getMapping(), testOutput);
        Assert.assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @Test
    public void testBuildSingleBlock_COMPRESSED() throws Exception {

        CompressionType compression = CompressionType.COMPRESSED;
        Constants.setCompression(compression);

        // Dumping
        SPIMIIndex indexer = new SPIMIIndex(compression, ds);
        indexer.buildIndex(Constants.TEST_PATH);


        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.TEST_PATH);
        Map.Entry<String, VocabularyEntry> entry;
        while ( (entry = fetcher.loadVocEntry()) != null )
            testOutput.put(entry.getKey(), entry.getValue());

        fetcher.getDocumentIndexStats();
        DocumentIndex fetchedIndex = new DocumentIndex();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while ( (entryDI = fetcher.loadDocEntry()) != null )
            fetchedIndex.addDocument(entryDI.getKey(), entryDI.getValue().getDocumentLength());

        fetcher.close();

        Assert.assertEquals(correctVocabulary.getMapping(), testOutput);
        Assert.assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @Test
    public void testBuildManyBlocks_COMPRESSED() throws Exception {
        CompressionType compression = CompressionType.COMPRESSED;
        Constants.setCompression(compression);

        // Dumping
        SPIMIIndex spimi = new SPIMIIndex(compression, ds);
        spimi.setLimit(1);
        spimi.buildIndex(Constants.TEST_PATH);

        // Fetching
        Fetcher fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.TEST_PATH);
        Map.Entry<String, VocabularyEntry> entry;
        while ( (entry = fetcher.loadVocEntry() ) != null)
            testOutput.put(entry.getKey(), entry.getValue());

        fetcher.getDocumentIndexStats();
        DocumentIndex fetchedIndex = new DocumentIndex();
        Map.Entry<Integer, DocumentIndexEntry> entryDI;
        while ( (entryDI = fetcher.loadDocEntry()) != null )
            fetchedIndex.addDocument(entryDI.getKey(), entryDI.getValue().getDocumentLength());

        fetcher.close();

        Assert.assertEquals(correctVocabulary.getMapping(), testOutput);
        Assert.assertEquals(correctDocumentIndex, fetchedIndex);
    }

    @After
    public void flush() {
        IOUtils.deleteDirectory(Constants.TEST_PATH);
    }
}
