package it.unipi.scoring;

import it.unipi.index.InMemoryIndexing;
import it.unipi.model.implementation.DocumentIndex;
import it.unipi.model.implementation.DocumentStream;
import it.unipi.model.implementation.Tokenizer;
import it.unipi.model.implementation.Vocabulary;
import it.unipi.utils.Constants;
import junit.framework.TestCase;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

public class ScorerTest extends TestCase {
    public void testScore() {
        DocumentStream ds = new DocumentStream(Constants.TEST_COLLECTION_FILE);

        DocumentIndex documentIndex = new DocumentIndex();
        Vocabulary vocabulary = new Vocabulary();
        Tokenizer tokenizer = new Tokenizer(false);

        InMemoryIndexing inMemoryIndexing = new InMemoryIndexing(ds, documentIndex, vocabulary, tokenizer);
        inMemoryIndexing.buildIndex();

        Scorer scorer = new Scorer(vocabulary, documentIndex, tokenizer);

        String query = "beijing duck recipe";
        Scorer.DocumentScore[] scores  = scorer.score(query, 5);
        Integer[] docIds = Arrays.stream(scores).map(docScore -> docScore.docId).toArray(Integer[]::new);

        assertArrayEquals(docIds, new Integer[]{4, 1, 2, 3, 0});
    }
}