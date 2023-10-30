package it.unipi.model;

public interface VocabularyInterface {
    
    // Returns if the vocabulary has the selected term
    public boolean isPresent(String term);

    void addEntry(String token, int docid);

    VocabularyEntry getEntry(String token);

}
