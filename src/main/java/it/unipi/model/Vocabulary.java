package it.unipi.model;
import it.unipi.PostingList;

import java.util.HashMap;
public class Vocabulary implements VocabularyInterface {
    private HashMap<String, VocabularyEntry> table;

    public Vocabulary(){
        table = new HashMap<String, VocabularyEntry>();
    }

    @Override
    public boolean isPresent(String term) {
        return table.containsKey(term);
    }

    @Override
    public void addEntry(String token, int docid) {
        // TODO: add entry to the posting list
        VocabularyEntry ve;
        if (!isPresent(token)) {
            ve = new VocabularyEntry();
            ve.setFrequency(0);
            ve.setPostingList(new PostingList());
            ve.setUpperBound((double) 0);
            table.put(token,ve);
        }
        else ve=getEntry(token);
        ve.setFrequency(getEntry(token).getFrequency() + 1);

        ve.getPostingList().addPosting(docid);
    }

    @Override
    public VocabularyEntry getEntry(String token) {
        return table.get(token);
    }

}
