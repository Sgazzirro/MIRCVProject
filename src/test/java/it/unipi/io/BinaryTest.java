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
        new File("./test").mkdirs();
    }

    @Test
    public void testFirstEntry() throws IOException {
        Vocabulary voc = new VocabularyImpl();
        String testTerm = "test";

        voc.addEntry(testTerm, 1);
        voc.addEntry(testTerm, 1);
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
        assertEquals(entry.getDocumentFrequency(), testEntry.getDocumentFrequency());
        assertEquals(entry.getPostingList().docId(), testEntry.getPostingList().docId());
    }


    @Test
    public void testGenericEntry() throws IOException {
        String term = "test";
        PostingList p;
        if(!Constants.getCompression())
            p = new PostingListImpl();
        else
            p = new PostingListCompressed();
        p.addPosting(1, 1);
        p.addPosting(2, 2);
        VocabularyEntry i = new VocabularyEntry(2, 2.2, p);


        Map.Entry<String, VocabularyEntry> input = new AbstractMap.SimpleEntry<>(term, i);
        boolean opened = dumper.start("./data/test/");
        assertTrue(opened);

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



    @After
    public void flush() {
        try{
            FileUtils.deleteDirectory(new File("./data/test/"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public static void listDirectoryContent(String directoryPath) {
        try {
            // Create a Path object representing the directory
            Path dirPath = Paths.get(directoryPath);

            // Get a stream of all entries (files and sub-directories) in the directory
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath);

            // Iterate over the entries and print their names
            for (Path entry : directoryStream) {
                System.out.println(entry.getFileName());
            }

            // Close the directory stream
            directoryStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
