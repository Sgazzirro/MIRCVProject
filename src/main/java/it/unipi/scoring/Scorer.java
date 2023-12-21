package it.unipi.scoring;

import it.unipi.model.Posting;
import it.unipi.model.PostingList;
import it.unipi.utils.Constants;

public class Scorer {

    private static double TFIDFPartialTF(Posting posting) {
        return 1 + Math.log10(posting.getTf());
    }

    private static double BM25PartialTF(Posting posting) {
        int docLength           = Constants.documentIndex.getLength(posting.getDocId());
        double averageDocLength = Constants.documentIndex.getAverageLength();

        return posting.getTf() / ( Constants.BM25_k *
                ( (1 - Constants.BM25_b) + Constants.BM25_b * docLength / averageDocLength) +
                posting.getTf() );
    }

    public static double partialTF(Posting posting) {
        return switch (Constants.getScoring()) {
            case TFIDF -> TFIDFPartialTF(posting);
            case BM25 -> BM25PartialTF(posting);
        };
    }

    public static double score(Posting posting, double idf) {
        return partialTF(posting) * idf;
    }
}
