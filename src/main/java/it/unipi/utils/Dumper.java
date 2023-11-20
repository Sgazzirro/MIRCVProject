package it.unipi.utils;




import it.unipi.model.DocumentIndex;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.VocabularyEntry;

import java.util.Map;

public interface Dumper {

    // TODO (INTO THE IMPLEMENTATION): Private method long[] dumpPostings(PostingList ps)

    /**
     * Open the stream
     * @param filename the file to open
     * @return whether the stream has been open correctly or an IOException has been raised
     */
    public boolean start(String filename);
    public void dumpVocabularyEntry(Map.Entry<String, VocabularyEntry> entry);
    public void dumpDocumentIndex(DocumentIndex docIndex);
    public void dumpDocumentIndexEntry(Map.Entry<Integer, DocumentIndexEntry> entry);
    public void dumpVocabulary(Vocabulary vocabulary);

    /**
     * Close the stream
     * @return whether the stream has been closed correctly or an IOException has been raised
     */
    public boolean end();
}
