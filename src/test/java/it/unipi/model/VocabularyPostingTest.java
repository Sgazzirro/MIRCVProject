package it.unipi.model;

import it.unipi.encoding.CompressionType;
import it.unipi.io.Dumper;
import it.unipi.io.Fetcher;
import it.unipi.utils.Constants;
import it.unipi.utils.IOUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class VocabularyPostingTest {

    // Test over different compression types
    @Parameterized.Parameters
    public static Collection<Object[]> compressionTypes() {
        return Arrays.asList(new Object[][] {
                {CompressionType.DEBUG},
                {CompressionType.BINARY},
                {CompressionType.COMPRESSED}
        });
    }

    @BeforeClass
    public static void setUp() {
        Constants.setPath(Constants.testPath);
    }

    private final Fetcher fetcher;

    private static final int LENGTH = 2;

    public VocabularyPostingTest(CompressionType compression) throws Exception {
        Constants.setCompression(compression);

        VocabularyEntry entry = new VocabularyEntry();
        PostingList correctPostingList = PostingList.getInstance(compression, entry);
        for (int i = 0; i < LENGTH; i++)
            correctPostingList.addPosting(i, i+1);

        Dumper dumper = Dumper.getInstance(compression);
        dumper.start(Constants.testPath);
        dumper.dumpVocabularyEntry(new AbstractMap.SimpleEntry<>("test1", entry));
        dumper.dumpVocabularyEntry(new AbstractMap.SimpleEntry<>("test2", entry));
        dumper.close();

        fetcher = Fetcher.getFetcher(compression);
        fetcher.start(Constants.testPath);
    }

    @Test
    public void testEntries() throws IOException {
        Map.Entry<String, VocabularyEntry> entry = fetcher.loadVocEntry();
        assertEquals(entry.getKey(), "test1");

        testPostingList(entry.getValue().getPostingList());

        entry = fetcher.loadVocEntry();
        assertEquals(entry.getKey(), "test2");

        testPostingList(entry.getValue().getPostingList());
    }

    private void testPostingList(PostingList postingList) {
        List<Integer> docIdsList = postingList.getDocIdsList();
        List<Integer> termFreqList = postingList.getTermFrequenciesList();

        assertEquals(docIdsList.size(), LENGTH);
        assertEquals(termFreqList.size(), LENGTH);
    }

    @After
    public void cleanUp() {
        IOUtils.deleteDirectory(Constants.testPath);
    }
}
