package it.unipi;

import it.unipi.encoding.implementation.TokenizerImpl;
import it.unipi.io.DocumentStream;
import it.unipi.model.Document;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.scoring.MaxScore;
import it.unipi.encoding.CompressionType;
import it.unipi.utils.Constants;
import it.unipi.scoring.DocumentScore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class IndexCreated {
    public static void main(String[] args){
        // Return how many documents and how many words are indexed
        int numDocs;
        int numWords;
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Path.of("./data"));
        MaxScore max = new MaxScore(new VocabularyImpl(), new TokenizerImpl());

        DocumentStream stream = DocumentStream.getInstance();

        for(int i = 0; i < 1; i++) {
            // TODO - with numResults = 10 or 100 we get different results
            int numResults = 2;
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
                    reverseMode.stream().mapToInt(score -> score.docId).distinct().count());
            System.out.println();

            for (DocumentScore first : reverseMode) {
                System.out.println("ID : " + first.docId + " SCORE : " + first.score);

                if (printFirstText) {
                    System.out.println(stream.getDoc(first.docId).getText() + "\n");
                    printFirstText = false;
                }
            }

        }
    }
}
