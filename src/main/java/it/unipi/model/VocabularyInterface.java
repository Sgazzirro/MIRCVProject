package it.unipi.model;

import it.unipi.model.implementation.VocabularyEntry;

public interface VocabularyInterface {
    
    // Returns whether the vocabulary has the term inside
    public boolean isPresent(String term);

    // Add the posting docid and create the entry in the vocabulary if never-seen token
    void addEntry(String token, int docid);

    // Returnes the vocEntry of the term
    VocabularyEntry getEntry(String token);

}
