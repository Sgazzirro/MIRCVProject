package it.unipi.scoring;

import it.unipi.encoding.Tokenizer;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.*;
import it.unipi.utils.Constants;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Scorer {

    private static double TFIDFScore(PostingList postingList) {
        double tf = 1 + Math.log10(postingList.termFrequency());
        return tf * postingList.vocabularyEntry().idf();
    }

    private static double BM25Score(PostingList postingList) {
        double tf = postingList.termFrequency();

        int docLength           = Constants.documentIndex.getLength(postingList.docId());
        double averageDocLength = Constants.documentIndex.getAverageLength();

        return tf / ( Constants.BM25_k *
                ( (1 - Constants.BM25_b) + Constants.BM25_b * docLength / averageDocLength) +
                tf ) * postingList.vocabularyEntry().idf();
    }

    public static double score(PostingList postingList) {
        return switch (Constants.getScoring()) {
            case TFIDF -> TFIDFScore(postingList);
            case BM25 -> BM25Score(postingList);
        };
    }
}
