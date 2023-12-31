package it.unipi.model;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Encoder;
import it.unipi.encoding.EncodingType;
import it.unipi.encoding.Tokenizer;
import it.unipi.encoding.implementation.Simple9;
import it.unipi.encoding.implementation.UnaryEncoder;
import it.unipi.io.Fetcher;
import it.unipi.scoring.Scorer;
import it.unipi.scoring.ScoringType;
import it.unipi.utils.Constants;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
        Tokenizer tokenizer = Tokenizer.getInstance();
        String[] terms = new String[] {
                "car",
                "future",
                "inquisition"
        };

        return Arrays.stream(terms)
                .map(s -> new Object[]{tokenizer.tokenize(s).get(0)})
                .toList();
    }

    private static Fetcher fetcher;

    private final VocabularyEntry entry;
    private final PostingList postingList;

    public PostingListCollectionTest(String term) throws IOException {
        fetcher = Fetcher.getFetcher(CompressionType.COMPRESSED);
        fetcher.start(Constants.getPath());

        int[] info = fetcher.getDocumentIndexStats();
        Constants.N = info[0];

        entry = fetcher.loadVocEntry(term);
        postingList = entry.getPostingList();
    }

    @Test
    public void testLength() {
        int length = entry.getDocumentFrequency();

        int cont = 0;
        while (postingList.hasNext()) {
            postingList.next();
            cont++;
        }

        assertEquals(length, cont);
    }

    @Test
    public void testDocIds() {
        int curr, prev = -1;

        while (postingList.hasNext()) {
            postingList.next();
            curr = postingList.docId();
            assertTrue(curr > prev);

            prev = curr;
        }
    }

    @Test
    public void testTermFrequencies() {
        int tf;

        while (postingList.hasNext()) {
            postingList.next();
            tf = postingList.termFrequency();

            assertTrue(tf > 0);
            assertTrue(tf < Constants.N);
        }
    }

    @Test
    public void testUpperBound() {
        double upperBound = 0;
        Scorer scorer = Scorer.getScorer(new DocumentIndex());      // Only works with TFIDF

        postingList.reset();
        while (postingList.hasNext()) {
            postingList.next();
            upperBound = Math.max(upperBound, scorer.score(postingList));
        }

        assertEquals(upperBound, entry.getUpperBound(), 1e-6);
    }

    @After
    public void reset() {
        postingList.reset();
    }

    @BeforeClass
    public static void setUp() {
        Constants.setScoring(ScoringType.TFIDF);
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Constants.DATA_PATH);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        fetcher.close();
    }
}