package it.unipi.model;
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
    public VocabularyEntry addEntry(String token) {
        // TODO: Create a new entry into the vocabulary
        return null;
    }

    @Override
    public VocabularyEntry getEntry(String token) {
        return table.get(token);
    }

}
