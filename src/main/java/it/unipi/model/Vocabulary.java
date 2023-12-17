package it.unipi.model;

import java.util.Map;

public interface Vocabulary {
    
    // Returns whether the vocabulary has the term inside
    public boolean isPresent(String term);

    // Add the posting docid and create the entry in the vocabulary if never-seen token
    void addEntry(String token, int docid);

    // Returns the vocEntry of the term
    VocabularyEntry getEntry(String token);

    // Returns an iterator over term, entry pairs
    public Iterable<Map.Entry<String, VocabularyEntry>> getEntries();

    // public TreeMap<String, VocabularyEntry> sortByTerm();
}
