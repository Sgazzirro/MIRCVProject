package it.unipi.model;

import it.unipi.encoding.CompressionType;
import it.unipi.io.Fetcher;
import it.unipi.utils.Constants;
import opennlp.tools.stemmer.PorterStemmer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * This test assumes that the whole collection
 * has been previously indexed
 */
@RunWith(Parameterized.class)
public class PostingListCollectionTest {

    // Test over different compression types
    @Parameterized.Parameters
    public static Collection<Object[]> terms() {
        PorterStemmer stemmer = new PorterStemmer();
        String[] terms = new String[] {
                "car",
                "future",
                "inquisition"
        };

        return Arrays.stream(terms)
                .map(s -> new Object[]{stemmer.stem(s)})
                .toList();
    }

    private static Fetcher fetcher;

    private final VocabularyEntry entry;
    private final PostingList postingList;

    public PostingListCollectionTest(String term) throws IOException {
        fetcher = Fetcher.getFetcher(CompressionType.COMPRESSED);
        fetcher.start();

        entry = fetcher.loadVocEntry(term);
        postingList = entry.getPostingList();
    }

    @BeforeClass
    public static void setUp() {
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Constants.dataPath);

        fetcher = Fetcher.getFetcher(CompressionType.COMPRESSED);
        fetcher.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        fetcher.close();
    }

    @Test
    public void testLength() throws EOFException {
        int length = entry.getDocumentFrequency();

        int cont = 0;
        while (postingList.hasNext()) {
            postingList.next();
            cont++;
        }

        assertEquals(length, cont);
    }

}