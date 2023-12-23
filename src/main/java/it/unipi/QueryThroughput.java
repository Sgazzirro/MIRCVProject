package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.io.DocumentStream;
import it.unipi.scoring.DocumentScore;
import it.unipi.scoring.MaxScore;
import it.unipi.scoring.ScoringType;
import it.unipi.utils.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class QueryThroughput {
    public static void main(String[] args){
        // Constants settings
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.BLOCK_SIZE=10000;
        Constants.setScoring(ScoringType.TFIDF);
        Constants.setPath(Path.of("./data"));
        int numResults = 10;

        // charge queries in memory
        String QUERY_FILE = "./data/evaluation/msmarco-test2019-queries.tsv";
        List<String> queries = new ArrayList<>();
        try {
            BufferedReader queryReader = new BufferedReader(new FileReader(QUERY_FILE));
            String queryLine;
            while((queryLine=queryReader.readLine())!=null){
                String[] params = queryLine.split("\t");
                queries.add(params[1]);
            }
        } catch (IOException e){
            e.printStackTrace();
        }

        // scoring
        MaxScore max = new MaxScore(Constants.vocabulary, Constants.documentIndex, Tokenizer.getInstance());
        long time = System.currentTimeMillis();
        for(String query: queries) {
            max.score(query, numResults, "conjunctive");
        }
        System.out.println(System.currentTimeMillis()-time);
    }
}
