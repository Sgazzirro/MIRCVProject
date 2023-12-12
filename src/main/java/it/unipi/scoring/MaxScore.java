package it.unipi.scoring;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.*;
import it.unipi.utils.DocumentScore;

import java.io.EOFException;
import java.util.*;

public class MaxScore{
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

        for(String token: queryTokens){
            VocabularyEntry entry = vocabularyImpl.getEntry(token);
            if(entry==null){
                queryTokens.remove(token);
            } else {
                treeMap.put(entry.getUpperBound(), entry.getPostingList());
            }
        }
        return maxScore(new ArrayList<>(treeMap.values()), new ArrayList<>(treeMap.keySet()), numResults);
    }

    private List<Double> computeUB (List<PostingList> p, List<Double> sigma){
        List<Double> ub = new ArrayList<>(p.size());
        ub.add(0, sigma.get(0));
        for(int i=1; i< p.size(); i++){
            ub.add(i, (ub.get(i-1)+sigma.get(i)));
        }
        return ub;
    }

    private PriorityQueue<DocumentScore> maxScore(List<PostingList> p, List<Double> sigma, int K){
        PriorityQueue<DocumentScore> scores = new PriorityQueue<>(K);
        List<Double> ub = computeUB(p, sigma);
        double theta = 0;
        int pivot = 0;
        int current = minimumDocId(p);
        if(current==-1) return null;
        int n = p.size();
        while(pivot < n){
            double score =0;
            int next = Integer.MAX_VALUE;
            for(int i=pivot; i<n; i++){
                if (p.get(i).docId() == current){
                    score += p.get(i).score();
                    try {
                        p.get(i).next();
                    } catch (EOFException e){
                        p.remove(i);
                        sigma.remove(i);
                        if(p.size()>1) ub = computeUB(p, sigma);
                        continue;
                    }
                }
                if(p.get(i).docId()<next){
                    next = p.get(i).docId();
                }
            }
            for(int i=pivot-1; i>0; i--){
                if(score + ub.get(i)<=theta) break;
                try {
                    p.get(i).nextGEQ(current);
                } catch(EOFException e){
                    p.remove(i);
                    sigma.remove(i);
                    if(p.size()>1) ub = computeUB(p, sigma);
                    continue;
                }
                if(p.get(i).docId()==current){
                    score+=p.get(i).score();
                }
            }
            DocumentScore ds = new DocumentScore(current, score);
            if (scores.isEmpty() || scores.size()<K || scores.peek().score<score){
                scores.add(ds);
                if(!(scores.size()<K)) scores.poll();
                theta = scores.peek().score;
                while (pivot<n && ub.get(pivot)<=theta){
                    pivot++;
                }
            }
            current=next;
        }
        return scores;
    }

    private boolean areEmpty(List<PostingList> postingLists){
        for(PostingList postingList:postingLists){
            if(!postingList.hasNext()) return true;
        }
        return false;
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

    private int minimumDocIdConjunctive(List<PostingList> postingLists){
        int minDocId = minimumDocId(postingLists);
        if (minDocId == -1) return -1;
        int actualMaxDocId = -1;
        while(true) {
            for (PostingList postingList : postingLists) {
                if(postingList.docId()>actualMaxDocId) actualMaxDocId = postingList.docId();
            }
            if (actualMaxDocId == minDocId) return minDocId;        // significa che sono tutte allo stesso docId
            else {
                try{
                    for(PostingList postingList: postingLists){
                        postingList.nextGEQ(actualMaxDocId);
                    }
                    minDocId = actualMaxDocId;
                } catch (EOFException e){
                    return -1;
                }
            }
        }
    }
}
