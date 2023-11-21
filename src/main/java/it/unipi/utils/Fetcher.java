package it.unipi.utils;


import it.unipi.model.PostingList;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;

import java.util.Map;

public interface Fetcher {

    boolean start(String path);
    void loadPosting(PostingList list);

    // TERM | DF | UB | PostingList
    VocabularyEntry loadVocEntry(String term);

    Map.Entry<String, VocabularyEntry> loadVocEntry();

    DocumentIndexEntry loadDocEntry(long docId);

    Map.Entry<Integer, DocumentIndexEntry> loadDocEntry();

    boolean end();

}
