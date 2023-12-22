package it.unipi.model.implementation;


import it.unipi.encoding.CompressionType;
import it.unipi.model.Vocabulary;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;
import it.unipi.io.Fetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static it.unipi.utils.Constants.CACHE;
import static it.unipi.utils.Constants.MEMORY_USED;

public class VocabularyImpl implements Vocabulary {

    private static final Logger logger = LoggerFactory.getLogger(Vocabulary.class);

    private final NavigableMap<String, VocabularyEntry> table;
    private final Fetcher fetcher;

    private final CompressionType compression;

    public VocabularyImpl() {
        this(Constants.getCompression());
    }

    public VocabularyImpl(CompressionType compression) {
        this(new TreeMap<>(), compression);
    }

    public VocabularyImpl(NavigableMap<String, VocabularyEntry> table, CompressionType compression) {
        this.table = table;
        this.compression = compression;

        fetcher = Fetcher.getFetcher(compression);
    }

    public VocabularyImpl(PriorityQueue<Map.Entry<String, Long>> init) {
        this();

        for (Map.Entry<String, Long> entry : init.stream().toList()){
            if (MEMORY_USED() > 50)
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
            entry = new VocabularyEntry(compression);
            table.put(token, entry);
        } else
            entry = getEntry(token);

        entry.addPosting(docId);
    }

    @Override
    public VocabularyEntry getEntry(String token) {
        if (!isPresent(token)) {
            try {
                fetcher.start(Constants.getPath());
                VocabularyEntry entry = fetcher.loadVocEntry(token);
                fetcher.close();

                if (entry != null) {
                    table.put(token, entry);
                    entry.touch();
                    CACHE(token, entry.getTouch());
                    return entry;
                }
                return null;
            } catch (Exception e) {
                logger.error("Could not fetch term " + token + " from vocabulary", e);
                return null;
            }
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
