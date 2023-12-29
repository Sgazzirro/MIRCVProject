package it.unipi;

import it.unipi.encoding.*;
import it.unipi.scoring.MaxScore;
import it.unipi.scoring.*;
import it.unipi.utils.Constants;

import java.io.*;
import java.nio.file.Path;
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
    private static String QUERIES = "./data/evaluation/msmarco-test2019-queries.tsv";
    /**
     * The name of the file in which we store our IR's results
     * RESULT FORMAT : QueryID | Q0 | pid | rank | Score | IDofTheRUN
     */
    private static String RESULT = "./data/evaluation/resultsTFIDF.txt";

    private static class Query{
        String queryID;
        String text;

        public Query(String query){
            String[] params = query.split("\t");
            System.out.println(params[0]);
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
        Double score;
        String scope = "STANDARD";

        public Result(String queryID, String PID, Integer rank, Double score) {
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

            MaxScore scorer = new MaxScore(Constants.vocabulary, Constants.documentIndex, Tokenizer.getInstance());
            while((queryLine = queries.readLine()) != null){
                Query q = new Query(queryLine);
                System.out.println(q.getText());
                List<Result> results = new ArrayList<>();
                PriorityQueue<DocumentScore> scoring = scorer.score(q.getText(), 10, "disjunctive");
                PriorityQueue<DocumentScore> reverseMode = new PriorityQueue<>(java.util.Collections.reverseOrder());
                while(scoring.size() > 0) {
                    DocumentScore first = scoring.poll();
                    reverseMode.add(first);
                }

                int rank = 1;
                while(reverseMode.size() > 0){
                    DocumentScore first = reverseMode.poll();
                    results.add(new Result(q.getQueryID(), String.valueOf(Constants.documentIndex.getDocNo(first.docId())), rank, first.score()));
                    rank++;
                }
                // TODO: Riempire la lista di risultati

                for(Result r : results){
                    writer.write(r.toString() + "\n");
                }
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){
        Constants.CACHING = false;
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Constants.DATA_PATH);
        Constants.setScoring(ScoringType.TFIDF);
        Constants.startSession();
        generateEvaluation();
        Constants.onExit();
    }
}
