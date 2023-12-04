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
        vocDumped.addEntry("c",2);

        dumper.start("./data/test/");
        dumper.dumpVocabulary(vocDumped);
        dumper.end();

        vocFetched = new VocabularyImpl();
        fetcher.start("./data/test/");
        for(int i=0; i<3; i++){
            Map.Entry<String, VocabularyEntry> output = fetcher.loadVocEntry();
            vocFetched.setEntry(output.getKey(), output.getValue());
        }
        fetcher.end();

        TokenizerImpl tokenizer = new TokenizerImpl();
        maxScore = new MaxScore(vocFetched, tokenizer);
    }

    @Test
    public void testVocabularies(){
        assertEquals(vocDumped, vocFetched);
    }

    @Test
    public void testMaxScore1(){
        int numResults = 10;
        PriorityQueue<MaxScore.DocumentScore> results = maxScore.score("a", numResults);

        assert results.peek() != null;
        assertEquals(1, results.peek().docId);
        assertEquals(Math.log10(2), Objects.requireNonNull(results.poll()).score, 0.1);

        assert results.peek() != null;
        assertEquals(2, results.peek().docId);
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