package it.unipi.scoring;

import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.scoring.implementation.BM25Scorer;
import it.unipi.scoring.implementation.TFIDFScorer;
import it.unipi.utils.Constants;

public abstract class Scorer {

    protected abstract double partialScore(PostingList postingList);

    public double score(PostingList postingList) {
        return partialScore(postingList) * postingList.vocabularyEntry().idf();
    }

    public double computeUpperBound(PostingList postingList) {
        postingList.reset();

        double partialUpperBound = 0;
        while (postingList.hasNext()) {
            postingList.next();
            double partialScore = partialScore(postingList);
            if (partialScore > partialUpperBound)
                partialUpperBound = partialScore;
        }

        return partialUpperBound * postingList.vocabularyEntry().idf();
    }

    public static Scorer getScorer(DocumentIndex documentIndex) {
        return switch (Constants.getScoring()) {
            case TFIDF -> new TFIDFScorer();
            case BM25 -> new BM25Scorer(documentIndex);
        };
    }
}
