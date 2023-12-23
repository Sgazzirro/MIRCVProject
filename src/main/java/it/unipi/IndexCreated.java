package it.unipi;

import it.unipi.encoding.Tokenizer;
import it.unipi.io.DocumentStream;
import it.unipi.scoring.MaxScore;
import it.unipi.encoding.CompressionType;
import it.unipi.scoring.ScoringType;
import it.unipi.utils.Constants;
import it.unipi.scoring.DocumentScore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class IndexCreated {
    public static void main(String[] args) throws IOException {
        // Return how many documents and how many words are indexed
        int numDocs;
        int numWords;
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setScoring(ScoringType.BM25);
        Constants.setPath(Path.of("./data"));
        Constants.startSession();

        MaxScore max = new MaxScore(Constants.vocabulary, Constants.documentIndex, Tokenizer.getInstance());

        DocumentStream stream = new DocumentStream();

        for(int i = 0; i < 2; i++) {
            long start = System.currentTimeMillis();
            // TODO - with numResults = 10 or 100 we get different results
            int numResults = 10;
            boolean printFirstText = true;

            PriorityQueue<DocumentScore> scoring = max.score("average rainfall in Costa Rica", numResults, "disjunctive");
            List<DocumentScore> reverseMode = new ArrayList<>();

            while (!scoring.isEmpty()) {
                DocumentScore first = scoring.poll();
                reverseMode.add(0, first);
            }

            // Print how many different documents we got (for testing purposes)
            System.out.println("Results asked: " + numResults);
            System.out.println("Results obtained: " +
                    reverseMode.stream().mapToInt(DocumentScore::docId).distinct().count());
            System.out.println();

            for (DocumentScore first : reverseMode) {
                System.out.println("ID : " + first.docId() + " SCORE : " + first.score());

                if (printFirstText) {
                    System.out.println(stream.getDoc(first.docId()).getText() + "\n");
                    printFirstText = false;
                }
            }

            long end = System.currentTimeMillis();
            if(i == 0)
                System.out.println("SENZA IL CACHING GENTILMENTE OFFERTO DA JAVA : " + (end - start));
            else
                System.out.println("CON IL CACHING GENTILMENTE OFFERTO DA JAVA : " + (end - start));
        }
    }
}
