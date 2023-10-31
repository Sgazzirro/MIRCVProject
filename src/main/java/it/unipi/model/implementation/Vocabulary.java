package it.unipi.model.implementation;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unipi.model.VocabularyInterface;

import java.io.Serializable;
import java.util.HashMap;
public class Vocabulary implements VocabularyInterface, Serializable {
    @JsonProperty("VocabularyTable")
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

    @Override
    public String toString(){
        return "Table: "+table.toString();
    }


}
