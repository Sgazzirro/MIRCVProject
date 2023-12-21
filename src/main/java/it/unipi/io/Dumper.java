package it.unipi.io;




import it.unipi.model.DocumentIndex;
import it.unipi.model.Vocabulary;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.encoding.CompressionType;
import it.unipi.io.implementation.DumperBinary;
import it.unipi.io.implementation.DumperCompressed;
import it.unipi.io.implementation.DumperTXT;
import it.unipi.utils.Constants;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface Dumper extends AutoCloseable {

    default boolean start() {
        return start(Constants.getPath());
    }

    /**
     * Open the stream
     * @param path the path where to dump the files
     * @return whether the stream has been open correctly or an IOException has been raised
     */
    boolean start(Path path);

    void dumpVocabulary(Vocabulary vocabulary) throws IOException;
    void dumpVocabularyEntry(Map.Entry<String, VocabularyEntry> entry) throws IOException;

    void dumpDocumentIndex(DocumentIndex docIndex) throws IOException;
    void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry) throws IOException;

    static Dumper getInstance(CompressionType compression) {
        return switch (compression) {
            case DEBUG -> new DumperTXT();
            case BINARY -> new DumperBinary();
            case COMPRESSED -> new DumperCompressed();
        };
    }

}
