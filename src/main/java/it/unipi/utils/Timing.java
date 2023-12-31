package it.unipi.utils;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Encoder;
import it.unipi.encoding.EncodingType;
import it.unipi.encoding.Tokenizer;
import it.unipi.encoding.implementation.EliasFano;
import it.unipi.encoding.implementation.UnaryEncoder;
import it.unipi.scoring.MaxScore;
import it.unipi.scoring.ScoringType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class Timing {

    public static double TimeIT(boolean cache, boolean refresh, int maxRes) {
        Constants.CACHING = cache;
        Constants.startSession();
        long start, end;
        long accumulated = 0;
        int queries = 0;
        try(
                BufferedReader readerQ = new BufferedReader(new FileReader("./data/evaluation/msmarco-test2019-queries.tsv"))
        ){
            String query;

            while ( (query = readerQ.readLine()) != null ) {
                if (refresh)
                    Constants.startSession();
                if (++queries == maxRes)
                    break;
                start = System.currentTimeMillis();
                MaxScore scorer = new MaxScore(Constants.vocabulary, Constants.documentIndex, Tokenizer.getInstance());
                scorer.score(query.split("\t")[1], 1, "disjunctive");
                end = System.currentTimeMillis();
                accumulated += (end - start);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        double result = (double) accumulated;
        result = result / queries;              // Average time for 1 query
        double throughput = 1000 / result;      // How many query will be answered in 1 second

        System.out.println("THROUGHPUT WITH CACHE = " + cache +  ", REFRESH = " +  refresh + " SEARCHING FOR " + queries + " QUERIES : " + throughput );
        System.out.println("QUERY LATENCY WITH CACHE = " + cache +  ", REFRESH = " +  refresh + " SEARCHING FOR " + queries + " QUERIES : " + accumulated );
        Constants.onExit();

        return result;
    }

    public static void main(String[] args){
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Constants.DATA_PATH.resolve("EliasFano_Unary_10000_TFIDF"));
        Constants.setScoring(ScoringType.TFIDF);
        Encoder.setDocIdsEncoder(new EliasFano(EncodingType.DOC_IDS));
        Encoder.setTermFrequenciesEncoder(new UnaryEncoder(EncodingType.TERM_FREQUENCIES));
        Constants.BLOCK_SIZE = 10000;

        System.gc();

        TimeIT(false, true, 1);
        TimeIT(false, true,10);
        //TimeIT(false, true, 100);

        TimeIT(true, false,1);
        TimeIT(true, false,10);
        //TimeIT(true, false, 100);
    }
}
