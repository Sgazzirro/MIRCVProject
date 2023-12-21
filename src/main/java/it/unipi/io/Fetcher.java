package it.unipi.io;


import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.encoding.CompressionType;
import it.unipi.io.implementation.FetcherBinary;
import it.unipi.io.implementation.FetcherCompressed;
import it.unipi.io.implementation.FetcherTXT;
import it.unipi.utils.Constants;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface Fetcher extends AutoCloseable {

    default boolean start() {
        return start(Constants.getPath());
    }

    /**
     * Open the stream
     * @param path the path where to dump the files
     * @return whether the stream has been open correctly or an IOException has been raised
     */
    boolean start(Path path);

    VocabularyEntry loadVocEntry(String term) throws IOException;
    Map.Entry<String, VocabularyEntry> loadVocEntry() throws IOException;
    void loadPosting(VocabularyEntry entry) throws IOException;

    DocumentIndexEntry loadDocEntry(long docId) throws IOException;
    Map.Entry<Integer, DocumentIndexEntry> loadDocEntry() throws IOException;
    int[] getDocumentIndexStats() throws IOException;

    static Fetcher getFetcher(CompressionType compression) {
        return switch (compression) {
            case DEBUG -> new FetcherTXT();
            case BINARY -> new FetcherBinary();
            case COMPRESSED -> new FetcherCompressed();
        };
    }
}
