package it.unipi.index;


import it.unipi.model.implementation.*;
import it.unipi.model.*;
import it.unipi.utils.ASCIIWriter;
import it.unipi.utils.Constants;
import it.unipi.utils.WritingInterface;

import java.io.*;
import java.util.*;

public class InMemoryIndexing {
    /*
        Indexing following in-memory indexing pseudocode.
        Given a stream of documents D:
            - Update its document index entry
            - Tokenize it
            - For all tokens retrieved
                -> eventually update vocabulary entry
                -> update postings
     */

    public DocumentStreamInterface documentStreamInterface;
    public VocabularyInterface vocabulary;

    public DocumentIndexInterface docIndex;

    public TokenizerInterface tokenizerInterface;

    public static WritingInterface writerVOC;
    public static WritingInterface writerID;
    public static WritingInterface writerTF;

    public InMemoryIndexing(DocumentStreamInterface d, DocumentIndexInterface doc, VocabularyInterface v, TokenizerInterface tok){
        documentStreamInterface = d;
        docIndex = doc;
        vocabulary = v;
        tokenizerInterface = tok;
    }

    public static void closeWriters() throws IOException {
        writerTF.close();
        writerID.close();
        writerVOC.close();
    }

    public static void assignWriters(String fvoc, String fids, String ffreq, int mode) throws IOException {
        if(mode == 0){
            writerID = new ASCIIWriter();
            writerVOC = new ASCIIWriter();
            writerTF = new ASCIIWriter();
        }
        else{
            // TODO : BINARY WRITERS
        }
        // If writers were already assigned, close them
        if(writerID != null && writerID.isOpen())
            writerID.close();
        if(writerTF != null && writerTF.isOpen())
            writerTF.close();
        if(writerVOC != null && writerVOC.isOpen())
            writerVOC.close();

        writerVOC.open(fvoc);
        writerID.open(fids);
        writerTF.open(ffreq);
    }

    public void buildIndex() throws IOException {
        Document document;

        while((document = documentStreamInterface.nextDoc()) != null)
            processDocument(document);

        // Compute idf and upper bound for each term in the vocabulary
        // computeScores();

        // TODO - Improve the following methods by periodically dumping the memory instead of dumping it all at once
        dumpVocabulary();
        dumpDocumentIndex();
        writerVOC.close();
        writerID.close();
        writerTF.close();
    }

    private void computeScores() {
        int numDocuments = docIndex.getNumDocuments();

        // Iterate over all terms in the collection
        for (Map.Entry<String, VocabularyEntry> pair : vocabulary.getEntries()) {
            VocabularyEntry entry = pair.getValue();
            int documentFrequency = entry.getDocumentFrequency();

            // Compute IDf for each term
            PostingList postingList = entry.getPostingList();
            double idf = Math.log10((double) numDocuments / documentFrequency);
            postingList.setTermIdf(idf);

            // Compute upper bound for each term
            double upperBound = 0;
            do {
                // The posting list necessarily has at least a posting
                upperBound = Math.max(upperBound, postingList.score());
                postingList.next();
            } while (postingList.hasNext());
            postingList.reset();
        }
    }

    public void processDocument(Document document) {
        List<String> tokenized = tokenizerInterface.tokenizeBySpace(document.getText());

        for (String token : tokenized) {
            vocabulary.addEntry(token, document.getId());
        }

        docIndex.addDocument(document.getId(), tokenized.size());
    }
    protected static void dumpVocabularyLine(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        // Onto the vocabulary
        // Term | DF | UpperBound | IDF | OffsetID | OffsetTF | #Posting
        String term = entry.getKey();
        VocabularyEntry vocEntry = entry.getValue();

        int termFrequency = vocEntry.getDocumentFrequency();
        double upperBound = vocEntry.getUpperBound();
        double IDF = vocEntry.getPostingList().getTermIdf();

        PostingList postingList = vocEntry.getPostingList();
        long[] offsets = postingList.dumpPostings(writerID, writerTF);
        int length = postingList.getDocIdList().size();

        String result =  new StringBuilder().append(term).append(",")
                        .append(termFrequency).append(",")
                        .append(upperBound).append(",")
                        .append(IDF).append(",")
                        .append(offsets[0]).append(",")
                        .append(offsets[1]).append(",")
                        .append(length).append("\n").toString();

        writerVOC.write(result);
    }


    protected void dumpVocabulary() {
        try{
            for (Map.Entry<String, VocabularyEntry> entry : this.vocabulary.getEntries()) {
                dumpVocabularyLine(entry);
            }
        } catch (IOException e) {
            System.err.println("There has been an error in writing the vocabulary to file");
            e.printStackTrace();
        }
    }

    protected void dumpDocumentIndex(){
        dumpDocumentIndex(Constants.DOCUMENT_INDEX_FILE);
    }

    protected void dumpDocumentIndex(String filename) {

        // Dump documentIndex as csv with structure
        //  doc id | document length
        StringBuilder documentIndex = new StringBuilder();

        for (Map.Entry<Integer, DocumentIndexEntry> entry : this.docIndex.getEntries()) {
            int docId = entry.getKey();
            DocumentIndexEntry docEntry = entry.getValue();

            int docLength = docEntry.getDocumentLength();
            documentIndex.append(docId).append(",").append(docLength).append("\n");
        }

        // Write everything to file
        try (
                PrintWriter documentIndexWriter = new PrintWriter(filename)
        ) {
            documentIndexWriter.print(documentIndex);

        } catch (FileNotFoundException e) {
            System.err.println("There has been an error in writing the document index to file");
            e.printStackTrace();
        }
    }

    public DocumentStreamInterface getDocumentStreamInterface() {
        return documentStreamInterface;
    }

    public VocabularyInterface getVocabulary() {
        return vocabulary;
    }

    public DocumentIndexInterface getDocIndex() {
        return docIndex;
    }

    public TokenizerInterface getTokenizerInterface() {
        return tokenizerInterface;
    }
}
