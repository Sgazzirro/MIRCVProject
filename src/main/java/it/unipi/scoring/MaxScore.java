package it.unipi.scoring;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.model.implementation.DocumentIndexImpl;
import it.unipi.model.implementation.TokenizerImpl;
import it.unipi.model.implementation.VocabularyImpl;

import java.io.EOFException;
import java.util.*;

public class MaxScore{
    private final VocabularyImpl vocabularyImpl;
    private final DocumentIndexImpl documentIndexImpl;
    private final TokenizerImpl tokenizerImpl;

    static class DocumentScore implements Comparable<Scorer.DocumentScore> {
        int docId;
        double score;

        public DocumentScore(int docId, double score) {
            this.docId = docId;
            this.score = score;
        }

        @Override
        public int compareTo(Scorer.DocumentScore o) {
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

    public MaxScore(VocabularyImpl vocabularyImpl, DocumentIndexImpl documentIndexImpl, TokenizerImpl tokenizerImpl) {
        this.vocabularyImpl = vocabularyImpl;
        this.documentIndexImpl = documentIndexImpl;
        this.tokenizerImpl = tokenizerImpl;
    }

    public PriorityQueue<DocumentScore> score(String query, int numResults){
        PriorityQueue<DocumentScore> scores = new PriorityQueue<>();
        // term upper bounds
        List<Double> sigma = new ArrayList<>();
        List<String> queryTokens = tokenizerImpl.tokenizeBySpace(query);

        // get the posting lists of all the terms in the query
        List<PostingList> postingLists = new ArrayList<>();
        for(String token: queryTokens){
            VocabularyEntry entry = vocabularyImpl.getEntry(token);
            if(entry==null){
                queryTokens.remove(token);
            } else {
                postingLists.add(entry.getPostingList());
                sigma.add(entry.getUpperBound());
            }
        }
        // order posting lists by increasing term upper bound
        // DA VEDERE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //postingLists.sort((l1,l2)-> l1.getTermUpperBound().compareTo(l2.getTermUpperBound));

        // la threshold varrà 0 fino a che non ci sono almeno
        // numResults documenti analizzati dentro scores, altrimenti varrà
        // quanto lo score dell'ultimo elemento della queue
        Collections.sort(sigma);
        Collections.reverse(sigma);

        // DA QUA INIZIA LO PSEUDOCODICE DELL'ALGORITMO
        List<Double> ub = new ArrayList<>(queryTokens.size());
        ub.set(0, sigma.get(0));
        for(int i=1; i< queryTokens.size(); i++){
            ub.set(i, (ub.get(i-1)+sigma.get(i))); // non capisco perchè qua da sto warning
        }
        double theta = 0;
        int pivot = 0;
        int current = minimumDocId(postingLists);
        int n = queryTokens.size();
        while(!areEmpty(postingLists)){
            double score =0;
            int next = Integer.MAX_VALUE;
            for(int i=pivot; i<n; i++){
                if (postingLists.get(i).docId() == current){
                    score += postingLists.get(i).score();
                    try {
                        postingLists.get(i).next();
                    } catch (EOFException e){
                        continue;
                    }
                }
                if(postingLists.get(i).docId()<next){
                    next = postingLists.get(i).docId();
                }
            }
            for(int i=pivot-1; i>0; i--){
                if(score + ub.get(i)<=theta) break;
                try {
                    postingLists.get(i).nextGEQ(current);
                } catch(EOFException e){
                    continue;
                }
                if(postingLists.get(i).docId()==current){
                    score+=postingLists.get(i).score();
                }
            }
            DocumentScore de = new DocumentScore(current, score);
            if (scores.isEmpty() || scores.peek().score<score){
                scores.add(de);
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
                    e.printStackTrace();
                }
                if(postingList.docId()<min){
                    min=postingList.docId();
                }
            }
        }
        return min;
    }
}
