package it.unipi.model;

import it.unipi.encoding.CompressionType;
import it.unipi.io.Fetcher;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static it.unipi.utils.Constants.CACHE;
import static it.unipi.utils.Constants.MEMORY_USED;

public class Vocabulary {
    private final NavigableMap<String, VocabularyEntry> table;
    private final Fetcher fetcher;
    private final CompressionType compression;
    private static final Logger logger = LoggerFactory.getLogger(Vocabulary.class);
    public Vocabulary() {
        this(Constants.getCompression());
    }

    public Vocabulary(CompressionType compression) {
        this(new TreeMap<>(), compression);
    }

    public Vocabulary(NavigableMap<String, VocabularyEntry> table, CompressionType compression) {
        this.table = table;
        this.compression = compression;

        fetcher = Fetcher.getFetcher(compression);
    }

    public Vocabulary(PriorityQueue<Map.Entry<String, Long>> init) {
        this();
        Constants.CACHING = false;
        while(init.size() > 0){
            if (MEMORY_USED() > 50)
                break;
            Map.Entry<String, Long> entry = init.poll();
            VocabularyEntry loaded = getEntry(entry.getKey());
            loaded.setTouches(entry.getValue());
        }
        Constants.CACHING = true;
    }

    public boolean isNotPresent(String term) {
        return !table.containsKey(term);
    }

    public void addEntry(String token, int docId) {
        VocabularyEntry entry;
        if (isNotPresent(token)) {
            entry = new VocabularyEntry(compression);
            table.put(token, entry);
        } else
            entry = getEntry(token);

        entry.addPosting(docId);
    }

    public VocabularyEntry getEntry(String token) {
        if (isNotPresent(token)) {
            try {
                fetcher.start(Constants.getPath());
                VocabularyEntry entry = fetcher.loadVocEntry(token);
                fetcher.close();

                if (entry != null) {
                    table.put(token, entry);
                    entry.touch();
                    CACHE(token, entry.getTouches());
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
        CACHE(token, entry.getTouches());
        return entry;
    }

    public Map<String, VocabularyEntry> getMapping() {
        return table;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vocabulary that = (Vocabulary) o;
        return Objects.equals(table, that.table) && compression == that.compression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, compression);
    }
}