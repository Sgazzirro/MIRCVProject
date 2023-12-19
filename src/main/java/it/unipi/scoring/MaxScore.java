package it.unipi.scoring;

import it.unipi.encoding.Tokenizer;
import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;

import java.io.EOFException;
import java.util.*;

public class MaxScore{
    ////////////////////////////////////////////////////
    // TODO: GROSSO COME UNA CASA: LE POSTING LIST DOPO ESSER STATE VISTE DEVONO ESSERE RIPORTATE A 0, SENNò RIMANGONO PIANTATE INFONDO
    private final Vocabulary vocabulary;
    private final DocumentIndex documentIndex;
    private final Tokenizer tokenizer;

    public MaxScore(Vocabulary vocabulary, DocumentIndex documentIndex, Tokenizer tokenizer) {
        this.vocabulary = vocabulary;
        this.documentIndex = documentIndex;
        this.tokenizer = tokenizer;
    }


    public PriorityQueue<DocumentScore> score(String query, int numResults, String mode){
        // Stupid check but better safe than sorry
        numResults = Math.min(numResults, Constants.N);

        List<String> queryTokens = tokenizer.tokenizeBySpace(query);
        TreeMap<Double, PostingList> treeMap = new TreeMap<>();

        ////////////////// DISJUNCTIVE MODE ///////////////////////
        if(mode.equals("disjunctive")) {
            for (String token : queryTokens) {
                VocabularyEntry entry = vocabulary.getEntry(token);
                if (entry != null)
                    treeMap.put(entry.getUpperBound(), entry.getPostingList());
            }
            if (treeMap.isEmpty())
                return null;

            PriorityQueue<DocumentScore> result = maxScore(new ArrayList<>(treeMap.values()), new ArrayList<>(treeMap.keySet()), numResults);
            Constants.cachingStrategy();
            return result;
        }
        ////////////////// CONJUNCTIVE MODE ///////////////////////
        else {
            List<PostingList> postingLists = new ArrayList<>();
            for (String token : queryTokens) {
                VocabularyEntry entry = vocabulary.getEntry(token);
                if(entry==null) return new PriorityQueue<>();
                postingLists.add(entry.getPostingList());
            }
            PriorityQueue<DocumentScore> result =  conjunctiveScore(postingLists, numResults);
            Constants.cachingStrategy();
            return result;
        }
    }

    private PriorityQueue<DocumentScore> conjunctiveScore(List<PostingList> p, int K){
        PriorityQueue<DocumentScore> scores = new PriorityQueue<>(K);
        while(nextConjunctive(p)==0){
            double score = 0;
            for(PostingList postingList: p){
                score += postingList.score();
            }
            DocumentScore documentScore = new DocumentScore(p.get(0).docId(), score);
            if(scores.size()<K) scores.add(documentScore);
            else if (scores.peek().score<score){
                scores.add(documentScore);
                scores.poll();
            }
        }
        for (PostingList postingList: p) postingList.reset();    // RESET of the posting lists
        return scores;
    }

    private PriorityQueue<DocumentScore> maxScore(List<PostingList> p, List<Double> sigma, int K) {

        PriorityQueue<DocumentScore> scores = new PriorityQueue<>(K);
        List<Double> ub = computeUB(p, sigma);
        double theta = 0;
        int pivot = 0;
        int current = minimumDocId(p);
        if (current == -1) return null;
        int n = p.size();
        while (pivot < p.size()) {
            double score = 0;
            int next = Integer.MAX_VALUE;
            for (int i = pivot; i < p.size(); i++) {
                if (p.get(i).docId() == current) {
                    score += p.get(i).score();
                    try {
                        p.get(i).next();
                    } catch (EOFException e) {
                        p.get(i).reset();    // RESET of the posting list
                        p.remove(i);
                        sigma.remove(i);
                        if (p.size() > 1)
                            ub = computeUB(p, sigma);

                        i--;
                        continue;
                    }
                }
                if (p.get(i).docId() < next) {
                    next = p.get(i).docId();
                }
            }
            for (int i = pivot - 1; i >= 0; i--) {
                if (score + ub.get(i) <= theta) break;
                try {
                    p.get(i).nextGEQ(current);
                } catch (EOFException e) {
                    p.get(i).reset();    // RESET of the posting list
                    p.remove(i);
                    sigma.remove(i);
                    if (p.size() > 1)
                        ub = computeUB(p, sigma);

                    i--;
                    continue;
                }
                if (p.get(i).docId() == current) {
                    score += p.get(i).score();

                }
            }
            DocumentScore ds = new DocumentScore(current, score);
            if (scores.isEmpty() || scores.size() < K || scores.peek().score < score) {
                scores.add(ds);
                if (!(scores.size() <= K)) scores.poll();
                theta = scores.peek().score;
                while (pivot < p.size() && ub.get(pivot) < theta) {
                    pivot++;

                }
            }
            // NESSUNO HA MODIFICATO NEXT
            current = next;
        }
        for (PostingList postingList : p)
            postingList.reset(); // RESET of the last posting lists

        return scores;
    }

    private int minimumDocId(List<PostingList> postingLists){
        int min = Integer.MAX_VALUE;
        for(PostingList postingList: postingLists){
            if(postingList.hasNext()){
                try {
                    postingList.next();
                } catch(EOFException e){
                    return -1;
                }
                if(postingList.docId()<min){
                    min=postingList.docId();
                }
            }
        }
        return min;
    }

    private int nextConjunctive(List<PostingList> postingLists){
        int current = minimumDocId(postingLists);
        if (current == -1) return -1;
        while(true){
            boolean allToCurrent=true;
            for(PostingList p: postingLists){
                if(p.docId()<current){
                    try{
                        p.nextGEQ(current);
                    } catch (EOFException e){
                        return -1;
                    }
                }
                if(p.docId()>current){
                    current = p.docId();
                    allToCurrent=false;
                    break;
                }
            }
            if(allToCurrent){
                return 0;
            }
        }
    }

    private List<Double> computeUB (List<PostingList> p, List<Double> sigma){
        List<Double> ub = new ArrayList<>(p.size());
        ub.add(0, sigma.get(0));
        for(int i=1; i< p.size(); i++){
            ub.add(i, (ub.get(i-1)+sigma.get(i)));
        }
        return ub;
    }

    private boolean areEmpty(List<PostingList> postingLists){
        for(PostingList postingList:postingLists){
            if(!postingList.hasNext()) return true;
        }
        return false;
    }
}
