package it.unipi.io;
import it.unipi.encoding.CompressionType;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.*;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class BinaryTest {

    private static final Logger logger = LoggerFactory.getLogger(BinaryTest.class);

    private static Dumper dumper;
    private static Fetcher fetcher;

    @BeforeClass
    public static void setup() {
        dumper = Dumper.getInstance(CompressionType.COMPRESSED);
        fetcher = Fetcher.getFetcher(CompressionType.COMPRESSED);

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
        try {
            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            VocabularyEntry testEntry = output.getValue();
            fetcher.close();

            assertEquals(testTerm, output.getKey());
            assertEquals(entry, testEntry);

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
    }


    @Test
    public void testGenericEntry() {
        String testTerm = "teseo";
        VocabularyEntry entry = new VocabularyEntry();

        entry.addPosting(1, 1);
        entry.addPosting(2, 2);
        entry.setUpperBound(2.2);

        try {
            NavigableMap<String, VocabularyEntry> tree = new TreeMap<>();
            tree.put(testTerm, entry);
            Map.Entry<String, VocabularyEntry> input = tree.entrySet().iterator().next();
            dumper.start();
            dumper.dumpVocabularyEntry(input);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            fetcher.close();

            assertEquals(input.getKey(), output.getKey());
            assertEquals(input.getValue(), output.getValue());

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
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

        try {
            VocabularyEntry entry = voc.getEntry(testTerm2);
            Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(testTerm2, entry);
            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            VocabularyEntry output = fetcher.loadVocEntry("dog");
            fetcher.close();

            assertEquals(input.getValue(), output);

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
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

        try {
            VocabularyEntry entry = voc.getEntry(test);
            Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            fetcher.close();

            assertEquals(input.getKey(), output.getKey());
            assertEquals(input.getValue(), output.getValue());

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
    }

    @Test
    public void testNextNotPresent() {
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = Vocabulary.getInstance();
        String test = "test";
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);

        try {
            VocabularyEntry entry = voc.getEntry(test);
            Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            fetcher.close();

            assertEquals(input.getKey(), output.getKey());

            PostingList plInput = entry.getPostingList();
            PostingList plOutput = output.getValue().getPostingList();
            plInput.next();
            plInput.next();
            plOutput.next();
            plOutput.next();

            Assert.assertThrows(NoSuchElementException.class, plInput::next);
            Assert.assertThrows(NoSuchElementException.class, plOutput::next);

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
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

        try {
            VocabularyEntry entry = voc.getEntry(test2);
            Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test2, entry);

            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output;
                     fetcher.loadVocEntry();            // carica test2
            output = fetcher.loadVocEntry();            // carica test
            fetcher.close();

            assertEquals(input.getKey(), output.getKey());

            PostingList plInput = entry.getPostingList();
            PostingList plOutput = output.getValue().getPostingList();
            plInput.next();
            plOutput.next();

            Assert.assertThrows(NoSuchElementException.class, plInput::next);
            Assert.assertThrows(NoSuchElementException.class, plOutput::next);

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
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

        try {
            VocabularyEntry entry = voc.getEntry(test);
            Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            fetcher.close();

            assertEquals(input.getKey(), output.getKey());

            PostingList plInput = entry.getPostingList();
            PostingList plOutput = output.getValue().getPostingList();

            plInput.nextGEQ(2);
            plOutput.nextGEQ(2);

            assertEquals(2,plOutput.docId());

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
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

        try {
            VocabularyEntry entry = voc.getEntry(test);
            Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            fetcher.close();

            assertEquals(input.getKey(), output.getKey());

            PostingList plInput = entry.getPostingList();
            PostingList plOutput = output.getValue().getPostingList();

            plInput.nextGEQ(8);
            plOutput.nextGEQ(8);
            assertEquals(8,plOutput.docId());

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
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

        try {
            VocabularyEntry entry = voc.getEntry(test);
            Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            fetcher.close();

            assertEquals(input.getKey(), output.getKey());

            PostingList plInput = entry.getPostingList();
            PostingList plOutput = output.getValue().getPostingList();

            plInput.nextGEQ(18);
            plOutput.nextGEQ(18);
            assertEquals( 540, plOutput.docId());

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
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

        try {
            VocabularyEntry entry = voc.getEntry(test);
            Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

            dumper.start();
            dumper.dumpVocabulary(voc);
            dumper.close();

            fetcher.start();
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            fetcher.close();

            assertEquals(input.getKey(), output.getKey());

            PostingList plInput = entry.getPostingList();
            PostingList plOutput = output.getValue().getPostingList();
            Assert.assertThrows(NoSuchElementException.class, () ->
                plInput.nextGEQ(600)
            );
            Assert.assertThrows(NoSuchElementException.class, () ->
                plOutput.nextGEQ(600)
            );

        } catch (Exception e) {
            logger.error("Error in test", e);
        }
    }

    @Test
    public void completeTest() {
        // parameters
        String testTerm = "test";
        Random random = new Random();
        int numTimes = 10;
        List<Integer> list = new ArrayList<>();
        int lowerBound = 1;
        int upperBound = 8800000;
        int numDocs = 10000;

        // tests
        for (int i=0; i<numTimes; i++) {
            int numDocIds = random.nextInt(numDocs) + 1;
            for (int j=0; j<numDocIds; j++) {
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

            try {
                // dump
                dumper.start(Constants.testPath);
                dumper.dumpVocabularyEntry(input);
                dumper.close();

                // fetch
                fetcher.start(Constants.testPath);
                Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
                fetcher.close();

                // assert
                assertEquals(input.getKey(), output.getKey());
                assertEquals(input.getValue(), output.getValue());

            } catch (Exception e) {
                logger.error("Error in test", e);
            }

            list.clear();
            IOUtils.deleteDirectory(Constants.testPath);
        }
    }

    @After
    public void flush() {
        IOUtils.deleteDirectory(Constants.testPath);
    }


}
