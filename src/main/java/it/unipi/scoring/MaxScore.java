package it.unipi.scoring;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.*;
import it.unipi.utils.DocumentScore;

import java.io.EOFException;
import java.util.*;

public class MaxScore{
    ////////////////////////////////////////////////////
    // TODO: GROSSO COME UNA CASA: LE POSTING LIST DOPO ESSER STATE VISTE DEVONO ESSERE RIPORTATE A 0, SENNò RIMANGONO PIANTATE INFONDO
    private final VocabularyImpl vocabularyImpl;
    //private final DocumentIndexImpl documentIndexImpl;
    private final TokenizerImpl tokenizerImpl;

    public MaxScore(VocabularyImpl vocabularyImpl, TokenizerImpl tokenizerImpl) {
        this.vocabularyImpl = vocabularyImpl;
        //this.documentIndexImpl = documentIndexImpl;
        this.tokenizerImpl = tokenizerImpl;
    }

    public PriorityQueue<DocumentScore> score(String query, int numResults){
        List<String> queryTokens = tokenizerImpl.tokenizeBySpace(query);
        TreeMap<Double, PostingList> treeMap = new TreeMap<>();

        // TO DO: PUT IT IN METHOD CALL
        String mode = "disjunctive";

        ////////////////// DISJUNCTIVE MODE ///////////////////////
        if(mode.equals("disjunctive")) {
            System.out.println(query);
            for (String token : queryTokens) {
                VocabularyEntry entry = vocabularyImpl.getEntry(token);
                if(entry!=null) {
                    treeMap.put(entry.getUpperBound(), entry.getPostingList());
                }
            }
            if(treeMap.isEmpty()) return null;
            return maxScore(new ArrayList<>(treeMap.values()), new ArrayList<>(treeMap.keySet()), numResults);
        }
        ////////////////// CONJUNCTIVE MODE ///////////////////////
        else {
            List<PostingList> postingLists = new ArrayList<>();
            for (String token : queryTokens) postingLists.add(vocabularyImpl.getEntry(token).getPostingList());
            return conjunctiveScore(postingLists, numResults);
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
            for (int i = pivot; i < n; i++) {
                if (p.get(i).docId() == current) {
                    score += p.get(i).score();
                    try {
                        p.get(i).next();
                    } catch (EOFException e) {
                        p.remove(i);
                        sigma.remove(i);
                        if (p.size() > 1) ub = computeUB(p, sigma);
                        continue;
                    }
                }
                if (p.get(i).docId() < next) {
                    next = p.get(i).docId();
                }
            }
            for (int i = pivot - 1; i > 0; i--) {
                if (score + ub.get(i) <= theta) break;
                try {
                    p.get(i).nextGEQ(current);
                } catch (EOFException e) {
                    p.remove(i);
                    sigma.remove(i);
                    if (p.size() > 1) ub = computeUB(p, sigma);
                    continue;
                }
                if (p.get(i).docId() == current) {
                    score += p.get(i).score();

                }
            }
            DocumentScore ds = new DocumentScore(current, score);
            if (scores.isEmpty() || scores.size() < K || scores.peek().score < score) {
                scores.add(ds);
                if (!(scores.size() < K)) scores.poll();
                theta = scores.peek().score;
                while (pivot < p.size() && ub.get(pivot) < theta) {
                    pivot++;
                }
            }
            // NESSUNO HA MODIFICATO NEXT
            current = next;
        }
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
