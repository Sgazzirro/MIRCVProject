package it.unipi.utils;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.implementation.TokenizerImpl;
import it.unipi.scoring.DocumentScore;
import it.unipi.scoring.MaxScore;
import it.unipi.scoring.ScoringType;
import opennlp.tools.parser.Cons;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Timing {

    public static void main(String[] args){
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Path.of("./data"));
        Constants.setScoring(ScoringType.TFIDF);
        long start, end;

        Constants.CACHING = false;
        Constants.startSession();

        try(
                BufferedReader readerQ = new BufferedReader(new FileReader("./data/evaluation/msmarco-test2019-queries.tsv"));
                ){
            String query;


            start = System.currentTimeMillis();
            while((query = readerQ.readLine()) != null){
                MaxScore scorer = new MaxScore(Constants.vocabulary, Constants.documentIndex, new TokenizerImpl(true, true));
                PriorityQueue<DocumentScore> scoring = scorer.score(query.split("\t")[1], 300, "disjunctive");
                Constants.startSession();
                System.gc();
            }
            end = System.currentTimeMillis();

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        Constants.onExit();

        System.out.println("WITHOUT CACHE : " + (end - start));

        // ------------------------------------------------------------------------------------------------------------------------------

        Constants.CACHING = true;
        Constants.startSession();

        try(
                BufferedReader readerQ = new BufferedReader(new FileReader("./data/evaluation/msmarco-test2019-queries.tsv"));
        ){
            String query;
            MaxScore scorer = new MaxScore(Constants.vocabulary, Constants.documentIndex, new TokenizerImpl(true, true));

            start = System.currentTimeMillis();
            while((query = readerQ.readLine()) != null){
                PriorityQueue<DocumentScore> scoring = scorer.score(query.split("\t")[1], 300, "disjunctive");
            }
            end = System.currentTimeMillis();

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("WITH CACHE : " + (end - start));
        Constants.onExit();



    }
}
