package it.unipi.scoring;

import it.unipi.index.InMemoryIndexing;
import it.unipi.model.implementation.DocumentIndexImpl;
import it.unipi.model.implementation.DocumentStreamImpl;
import it.unipi.model.implementation.TokenizerImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.Constants;
import junit.framework.TestCase;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

public class ScorerTest extends TestCase {
    public void testScore() {
        DocumentStreamImpl ds = new DocumentStreamImpl(Constants.TEST_COLLECTION_FILE);

        DocumentIndexImpl documentIndexImpl = new DocumentIndexImpl();
        VocabularyImpl vocabularyImpl = new VocabularyImpl();
        TokenizerImpl tokenizerImpl = new TokenizerImpl(false);

        InMemoryIndexing inMemoryIndexing = new InMemoryIndexing(ds, documentIndexImpl, vocabularyImpl, tokenizerImpl);
        inMemoryIndexing.buildIndex();

        Scorer scorer = new Scorer(vocabularyImpl, documentIndexImpl, tokenizerImpl);

        String query = "beijing duck recipe";
        Scorer.DocumentScore[] scores  = scorer.score(query, 5);
        Integer[] docIds = Arrays.stream(scores).map(docScore -> docScore.docId).toArray(Integer[]::new);

        assertArrayEquals(docIds, new Integer[]{4, 1, 2, 3, 0});
    }
}