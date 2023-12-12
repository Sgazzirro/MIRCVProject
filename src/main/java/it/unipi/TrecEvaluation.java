package it.unipi;

import it.unipi.model.implementation.TokenizerImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.scoring.MaxScore;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A class demanded to perform a trec_eval onto our index
 */
public class TrecEvaluation {
    /**
     * The name of the file in which qrels are encoded
     * STRUCTURE OF A QREL: queryID | 0 | documentID | Relevance
     */
    private static String QRELS = "./data/evaluation/qrels.train.tsv";
    /**
     * The name of the file in which trec queries are stored
     * QUERY STRUCTURE : queryID | text
     */
    private static String QUERIES = "./data/evaluation/queries.train.tsv";
    /**
     * The name of the file in which we store our IR's results
     * RESULT FORMAT : QueryID | Q0 | pid | rank | Score | IDofTheRUN
     */
    private static String RESULT = "./data/evaluation/results.tsv";

    private static class Query{
        String queryID;
        String text;

        public Query(String query){
            String[] params = query.split("");
            queryID = params[0];
            text = params[1];
        }

        public String getQueryID() {
            return queryID;
        }

        public String getText() {
            return text;
        }
    }

    private static class QRel{
        String queryID;
        String PID;
        Integer relevance;

        public QRel(String qrel){
            String[] params = qrel.split("");
            queryID = params[0];
            PID = params[2];
            relevance = Integer.parseInt(params[3]);
        }

        public String getQueryID() {
            return queryID;
        }

        public String getPID() {
            return PID;
        }

        public Integer getRelevance() {
            return relevance;
        }
    }

    private static class Result{
        String queryID;
        String PID;
        Integer rank;
        Float score;
        String scope = "STANDARD";

        public Result(String queryID, String PID, Integer rank, Float score) {
            this.queryID = queryID;
            this.PID = PID;
            this.rank = rank;
            this.score = score;
        }

        public String toString(){
            return queryID + " Q0 " + PID + " " + rank.toString() + " " + score.toString() +  "  " + scope;
        }
    }
    public static void generateEvaluation(){
        try(
                BufferedReader queries = new BufferedReader(new FileReader(QUERIES));
                BufferedWriter writer = new BufferedWriter(new FileWriter(RESULT));
                ) {
            String queryLine;

            MaxScore scorer = new MaxScore(new VocabularyImpl(), new TokenizerImpl());
            while((queryLine = queries.readLine()) != null){
                Query q = new Query(queryLine);
                List<Result> results = new ArrayList<>();
                scorer.score(q.getText(), 1000);

                // TODO: Riempire la lista di risultati

                for(Result r : results){
                    writer.write(r.toString());
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
