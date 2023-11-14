package it.unipi.utils;


import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.model.implementation.VocabularyEntry;

public interface Fetcher {
    public void loadPosting(PostingListImpl list);

    //
    // TERM | DF | UB | PostingList
    public VocabularyEntry loadVocEntry(String term);

    public DocumentIndexEntry loadDocEntry(long docId);

}
