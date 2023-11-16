package it.unipi.utils;


import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.model.implementation.VocabularyEntry;

import java.util.Map;

public interface Fetcher {
    public boolean start(String filename);
    public void setScope(String filename);
    public void loadPosting(PostingList list);

    //
    // TERM | DF | UB | PostingList
    public Map.Entry<String, VocabularyEntry> loadVocEntry(String term);

    public Map.Entry<String, VocabularyEntry> loadVocEntry();

    public DocumentIndexEntry loadDocEntry(long docId);

    public Map.Entry<Integer, DocumentIndexEntry> loadDocEntry();

    public boolean end();

    //

}
