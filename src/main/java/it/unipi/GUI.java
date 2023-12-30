package it.unipi;

import it.unipi.encoding.Tokenizer;
import it.unipi.io.DocumentStream;
import it.unipi.scoring.MaxScore;
import it.unipi.encoding.CompressionType;
import it.unipi.scoring.ScoringType;
import it.unipi.utils.Constants;
import it.unipi.scoring.DocumentScore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

public class GUI {
    public static void main(String[] args) throws IOException {
        Constants.CACHING = true;
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setScoring(ScoringType.TFIDF);
        Constants.setPath(Constants.DATA_PATH);
        Constants.startSession();

        MaxScore max = new MaxScore(Constants.vocabulary, Constants.documentIndex, Tokenizer.getInstance());
        DocumentStream stream = new DocumentStream(Constants.COLLECTION_FILE);

        int NUM_RESULTS = 10;
        String option = "disjunctive";

        do {
            System.out.println("MSMARCO PASSAGES: Input Your query (Closed between \"\" for conjunctive mode, disjunctive otherwise)");
            String query = new Scanner(System.in).nextLine();
            if (query.startsWith("\"") && query.endsWith("\""))
                option = "conjunctive";
            else
                option = "disjunctive";

            long start = System.currentTimeMillis();
            PriorityQueue<DocumentScore> scoring = max.score(query, NUM_RESULTS, option);
            long time = System.currentTimeMillis() - start;

            System.out.println("+--------------------------------------+");
            System.out.println(scoring.size() + " results loaded in " + time + " (ms)");
            System.out.println("+--------------------------------------+");

            List<DocumentScore> reverseMode = new ArrayList<>();
            if(scoring.isEmpty()){
                System.out.println("+--------------------------------------+");
                System.out.println("No results found... :(");
                System.out.println("+--------------------------------------+");
                continue;
            }

            while (!scoring.isEmpty()) {
                DocumentScore first = scoring.poll();
                DocumentScore second = new DocumentScore(Constants.documentIndex.getDocNo(first.docId()), first.score());
                reverseMode.add(0, second);
            }

            System.out.println("DOCNO : " + reverseMode.get(0));
            System.out.println("What do you want to do next?");
            System.out.println("+--------------------------------------+");
            System.out.println("1 : Check the passage at DOCNO : " + reverseMode.get(0).docId());
            System.out.println("2 : Make another query");
            System.out.println("3 : Change the number of results to fetch at the next query");
            System.out.println("4 : Check all results (only DOCNO)");
            System.out.println("5 : Quit");
            System.out.println("+--------------------------------------+");
            int choice = new Scanner(System.in).nextInt();
            if(choice == 5)
                break;
            switch (choice) {
                case 1:
                    do {
                        System.out.println("+--------------------------------------+");
                        System.out.println("Fetching passage...");
                        System.out.println("+--------------------------------------+");
                        System.out.println(stream.getDoc(reverseMode.get(0).docId()).getText());
                        reverseMode.remove(0);
                        System.out.println("Wanna see the next passage? [Y/N]");
                    } while (new Scanner(System.in).nextLine().equals("Y"));
                    break;
                case 2:
                    break;
                case 3:
                    System.out.println("New K : ");
                    int K = new Scanner(System.in).nextInt();
                    if (K < 1000)
                        NUM_RESULTS = K;
                    break;
                case 4:
                    while(reverseMode.size() > 0) {
                        System.out.println(Constants.documentIndex.getDocNo(reverseMode.get(0).docId()));
                        System.out.println(reverseMode.get(0));
                        reverseMode.remove(0);
                    }
                    break;
            }
        }while(true);

        Constants.onExit();
    }
}
