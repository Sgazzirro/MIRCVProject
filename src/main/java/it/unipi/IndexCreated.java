package it.unipi;

import it.unipi.model.implementation.Document;
import it.unipi.model.implementation.TokenizerImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.scoring.MaxScore;
import it.unipi.utils.CompressionType;
import it.unipi.utils.Constants;
import it.unipi.utils.DocumentScore;
import it.unipi.utils.FetcherCompressed;

import java.nio.file.Path;
import java.util.PriorityQueue;

public class IndexCreated {
    public static void main(String[] args){
        // Return how many documents and how many words are indexed
        int numDocs;
        int numWords;
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Path.of("./data"));
        MaxScore max = new MaxScore(new VocabularyImpl(), new TokenizerImpl());

        for(int i = 0; i < 1; i++) {
            PriorityQueue<DocumentScore> scoring = max.score("Average Rainfall In Costa Rica", 10, "disjunctive");
            for (DocumentScore score : scoring) {
                System.out.println(score.docId);
            }

        }
    }
}
