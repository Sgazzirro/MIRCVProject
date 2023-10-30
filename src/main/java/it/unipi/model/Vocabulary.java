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
        if (isPresent(token)) {
            getEntry(token).setFrequency(getEntry(token).getFrequency() + 1);
            return getEntry(token);
        }
        VocabularyEntry ve = new VocabularyEntry();
        ve.setFrequency(1);
        // non so bene cosa intendi per location posting list
        ve.setLocationPostingList(0);
        ve.setLocationPostingList(0);
        table.put(token,ve);
        return ve;
    }

    @Override
    public VocabularyEntry getEntry(String token) {
        return table.get(token);
    }

}
