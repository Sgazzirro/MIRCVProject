package it.unipi.io;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.PostingListCompressed;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.*;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public void testNext2dot5Blocks(){
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




    @After
    public void flush() {
        try{
            FileUtils.deleteDirectory(new File("./data/test/"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
