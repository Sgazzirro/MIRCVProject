package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.scoring.MaxScore;
import it.unipi.utils.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QueryThroughput {
    // class designed to evaluate the query throughput of a given combination
    // queries are the ones of the trec eval

    public static long time(Path indexPath, int numResults) throws IOException {
        // Constants settings
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(indexPath);
        Constants.startSession();

        // charge queries in memory
        String QUERY_FILE = "./data/evaluation/msmarco-test2019-queries.tsv";
        List<String> queries = new ArrayList<>();

        BufferedReader queryReader = new BufferedReader(new FileReader(QUERY_FILE));
        String queryLine;
        while ( (queryLine=queryReader.readLine()) != null ) {
            String[] params = queryLine.split("\t");
            queries.add(params[1]);
        }

        // scoring
        MaxScore max = new MaxScore(Constants.vocabulary, Constants.documentIndex, Tokenizer.getInstance());
        long time = System.currentTimeMillis();
        for (String query: queries)
            max.score(query, numResults, "conjunctive");

        return System.currentTimeMillis() - time;
    }

    public static void main(String[] args) throws IOException {
        double time = time(Constants.DATA_PATH, 10);
        System.out.println(time);
    }
}
