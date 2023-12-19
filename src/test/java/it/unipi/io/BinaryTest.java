package it.unipi.io;
import it.unipi.encoding.CompressionType;
import it.unipi.io.implementation.DumperCompressed;
import it.unipi.io.implementation.FetcherCompressed;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.*;
import org.junit.*;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class BinaryTest {

    private static Dumper dumper;
    private static Fetcher fetcher;

    @BeforeClass
    public static void setup() {
        dumper = new DumperCompressed();
        fetcher = new FetcherCompressed();

        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Constants.testPath);
    }

    @Test
    public void testFirstEntry() {
        Vocabulary voc = Vocabulary.getInstance();
        String testTerm = "test";

        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 2);
        voc.addEntry(testTerm, 2);

        VocabularyEntry entry = voc.getEntry(testTerm);
        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        VocabularyEntry testEntry = output.getValue();
        fetcher.end();

        assertEquals(testTerm, output.getKey());
        assertEquals(entry, testEntry);
        //assertEquals(entry.getDocumentFrequency(), testEntry.getDocumentFrequency());
        //assertEquals(entry.getPostingList().docId(), testEntry.getPostingList().docId());
    }


    @Test
    public void testGenericEntry() throws IOException {
        String testTerm = "teseo";
        VocabularyEntry entry = new VocabularyEntry();

        entry.addPosting(1, 1);
        entry.addPosting(2, 2);
        entry.setUpperBound(2.2);

        NavigableMap<String, VocabularyEntry> tree = new TreeMap<>();
        tree.put(testTerm, entry);
        Map.Entry<String, VocabularyEntry> input = tree.entrySet().iterator().next();
        dumper.start();
        dumper.dumpVocabularyEntry(input);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());
        assertEquals(input.getValue(), output.getValue());
    }

    @Test
    public void testSpecificEntry() {
        Vocabulary voc = Vocabulary.getInstance();
        String testTerm = "test";
        String testTerm2 = "dog";

        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 2);
        voc.addEntry(testTerm2, 3);

        VocabularyEntry entry = voc.getEntry(testTerm2);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(testTerm2, entry);
        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        VocabularyEntry output = fetcher.loadVocEntry("dog");
        fetcher.end();


        assertEquals(input.getValue(), output);
    }
    @Test
    public void testNext2dot5Blocks() {
        Constants.BLOCK_SIZE=2;
        Vocabulary voc = Vocabulary.getInstance();
        String test = "test";
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);
        voc.addEntry(test,3);
        voc.addEntry(test,4);
        voc.addEntry(test,5);

        VocabularyEntry entry = voc.getEntry(test);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());
        assertEquals(input.getValue(), output.getValue());
    }

    @Test
    public void testNextNotPresent() {
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = Vocabulary.getInstance();
        String test = "test";
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);

        VocabularyEntry entry = voc.getEntry(test);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());

        PostingList plInput = entry.getPostingList();
        PostingList plOutput = output.getValue().getPostingList();
        try {
            plInput.next();
            plInput.next();
            plOutput.next();
            plOutput.next();
        } catch (EOFException e){
            e.printStackTrace();
        }

        Assert.assertThrows(EOFException.class, plInput::next);
        Assert.assertThrows(EOFException.class, plOutput::next);
    }

    @Test
    public void testNextNotPresentOffsets() {
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = Vocabulary.getInstance();
        String test = "test";
        String test2= "test2";
        voc.addEntry(test2, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);

        VocabularyEntry entry = voc.getEntry(test2);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test2, entry);

        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry(); // carica test2
        output = fetcher.loadVocEntry();                                    // carica test
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());

        PostingList plInput = entry.getPostingList();
        PostingList plOutput = output.getValue().getPostingList();
        try {
            plInput.next();
            plOutput.next();
        } catch (EOFException e){
            e.printStackTrace();
        }

        Assert.assertThrows(EOFException.class, plInput::next);
        Assert.assertThrows(EOFException.class, plOutput::next);
    }

    @Test
    public void testNextGEQSameBlock(){
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = Vocabulary.getInstance();
        String test = "test";
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);
        voc.addEntry(test,3);
        voc.addEntry(test,4);
        voc.addEntry(test,5);
        voc.addEntry(test,6);
        voc.addEntry(test,7);
        voc.addEntry(test,8);
        voc.addEntry(test,9);
        voc.addEntry(test, 540);

        VocabularyEntry entry = voc.getEntry(test);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());

        PostingList plInput = entry.getPostingList();
        PostingList plOutput = output.getValue().getPostingList();

        try {
            plInput.nextGEQ(2);
            plOutput.nextGEQ(2);
        } catch (EOFException eofException) {
            eofException.printStackTrace();
        }
        assertEquals(2,plOutput.docId());
    }

    @Test
    public void testNextGEQDiffBlock(){
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = Vocabulary.getInstance();
        String test = "test";
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);
        voc.addEntry(test,3);
        voc.addEntry(test,4);
        voc.addEntry(test,5);
        voc.addEntry(test,6);
        voc.addEntry(test,7);
        voc.addEntry(test,8);
        voc.addEntry(test,9);
        voc.addEntry(test, 540);

        VocabularyEntry entry = voc.getEntry(test);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());

        PostingList plInput = entry.getPostingList();
        PostingList plOutput = output.getValue().getPostingList();

        try{
        plInput.nextGEQ(8);
        plOutput.nextGEQ(8);
        } catch (EOFException eofException){
            eofException.printStackTrace();
        }
        assertEquals(8,plOutput.docId());

    }
    @Test
    public void testNextGEQLast(){
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = Vocabulary.getInstance();
        String test = "test";
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);
        voc.addEntry(test,3);
        voc.addEntry(test,4);
        voc.addEntry(test,5);
        voc.addEntry(test,6);
        voc.addEntry(test,7);
        voc.addEntry(test,8);
        voc.addEntry(test,9);
        voc.addEntry(test, 540);

        VocabularyEntry entry = voc.getEntry(test);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());

        PostingList plInput = entry.getPostingList();
        PostingList plOutput = output.getValue().getPostingList();

        try{
        plInput.nextGEQ(18);
        plOutput.nextGEQ(18);
        } catch (EOFException eofException){
            eofException.printStackTrace();
        }
        assertEquals( 540, plOutput.docId());
    }

    @Test
    public void testNextGEQNotPresent(){
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = Vocabulary.getInstance();
        String test = "test";
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);
        voc.addEntry(test,3);
        voc.addEntry(test,4);
        voc.addEntry(test,5);
        voc.addEntry(test,6);
        voc.addEntry(test,7);
        voc.addEntry(test,8);
        voc.addEntry(test,9);
        voc.addEntry(test, 540);

        VocabularyEntry entry = voc.getEntry(test);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

        dumper.start();
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start();
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());

        PostingList plInput = entry.getPostingList();
        PostingList plOutput = output.getValue().getPostingList();
        Assert.assertThrows(EOFException.class, () -> {
            plInput.nextGEQ(600);
        });
        Assert.assertThrows(EOFException.class, () -> {
            plOutput.nextGEQ(600);
        });
    }

    @Test
    public void completeTest() throws IOException {
        // parameters
        String testTerm = "test";
        Random random = new Random();
        int numTimes = 10;
        List<Integer> list = new ArrayList<>();
        int lowerBound = 1;
        int upperBound = 8800000;
        int numDocs = 10000;

        // tests
        for(int i=0; i<numTimes; i++){
            int numDocIds = random.nextInt(numDocs) + 1;
            for(int j=0; j<numDocIds; j++){
                int randomValue = lowerBound + random.nextInt(upperBound);
                list.add(randomValue);
            }
            Collections.sort(list);

            // vocabulary entry creation
            VocabularyEntry entry = new VocabularyEntry();
            for (int num: list)
                entry.addPosting(num, num);

            NavigableMap<String, VocabularyEntry> tree = new TreeMap<>();
            tree.put(testTerm, entry);
            Map.Entry<String, VocabularyEntry> input = tree.entrySet().iterator().next();

            // dump
            dumper.start(Constants.testPath);
            dumper.dumpVocabularyEntry(input);
            dumper.end();

            // fetch
            fetcher.start(Constants.testPath);
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            fetcher.end();

            // assert
            assertEquals(input.getKey(), output.getKey());
            assertEquals(input.getValue(), output.getValue());

            list.clear();
            IOUtils.deleteDirectory(Constants.testPath);
        }
    }

    @After
    public void flush() {
        IOUtils.deleteDirectory(Constants.testPath);
    }


}
