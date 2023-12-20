package it.unipi.model.implementation;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;
import it.unipi.io.Fetcher;

import java.io.Serializable;
import java.util.*;

import static it.unipi.utils.Constants.CACHE;
import static it.unipi.utils.Constants.MEMORY_USED;

public class VocabularyImpl implements Vocabulary {

    private final NavigableMap<String, VocabularyEntry> table;
    private final Fetcher fetcher;

    public VocabularyImpl(){
        this(new TreeMap<>());
    }

    public VocabularyImpl(NavigableMap<String, VocabularyEntry> table){
        this.table = table;
        fetcher = Fetcher.getFetcher(Constants.getCompression());
    }

    public VocabularyImpl(PriorityQueue<Map.Entry<String, Long>> init) {
        this();
        for(Map.Entry<String, Long> entry : init.stream().toList()){
            if(MEMORY_USED() > 50)
                break;
            getEntry(entry.getKey()).setTouched(entry.getValue());
        }

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
                entry.touch();
                CACHE(token, entry.getTouch());
                return entry;
            }
            else
                return null;
        }
        VocabularyEntry entry = table.get(token);
        entry.touch();
        CACHE(token, entry.getTouch());
        return entry;
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
