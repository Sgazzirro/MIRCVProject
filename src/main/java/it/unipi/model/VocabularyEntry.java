package it.unipi.model;

import it.unipi.model.implementation.PostingListImpl;
import it.unipi.model.implementation.VocabularyEntryImpl;

public interface VocabularyEntry {

    void addPosting(int docId);

    int getDocumentFrequency();

    void setDocumentFrequency(int documentFrequency);

    double getUpperBound();

    void setUpperBound(double upperBound);

    PostingList getPostingList();

    void setPostingList(PostingList postingList);

    static it.unipi.model.VocabularyEntry parseTXT(String line) {
        String[] params = line.split(",");

        int documentFrequency = Integer.parseInt(params[1]);
        double upperBound = Double.parseDouble(params[2]);
        PostingList postingList = new PostingListImpl();
        postingList.setIdf(Double.parseDouble(params[3]));
        postingList.setDocIdsOffset(Long.parseLong(params[4]));
        postingList.setTermFreqOffset(Long.parseLong(params[5]));
        int length = Integer.parseInt(params[6]);
        postingList.setDocIdsLength(length);
        postingList.setTermFreqLength(length);

        return new VocabularyEntryImpl(documentFrequency, upperBound, postingList);
    }
}
