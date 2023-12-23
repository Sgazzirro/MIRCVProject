package it.unipi.scoring;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.model.DocumentIndex;
import it.unipi.model.Vocabulary;
import it.unipi.utils.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.PriorityQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MaxScoreTest {

    Vocabulary vocabulary;
    DocumentIndex documentIndex;
    MaxScore maxScore;

    @Before
    public void setup(){
        Constants.setCompression(CompressionType.DEBUG);
        Constants.N = 2;

        vocabulary = new Vocabulary();
        vocabulary.addEntry("a", 1);
        vocabulary.addEntry("b",1);
        vocabulary.addEntry("a", 2);
        vocabulary.addEntry("c", (int) Math.pow(2,30));

        documentIndex = new DocumentIndex();
        documentIndex.addDocument(1, 2);
        documentIndex.addDocument(2, 1);
        documentIndex.addDocument((int) Math.pow(2, 30), 1);

        Tokenizer tokenizer = Tokenizer.getInstance(false, false);
        maxScore = new MaxScore(vocabulary, documentIndex, tokenizer);

        // setting upper bounds by hand
        vocabulary.getEntry("a").setUpperBound(0.0);
        vocabulary.getEntry("b").setUpperBound(Math.log10(2));
        vocabulary.getEntry("c").setUpperBound(Math.log10(2));
    }

    @Test
    public void testMaxScore1(){
        int numResults = 2;
        PriorityQueue<DocumentScore> results = maxScore.score("a", numResults, "disjunctive");
        assertEquals(2, results.size());

        DocumentScore documentScore2 = results.poll();
        DocumentScore documentScore1 = results.poll();
        assertNotNull(documentScore1);
        assertNotNull(documentScore2);

        assertEquals(2, documentScore1.docId());
        assertEquals(1, documentScore2.docId());
        assertEquals(0, documentScore1.score(), 0.1);
        assertEquals(0, documentScore2.score(), 0.1);
    }

    @Test
    public void testMaxScore2(){
        int numResults = 10;
        PriorityQueue<DocumentScore> results = maxScore.score("b", numResults, "disjunctive");

        assertEquals(1, results.size());

        assert results.peek() != null;
        assertEquals(1, results.peek().docId());
        assertEquals(Math.log10(2), Objects.requireNonNull(results.poll()).score(), 0.1);
    }

    @Test
    public void testMaxScore3(){
        int numResults = 10;
        PriorityQueue<DocumentScore> results = maxScore.score("c", numResults, "disjunctive");

        assertEquals(1, results.size());

        assert results.peek() != null;
        assertEquals((int)Math.pow(2,30), results.peek().docId());
        assertEquals(Math.log10(2), Objects.requireNonNull(results.poll()).score(), 0.1);
    }
}
