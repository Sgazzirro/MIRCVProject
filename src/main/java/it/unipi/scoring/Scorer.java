package it.unipi.scoring;

import it.unipi.encoding.Tokenizer;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Scorer {

    static class DocumentScore implements Comparable<DocumentScore> {
        int docId;
        double score;

        public DocumentScore(int docId, double score) {
            this.docId = docId;
            this.score = score;
        }

        @Override
        public int compareTo(DocumentScore o) {
            // Reverse order so documents with higher score are first
            return Double.compare(o.score, score);
        }

        @Override
        public String toString() {
            return "{docId=" + docId +
                    ", score=" + String.format("%.4f", score) +
                    '}';
        }
    }

    private final Vocabulary vocabulary;
    private final Tokenizer tokenizer;

    public Scorer(Vocabulary vocabulary, Tokenizer tokenizer) {
        this.vocabulary = vocabulary;
        this.tokenizer = tokenizer;
    }

    public DocumentScore[] score(String query, int numResults) {
        // TODO: implement dynamic pruning condition (MaxScore or WAND)

        PriorityQueue<DocumentScore> scores = new PriorityQueue<>();

        List<String> tokens = tokenizer.tokenizeBySpace(query);
        int numTokens = tokens.size();

        // Get the posting lists of all the terms in the query
        List<PostingList> postingList = new ArrayList<>(numTokens);
        for (int i = 0; i < numTokens; i++) {
            VocabularyEntry entry = vocabulary.getEntry(tokens.get(i));

            // If the entry is null (this means that the term of the query was not present in any document)
            // discard the term and decrease the num of Tokens
            if (entry == null) {
                tokens.remove(i--);
                numTokens--;
            } else
                postingList.add(entry.getPostingList());
        }

        int docId, currentDocId;      // id of the current document
        double score;   // score of the current document

        while (!postingList.isEmpty()) {
            docId = postingList.get(0).docId();
            score = postingList.get(0).score();

            for (int i = 1; i < numTokens; i++) {
                currentDocId = postingList.get(i).docId();
                if (currentDocId < docId) {
                    docId = currentDocId;
                    score = postingList.get(i).score();
                } else if (currentDocId == docId)
                    score += postingList.get(i).score();
            }

            // Advance all posting lists related to the current docId
            for (int i = 0; i < numTokens; i++) {
                if (postingList.get(i).docId() == docId) {
                    // Advance the posting list
                    if (postingList.get(i).hasNext())
                        try {
                            postingList.get(i).next();
                        } catch (EOFException eofException) {
                            eofException.printStackTrace();
                        }
                    // If we are at the end of the posting list, remove it
                    else {
                        postingList.remove(i--);
                        numTokens--;
                    }
                }
            }

            scores.add(new DocumentScore(docId, score));
        }

        numResults = Math.min(numResults, scores.size());
        return scores.stream()
                .limit(numResults).sorted()
                .toArray(DocumentScore[]::new);
    }
}
