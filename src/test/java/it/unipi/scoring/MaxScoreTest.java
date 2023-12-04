package it.unipi.scoring;

import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.utils.*;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class MaxScoreTest {
    Dumper dumper;
    Fetcher fetcher;

    MaxScore maxScore;

    @Before
    public void setup(){
        dumper = new DumperCompressed();
        fetcher = new FetcherCompressed();
        Constants.setCompression(true);
    }

    @Test
    public void boh(){
        Vocabulary voc = new VocabularyImpl();
        voc.addEntry("a", 1);
        voc.addEntry("b",1);
        voc.addEntry("a", 2);
        voc.addEntry("c",2);

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
