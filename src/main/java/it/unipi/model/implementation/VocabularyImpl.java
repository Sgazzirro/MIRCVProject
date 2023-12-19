package it.unipi.model.implementation;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;
import it.unipi.io.Fetcher;

import java.io.Serializable;
import java.util.*;

public class VocabularyImpl implements Vocabulary {

    private final NavigableMap<String, VocabularyEntry> table;
    private final Fetcher fetcher;

    public VocabularyImpl(){
        table = new TreeMap<>();
        fetcher = Fetcher.getFetcher(Constants.getCompression());
    }

    @Override
    public boolean isPresent(String term) {
        return table.containsKey(term);
    }

    @Override
    public void addEntry(String token, int docId) {
        VocabularyEntry entry;
        if (!isPresent(token)) {
            entry = new VocabularyEntry();
            table.put(token, entry);
        } else
            entry = getEntry(token);

        entry.addPosting(docId);
    }

    @Override
    public VocabularyEntry getEntry(String token) {
        if(!isPresent(token)){
            fetcher.start(Constants.getPath());
            VocabularyEntry entry = fetcher.loadVocEntry(token);
            fetcher.end();
            if(entry!=null){
                table.put(token, entry);
                return entry;
            }
        }
        return table.get(token);
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
        VocabularyImpl that = (VocabularyImpl) o;
        return Objects.equals(table.entrySet(), that.table.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(table);
    }
}
