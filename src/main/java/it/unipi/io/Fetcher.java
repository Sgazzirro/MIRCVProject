package it.unipi.io;


import it.unipi.model.PostingList;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.encoding.CompressionType;
import it.unipi.io.implementation.FetcherBinary;
import it.unipi.io.implementation.FetcherCompressed;
import it.unipi.io.implementation.FetcherTXT;

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
