package it.unipi.io;
import org.apache.commons.io.FileUtils;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.model.implementation.PostingListCompressed;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.*;
import org.junit.*;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BinaryTest {

    Dumper dumper;
    Fetcher fetcher;

    @Before
    public void setup(){
        dumper = new DumperCompressed();
        fetcher = new FetcherCompressed();
        Constants.setCompression(true);
    }

    @Test
    public void testFirstEntry() throws IOException {
        Vocabulary voc = new VocabularyImpl();
        String testTerm = "test";

        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 2);
        voc.addEntry(testTerm, 2);

        VocabularyEntry entry = voc.getEntry(testTerm);
        dumper.start("./data/test/");
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start("./data/test/");
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
        PostingList p;
        p = new PostingListImpl();

        if(!Constants.getCompression())
            p = new PostingListImpl();
        else
            p = new PostingListCompressed();

        p.addPosting(1, 1);
        p.addPosting(2, 2);
        VocabularyEntry i = new VocabularyEntry(2, 2.2, p);

        NavigableMap<String, VocabularyEntry> tree = new TreeMap<>();
        tree.put(testTerm, i);
        Map.Entry<String, VocabularyEntry> input = tree.entrySet().iterator().next();
        dumper.start("./data/test/");
        dumper.dumpVocabularyEntry(input);
        dumper.end();

        fetcher.start("./data/test/");
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());
        assertEquals(input.getValue(), output.getValue());
    }




    @Test
    public void testSpecificEntry() {
        Vocabulary voc = new VocabularyImpl();
        String testTerm = "test";
        String testTerm2 = "dog";

        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 2);
        voc.addEntry(testTerm2, 3);

        VocabularyEntry entry = voc.getEntry(testTerm2);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(testTerm2, entry);
        dumper.start("./data/test/");
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start("./data/test/");
        VocabularyEntry output = fetcher.loadVocEntry("dog");
        fetcher.end();


        assertEquals(input.getValue(), output);
    }
    @Test
    public void testNext2dot5Blocks() {
        Constants.BLOCK_SIZE=2;
        Vocabulary voc = new VocabularyImpl();
        String test = "test";
        voc.addEntry(test, 1);
        voc.addEntry(test, 1);
        voc.addEntry(test,2);
        voc.addEntry(test,3);
        voc.addEntry(test,4);
        voc.addEntry(test,5);

        VocabularyEntry entry = voc.getEntry(test);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(test, entry);

        dumper.start("./data/test/");
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start("./data/test/");
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());
        assertEquals(input.getValue(), output.getValue());
    }
    @Test
    public void testNextGEQSameBlock(){
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = new VocabularyImpl();
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

        dumper.start("./data/test/");
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start("./data/test/");
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input.getKey(), output.getKey());

        PostingList plInput = entry.getPostingList();
        PostingList plOutput = output.getValue().getPostingList();

        try {
            plInput.nextGEQ(2);
            plOutput.nextGEQ(2);
        } catch (EOFException eofException){
            eofException.printStackTrace();
        }
        assertEquals(2,plOutput.docId());
    }

    @Test
    public void testNextGEQDiffBlock(){
        Constants.BLOCK_SIZE=3;
        Vocabulary voc = new VocabularyImpl();
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

        dumper.start("./data/test/");
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start("./data/test/");
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
        Vocabulary voc = new VocabularyImpl();
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

        dumper.start("./data/test/");
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start("./data/test/");
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
        Vocabulary voc = new VocabularyImpl();
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

        dumper.start("./data/test/");
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start("./data/test/");
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

    @After
    public void flush() {
        try{
            FileUtils.deleteDirectory(new File("./data/test/"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
