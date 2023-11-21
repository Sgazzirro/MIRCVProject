package it.unipi.model.implementation;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;

import java.io.Serializable;
import java.util.*;

public class VocabularyImpl implements Vocabulary, Serializable {

    private final NavigableMap<String, VocabularyEntry> table;

    public VocabularyImpl(){
        table = new TreeMap<>();
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
            ve.setDocumentFrequency(0);
            ve.setPostingList(new PostingListImpl());
            ve.setUpperBound((double) 0);
            table.put(token,ve);
        }
        else
            ve = getEntry(token);

        ve.addPosting(docId);
    }

    @Override
    public VocabularyEntry getEntry(String token) {
        return table.get(token);
    }

    public void setEntry(String term, VocabularyEntry entry) {
        table.put(term, entry);
    }

    /*
    public TreeMap<String, VocabularyEntry> sortByTerm(){
        // TreeMap to store values of HashMap
        TreeMap<String, VocabularyEntry> sorted = new TreeMap<>();

        // Copy all data from hashMap into TreeMap
        sorted.putAll(table);
        return sorted;
    }
*/
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
        VocabularyImpl that = (VocabularyImpl) o;
        return Objects.equals(table.entrySet(), that.table.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(table);
    }
}
