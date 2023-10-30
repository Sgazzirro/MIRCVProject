package it.unipi.model;

public interface VocabularyInterface {
    
    // Returns if the vocabulary has the selected term
    public boolean isPresent(String term);

    VocabularyEntry addEntry(String token);

    VocabularyEntry getEntry(String token);
}
