package it.unipi.io;

import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.PostingListCompressed;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

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
    public void test_first_entry() throws IOException {
        Vocabulary voc = new VocabularyImpl();
        String testTerm = "test";

        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 2);

        VocabularyEntry entry = voc.getEntry(testTerm);
        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(testTerm, entry);
        dumper.start("./data/test/");
        dumper.dumpVocabulary(voc);
        dumper.end();

        fetcher.start("./data/test/");
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input, output);
    }

    @Test
    public void test_generic_entry() throws IOException {
        String term = "test";
        PostingList p;
        if(!Constants.getCompression())
            p = new PostingListImpl();
        else
            p = new PostingListCompressed();
        p.addPosting(1, 1);
        p.addPosting(2, 2);
        VocabularyEntry i = new VocabularyEntry(2, 0.0, p);

        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(term, i);
        dumper.start("./data/test/");
        dumper.dumpVocabularyEntry(input);
        dumper.end();

        fetcher.start("./data/test/");
        Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
        fetcher.end();

        assertEquals(input, output);
    }

    @Test
    public void test_specific_entry() throws IOException{
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

    @After
    public void flush(){
        for (File file : Objects.requireNonNull(new File("./data/test").listFiles()))
            if (!file.isDirectory())
                file.delete();
    }
}
