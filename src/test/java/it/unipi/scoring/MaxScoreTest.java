package it.unipi.scoring;

import it.unipi.model.Tokenizer;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.TokenizerImpl;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.*;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MaxScoreTest {

    Vocabulary vocDumped;
    VocabularyImpl vocFetched;
    MaxScore maxScore;

    @Before
    public void setup(){
        Dumper dumper = new DumperCompressed();
        Fetcher fetcher = new FetcherCompressed();
        Constants.setCompression(true);

        vocDumped = new VocabularyImpl();
        vocDumped.addEntry("a", 1);
        vocDumped.addEntry("b",1);
        vocDumped.addEntry("a", 2);
        vocDumped.addEntry("c",(int)Math.pow(2,30));

        dumper.start(Constants.testPath);
        dumper.dumpVocabulary(vocDumped);
        dumper.end();

        vocFetched = new VocabularyImpl();
        fetcher.start(Constants.testPath);
        for(int i=0; i<3; i++){
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            vocFetched.setEntry(output.getKey(), output.getValue());
        }
        fetcher.end();

        TokenizerImpl tokenizer = new TokenizerImpl();
        maxScore = new MaxScore(vocFetched, tokenizer);

        // setting idfs by hand
        vocFetched.getEntry("a").getPostingList().setIdf(Math.log10(2/2));
        vocFetched.getEntry("b").getPostingList().setIdf(Math.log10(2/1));
        vocFetched.getEntry("c").getPostingList().setIdf(Math.log10(2/1));
        vocDumped.getEntry("a").getPostingList().setIdf(Math.log10(2/2));
        vocDumped.getEntry("b").getPostingList().setIdf(Math.log10(2/1));
        vocDumped.getEntry("c").getPostingList().setIdf(Math.log10(2/1));

        // setting upper bounds by hand
        vocFetched.getEntry("a").setUpperBound(0.0);
        vocFetched.getEntry("b").setUpperBound(Math.log10(2));
        vocFetched.getEntry("c").setUpperBound(Math.log10(2));
        vocDumped.getEntry("a").setUpperBound(0.0);
        vocDumped.getEntry("b").setUpperBound(Math.log10(2));
        vocDumped.getEntry("c").setUpperBound(Math.log10(2));
    }

    @Test
    public void testVocabularies(){
        assertEquals(vocDumped, vocFetched);
    }

    @Test
    public void testMaxScore1(){
        int numResults = 10;
        PriorityQueue<DocumentScore> results = maxScore.score("a", numResults);

        // del secondo documento nemmeno faccio lo score, perchè so già che non potrà raggiungere la threshold
        assertEquals(1, results.size());

        assert results.peek() != null;
        assertEquals(1, results.peek().docId);
        assertEquals(0, Objects.requireNonNull(results.poll()).score, 0.1);
    }

    @Test
    public void testMaxScore2(){
        int numResults = 10;
        PriorityQueue<DocumentScore> results = maxScore.score("b", numResults);

        assertEquals(1, results.size());

        assert results.peek() != null;
        assertEquals(1, results.peek().docId);
        assertEquals(Math.log10(2), Objects.requireNonNull(results.poll()).score, 0.1);
    }

    @Test
    public void testMaxScore3(){
        int numResults = 10;
        PriorityQueue<DocumentScore> results = maxScore.score("c", numResults);

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
