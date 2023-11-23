package it.unipi.scoring;

import junit.framework.TestCase;

import static org.junit.Assert.assertArrayEquals;

public class ScorerTest extends TestCase {

    public void testScore() {
        /*
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
         */
    }
}