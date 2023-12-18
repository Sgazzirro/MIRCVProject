package it.unipi.index;

import it.unipi.encoding.Tokenizer;
import it.unipi.io.DocumentStream;
import it.unipi.model.*;
import it.unipi.model.implementation.*;
import it.unipi.io.Dumper;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static java.lang.System.exit;

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

    /*
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

     */
    private Vocabulary vocabulary;
    private final Dumper dumper;
    private final Tokenizer tokenizer;
    private DocumentIndex docIndex;

    public InMemoryIndexing(Vocabulary voc, Dumper d, DocumentIndex di){
        vocabulary = voc;
        dumper = d;
        tokenizer = Tokenizer.getInstance();
        docIndex = di;
    }

    boolean setup(Path filename) {
        return dumper.start(filename);
    }

    boolean close(){
        return dumper.end();
    }

    // FIXME: This function is only used when you write the fully index in memory
    public void buildIndex(DocumentStream tokenStream, Path filePath) {
        Optional<Document> document;

        if (!setup(filePath)) {
            System.err.println("Something strange in opening the file");
            exit(1);
        }

        while((document = Optional.ofNullable(tokenStream.nextDoc())).isPresent())
            processDocument(document.get());

        dumpVocabulary();
        dumpDocumentIndex();

        if (!close()) {
            System.err.println("Something strange in closing the file");
            exit(1);
        }
    }

    public boolean processDocument(Document document) {
        if(document == null || document.getText().isEmpty())
            return false;
        List<String> tokenized = tokenizer.tokenizeBySpace(document.getText());

        for (String token : tokenized) {
            vocabulary.addEntry(token, document.getId());
        }

        docIndex.addDocument(document.getId(), tokenized.size());
        return true;
    }

    void dumpVocabulary(){
        dumper.dumpVocabulary(vocabulary);
        // Flush
        vocabulary = Vocabulary.getInstance();
    }

    void dumpVocabularyLine(Map.Entry<String, VocabularyEntry> entry) throws IOException {
        // Onto the vocabulary
        // Term | DF | UpperBound | IDF | OffsetID | OffsetTF | #Posting
        dumper.dumpVocabularyEntry(entry);
    }

    void dumpDocumentIndex(){
        dumper.dumpDocumentIndex(docIndex);
        // Flush
        docIndex = new DocumentIndexImpl();
    }

    void dumpDocumentIndexLine(Map.Entry<Integer, DocumentIndexEntry> entry){
        dumper.dumpDocumentIndexEntry(entry);
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public DocumentIndex getDocIndex() {
        return docIndex;
    }

    public void computePartialTermUB() {
        // TODO: With BM25 serve l'average length, quindi stiamo attenti a come calcolare questo partial score perché forse in realtà non si può
        for(Map.Entry<String, VocabularyEntry> entry: vocabulary.getEntries()){
            VocabularyEntry vocabularyEntry = entry.getValue();
            List<Integer> termFreqList = vocabularyEntry.getPostingList().getTermFrequenciesDecompressedList();
            int maxTermFreq = Collections.max(termFreqList);

            // partial term upper bound = (1+log(tf))
            double partialTermUB = 1+Math.log10(maxTermFreq);
            vocabularyEntry.setUpperBound(partialTermUB);
        }
    }
}
