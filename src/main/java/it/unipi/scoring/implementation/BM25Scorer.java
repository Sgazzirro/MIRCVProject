package it.unipi.scoring.implementation;

import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.scoring.Scorer;
import it.unipi.utils.Constants;

public class BM25Scorer extends Scorer {

    private DocumentIndex documentIndex;

    public BM25Scorer(DocumentIndex documentIndex) {
        this.documentIndex = documentIndex;
    }

    protected double partialScore(PostingList postingList) {
        int docLength           = documentIndex.getLength(postingList.docId());
        double averageDocLength = documentIndex.getAverageLength();

        return postingList.termFrequency() / ( Constants.BM25_k *
                ( (1 - Constants.BM25_b) + Constants.BM25_b * docLength / averageDocLength) +
                postingList.termFrequency() );
    }
}
