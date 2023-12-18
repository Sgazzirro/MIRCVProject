package it.unipi.scoring;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.model.Vocabulary;
import it.unipi.utils.*;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.PriorityQueue;

import static org.junit.Assert.assertEquals;

public class MaxScoreTest {

    Vocabulary vocabulary;
    MaxScore maxScore;

    @Before
    public void setup(){
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.N = 2;

        vocabulary = Vocabulary.getInstance();
        vocabulary.addEntry("a", 1);
        vocabulary.addEntry("b",1);
        vocabulary.addEntry("a", 2);
        vocabulary.addEntry("c", (int)Math.pow(2,30));

        Tokenizer tokenizer = Tokenizer.getInstance(false, false);
        maxScore = new MaxScore(vocabulary, tokenizer);

        // setting idfs by hand
        vocabulary.getEntry("a").getPostingList().setIdf(Math.log10(2/2));
        vocabulary.getEntry("b").getPostingList().setIdf(Math.log10(2/1));
        vocabulary.getEntry("c").getPostingList().setIdf(Math.log10(2/1));

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

        assert results.peek() != null;
        assertEquals(1, results.peek().docId);
        assertEquals(0, Objects.requireNonNull(results.poll()).score, 0.1);
        assertEquals(2, results.peek().docId);
        assertEquals(0, Objects.requireNonNull(results.poll()).score, 0.1);
    }

    @Test
    public void testMaxScore2(){
        int numResults = 10;
        PriorityQueue<DocumentScore> results = maxScore.score("b", numResults, "disjunctive");

        assertEquals(1, results.size());

        assert results.peek() != null;
        assertEquals(1, results.peek().docId);
        assertEquals(Math.log10(2), Objects.requireNonNull(results.poll()).score, 0.1);
    }

    @Test
    public void testMaxScore3(){
        int numResults = 10;
        PriorityQueue<DocumentScore> results = maxScore.score("c", numResults, "disjunctive");

        assertEquals(1, results.size());

        assert results.peek() != null;
        assertEquals((int)Math.pow(2,30), results.peek().docId);
        assertEquals(Math.log10(2), Objects.requireNonNull(results.poll()).score, 0.1);
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
