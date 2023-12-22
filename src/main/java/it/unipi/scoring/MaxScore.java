package it.unipi.scoring;

import it.unipi.encoding.Tokenizer;
import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MaxScore {

    private static final Logger logger = LoggerFactory.getLogger(MaxScore.class);

    private final Vocabulary vocabulary;
    private final DocumentIndex documentIndex;
    private final Tokenizer tokenizer;

    private final Scorer scorer;

    public MaxScore(Vocabulary vocabulary, DocumentIndex documentIndex, Tokenizer tokenizer) {
        this.vocabulary = vocabulary;
        this.documentIndex = documentIndex;
        this.tokenizer = tokenizer;

        scorer = Scorer.getScorer(documentIndex);
    }


    public PriorityQueue<DocumentScore> score(String query, int numResults, String mode){
        // Stupid check but better safe than sorry
        numResults = Math.min(numResults, Constants.N);

        List<String> queryTokens = tokenizer.tokenizeBySpace(query);
        TreeMap<Double, PostingList> treeMap = new TreeMap<>();

        ////////////////// DISJUNCTIVE MODE ///////////////////////
        int IOCalls = 0;
        if(mode.equals("disjunctive")) {
            for (String token : queryTokens) {
                if(!vocabulary.isPresent(token))
                    IOCalls++;
                VocabularyEntry entry = vocabulary.getEntry(token);
                if (entry != null)
                    treeMap.put(entry.getUpperBound(), entry.getPostingList());
            }
            if (treeMap.isEmpty())
                return null;
            
            logger.info("IO calls in maxscore: " + IOCalls);
            return maxScore(new ArrayList<>(treeMap.values()), new ArrayList<>(treeMap.keySet()), numResults);
        }
        ////////////////// CONJUNCTIVE MODE ///////////////////////
        else {
            List<PostingList> postingLists = new ArrayList<>();
            for (String token : queryTokens) {
                VocabularyEntry entry = vocabulary.getEntry(token);
                if (entry == null)
                    return new PriorityQueue<>();

                postingLists.add(entry.getPostingList());
            }

            return conjunctiveScore(postingLists, numResults);
        }
    }

    private PriorityQueue<DocumentScore> conjunctiveScore(List<PostingList> p, int K){
        if (K <= 0)
            return null;

        PriorityQueue<DocumentScore> scores = new PriorityQueue<>(K);
        while (nextConjunctive(p) == 0) {
            double score = 0;
            for (PostingList postingList : p)
                score += scorer.score(postingList);

            DocumentScore documentScore = new DocumentScore(p.get(0).docId(), score);
            if (scores.size() < K)
                scores.add(documentScore);
            else if (scores.peek().score < score) {
                scores.add(documentScore);
                scores.poll();
            }
        }

        for (PostingList postingList: p)
            postingList.reset();    // RESET of the posting lists

        return scores;
    }

    private PriorityQueue<DocumentScore> maxScore(List<PostingList> p, List<Double> sigma, int K) {
        if (K <= 0)
            return null;

        PriorityQueue<DocumentScore> scores = new PriorityQueue<>(K);
        List<Double> ub = computeUB(p, sigma);
        double theta = 0;       // Current maxScore
        int pivot = 0;

        int current = minimumDocId(p);
        if (current == -1)
            return null;

        while (pivot < p.size()) {
            double score = 0;
            int next = Integer.MAX_VALUE;
            for (int i = pivot; i < p.size(); i++) {
                PostingList currentList = p.get(i);
                if (currentList.docId() == current) {
                    score += scorer.score(currentList);

                    if (currentList.hasNext())
                        currentList.next();
                    else  {
                        currentList.reset();    // RESET of the posting list
                        p.remove(i);
                        sigma.remove(i);
                        if (p.size() > 1)
                            ub = computeUB(p, sigma);

                        i--;
                        continue;
                    }
                }

                next = Math.min(next, currentList.docId());
            }

            for (int i = pivot - 1; i >= 0; i--) {
                if (score + ub.get(i) <= theta)
                    break;

                PostingList currentList = p.get(i);
                try {
                    currentList.nextGEQ(current);
                } catch (NoSuchElementException e) {
                    currentList.reset();    // RESET of the posting list
                    p.remove(i);
                    sigma.remove(i);
                    if (p.size() > 1)
                        ub = computeUB(p, sigma);

                    i--;
                    continue;
                }

                if (currentList.docId() == current)
                    score += scorer.score(p.get(i));
            }

            DocumentScore ds = new DocumentScore(current, score);
            if (scores.isEmpty() || scores.size() < K || scores.peek().score < score) {
                scores.add(ds);
                if (scores.size() > K)  // Keep only top K results
                    scores.poll();

                assert scores.peek() != null;
                theta = scores.peek().score;
                while (pivot < p.size() && ub.get(pivot) < theta)
                    pivot++;
            }

            current = next;
        }

        for (PostingList postingList : p)
            postingList.reset(); // RESET of the last posting lists

        return scores;
    }

    private int minimumDocId(List<PostingList> postingLists){
        int min = Integer.MAX_VALUE;
        for (PostingList postingList : postingLists) {
            if (postingList.hasNext()){
                try {
                    postingList.next();
                } catch (NoSuchElementException e) {
                    return -1;
                }
                if (postingList.docId() < min)
                    min = postingList.docId();
            }
        }
        return min;
    }

    private int nextConjunctive(List<PostingList> postingLists){
        int current = minimumDocId(postingLists);
        if (current == -1)
            return -1;

        while (true) {
            boolean allToCurrent = true;
            for (PostingList p : postingLists) {
                if (p.docId() < current) {
                    try {
                        p.nextGEQ(current);
                    } catch (NoSuchElementException e){
                        return -1;
                    }
                }

                if (p.docId() > current) {
                    current = p.docId();
                    allToCurrent = false;
                    break;
                }
            }

            if (allToCurrent)
                return 0;
        }
    }

    private List<Double> computeUB (List<PostingList> p, List<Double> sigma){
        List<Double> ub = new ArrayList<>(p.size());
        ub.add(0, sigma.get(0));
        for (int i=1; i < p.size(); i++)
            ub.add(i, (ub.get(i-1) + sigma.get(i)));

        return ub;
    }
}
