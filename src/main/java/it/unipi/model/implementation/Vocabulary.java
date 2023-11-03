package it.unipi.model.implementation;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unipi.model.VocabularyInterface;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class Vocabulary implements VocabularyInterface, Serializable {

    private final HashMap<String, VocabularyEntry> table;

    public Vocabulary(){
        table = new HashMap<>();
    }

    public Vocabulary(String term, Integer freq, Double upperBound, Integer offset){
        table = new HashMap<>();
        table.put(term, new VocabularyEntry(freq, upperBound, new PostingList(offset)));
    }
    @Override
    public boolean isPresent(String term) {
        return table.containsKey(term);
    }

    @Override
    public void addEntry(String token, int docId) {
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

        ve.getPostingList().addPosting(docId);
    }

    @Override
    public VocabularyEntry getEntry(String token) {
        return table.get(token);
    }

    public void setEntry(String term, VocabularyEntry entry) {
        table.put(term, entry);
    }

    public TreeMap<String, VocabularyEntry> sortByTerm(){
        // TreeMap to store values of HashMap
        TreeMap<String, VocabularyEntry> sorted = new TreeMap<>();

        // Copy all data from hashMap into TreeMap
        sorted.putAll(table);
        return sorted;
    }

    @Override
    public Iterable<Map.Entry<String, VocabularyEntry>> getEntries() {
        return table.entrySet();
    }

    @Override
    public String toString(){
        return "Table: " + table;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vocabulary that = (Vocabulary) o;
        return Objects.equals(table.entrySet(), that.table.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(table);
    }
}
