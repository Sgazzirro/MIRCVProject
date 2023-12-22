package it.unipi.scoring.implementation;

import it.unipi.model.PostingList;
import it.unipi.scoring.Scorer;

public class TFIDFScorer extends Scorer {

    @Override
    protected double partialScore(PostingList postingList) {
        return 1 + Math.log10(postingList.termFrequency());
    }
}
