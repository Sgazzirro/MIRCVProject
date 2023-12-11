package it.unipi.utils;


import it.unipi.model.PostingList;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.VocabularyEntry;

import java.nio.file.Path;
import java.util.Map;

public interface Fetcher {

    boolean start(Path path);
    void loadPosting(PostingList list);

    // TERM | DF | UB | PostingList
    VocabularyEntry loadVocEntry(String term);

    Map.Entry<String, VocabularyEntry> loadVocEntry();

    DocumentIndexEntry loadDocEntry(long docId);

    Map.Entry<Integer, DocumentIndexEntry> loadDocEntry();

    boolean end();

    int[] getInformations();

    static Fetcher getFetcher(CompressionType compression) {
        switch (compression) {
            case DEBUG:
                return new FetcherTXT();
            case BINARY:
                return new FetcherBinary();
            case COMPRESSED:
                return new FetcherCompressed();
        }
        throw new RuntimeException("Unsupported compression type: " + compression);
    }
}
