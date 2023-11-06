package it.unipi.scoring;

import it.unipi.model.implementation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

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
            return "{" +
                    "docId=" + docId +
                    ", score=" + String.format("%.3f", score) +
                    '}';
        }
    }

    private final Vocabulary vocabulary;
    private final DocumentIndex documentIndex;

    private final Tokenizer tokenizer;

    public Scorer(Vocabulary vocabulary, DocumentIndex documentIndex, Tokenizer tokenizer) {
        this.vocabulary = vocabulary;
        this.documentIndex = documentIndex;
        this.tokenizer = tokenizer;
    }

    public DocumentScore[] score(String query, int numResults) {
        // TODO: implement dynamic pruning condition (MaxScore or WAND)

        PriorityQueue<DocumentScore> scores = new PriorityQueue<>();

        List<String> tokens = tokenizer.tokenizeBySpace(query);
        int numTokens = tokens.size();

        // Get the posting lists of all the terms in the query
        List<PostingList> postingLists = new ArrayList<>(numTokens);
        for (int i = 0; i < numTokens; i++) {
            VocabularyEntry entry = vocabulary.getEntry(tokens.get(i));

            // If the entry is null (this means that the term of the query was not present in any document)
            // discard the term and decrease the num of Tokens
            if (entry == null) {
                tokens.remove(i--);
                numTokens--;
            } else
                postingLists.add(entry.getPostingList());
        }

        int docId, currentDocId;      // id of the current document
        double score;   // score of the current document

        while (!postingLists.isEmpty()) {
            docId = postingLists.get(0).docId();
            score = postingLists.get(0).score();

            for (int i = 1; i < numTokens; i++) {
                currentDocId = postingLists.get(i).docId();
                if (currentDocId < docId) {
                    docId = currentDocId;
                    score = postingLists.get(i).score();
                } else if (currentDocId == docId)
                    score += postingLists.get(i).score();
            }

            // Advance all posting lists related to the current docId
            for (int i = 0; i < numTokens; i++) {
                if (postingLists.get(i).docId() == docId) {
                    // Advance the posting list
                    if (postingLists.get(i).hasNext())
                        postingLists.get(i).next();
                    // If we are at the end of the posting list, remove it
                    else {
                        postingLists.remove(i--);
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
