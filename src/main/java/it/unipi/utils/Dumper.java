package it.unipi.utils;




import it.unipi.model.DocumentIndex;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.VocabularyEntry;

import java.io.IOException;
import java.util.Map;

public interface Dumper {

    // TODO (INTO THE IMPLEMENTATION): Private method long[] dumpPostings(PostingList ps)

    /**
     * Open the stream
     * @param path the path where to dump the files
     * @return whether the stream has been open correctly or an IOException has been raised
     */
    boolean start(String path);
    void dumpVocabularyEntry(Map.Entry<String, VocabularyEntry> entry) throws IOException;
    void dumpDocumentIndex(DocumentIndex docIndex);
    void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry);
    void dumpVocabulary(Vocabulary vocabulary);

    /**
     * Close the stream
     * @return whether the stream has been closed correctly or an IOException has been raised
     */
    boolean end();
}
