package it.unipi.model;

import it.unipi.encoding.CompressionType;
import it.unipi.model.implementation.VocabularyImpl;

import java.util.Map;

public interface Vocabulary {
    
    // Returns whether the vocabulary has the term inside
    boolean isPresent(String term);

    // Add the posting docid and create the entry in the vocabulary if never-seen token
    void addEntry(String token, int docid);

    // Returns the vocEntry of the term
    VocabularyEntry getEntry(String token);

    // Returns an iterator over term, entry pairs
    Iterable<Map.Entry<String, VocabularyEntry>> getEntries();

    static Vocabulary getInstance() {
        return new VocabularyImpl();
    }

    static Vocabulary getInstance(CompressionType compression) {
        return new VocabularyImpl(compression);
    }
}
