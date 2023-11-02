package it.unipi.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.source.tree.Tree;
import it.unipi.model.DocumentIndexInterface;
import it.unipi.model.DocumentStreamInterface;
import it.unipi.model.TokenizerInterface;
import it.unipi.model.VocabularyInterface;
import it.unipi.model.implementation.*;
import opennlp.tools.stemmer.PorterStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class SPIMIIndex {
    public DocumentStreamInterface documentStreamInterface;
    public TokenizerInterface tokenizerInterface;
    private long block_size = 128000; // Each block 10KB

    private int next_block = 0;
    private boolean finish = false;

    public SPIMIIndex(DocumentStreamInterface d, TokenizerInterface tok){
        documentStreamInterface = d;
        tokenizerInterface = tok;
    }

    public void setLimit(int size){
        block_size = size;
    }

    public void buildIndexSPIMI(){
        for(File file: Objects.requireNonNull(new File("./data/blocks").listFiles()))
            if (!file.isDirectory())
                file.delete();

        // < Until documents are not finished >
        // < Create and Invert the block, write it to a file >
        // < Merge all blocks >

        while(!finished()){
            invertBlock();
        }

        // TODO: Merge all blocks
        //mergeAllBlocks();

    }

    public void invertBlock(){
        // Get the current state of memory
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMemory = startMemory;

        // Initialize structures
        VocabularyInterface currentVoc = new Vocabulary();
        DocumentIndexInterface currentDocIndex = new DocumentIndex();

        while(availableMemory(usedMemory, startMemory)){
            // Get the next document
            Document doc = documentStreamInterface.nextDoc();
            if(doc == null){
                // When I have finished, I set the flag
                finish = true;
                break;
            }
            List<String> tokenized = tokenizerInterface.tokenizeBySpace(doc.getText());
            // tokenized.removeAll(stopwords);     // Remove all stopwords

            // Use Porter stemmer
            PorterStemmer stemmer = new PorterStemmer();

            for (String token : tokenized) {
                currentVoc.addEntry(stemmer.stem(token), doc.getId());
            }
            currentDocIndex.addDocument(doc.getId(), tokenized.size());
            usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }

        // When out of memory:
        // 1) sort vocabulary by term
        // 2) write block to the disk
        TreeMap<String, VocabularyEntry> treeVoc = currentVoc.sortByTerm();
        writeBlock(treeVoc, currentDocIndex);
        next_block++;
    }

    private void writeBlock(TreeMap<String, VocabularyEntry> currentVoc, DocumentIndexInterface currentDocIndex) {
        dumpVocabulary(currentVoc);
        dumpDocumentIndex(currentDocIndex);
    }

    private void dumpVocabulary(TreeMap<String, VocabularyEntry> vocabulary) {
        // InitializeFilenames
        String vocabularyFile = "data/blocks/vocabulary" + next_block + ".csv";
        String docIdsFile = "data/blocks/doc_ids" + next_block + ".txt";
        String termFrequenciesFile = "data/blocks/term_frequencies" + next_block + ".txt";

        // Dump vocabulary as csv with structure
        //  term | frequency | upper bound | offset in postings
        StringBuilder output = new StringBuilder();
        StringJoiner docIds = new StringJoiner("\n");
        StringJoiner termFrequencies = new StringJoiner("\n");

        // Keep track of the offset of each term in the posting list
        int offset = 0;

        for (Map.Entry<String, VocabularyEntry> entry : vocabulary.entrySet()) {
            String term = entry.getKey();
            VocabularyEntry vocEntry = entry.getValue();

            int termFrequency = vocEntry.getFrequency();
            double upperBound = vocEntry.getUpperBound();

            output.append(term).append(",").append(termFrequency).append(",").append(upperBound).append(",").append(offset).append("\n");

            PostingList postingList = vocEntry.getPostingList();
            offset += postingList.dumpPostings(docIds, termFrequencies);
        }

        // Write everything to file
        try (
                PrintWriter vocabularyWriter = new PrintWriter(vocabularyFile);
                PrintWriter docIdsWriter = new PrintWriter(docIdsFile);
                PrintWriter termFrequenciesWriter = new PrintWriter(termFrequenciesFile)
        ) {
            vocabularyWriter.print(output);
            docIdsWriter.print(docIds);
            termFrequenciesWriter.print(termFrequencies);

        } catch (FileNotFoundException e) {
            System.err.println("There has been an error in writing the vocabulary to file");
            e.printStackTrace();
        }
    }

    private void dumpDocumentIndex(DocumentIndexInterface docIndex) {
        String documentIndexFile = "data/blocks/document_index"+next_block+".json";

        // Dump document index as Json
        ObjectMapper objectMapper = new ObjectMapper();
        try(
                PrintWriter documentIndexWriter = new PrintWriter(documentIndexFile)
        ) {
            String documentIndexJSON = objectMapper.writeValueAsString(docIndex);
            documentIndexWriter.println(documentIndexJSON);

        } catch (JsonProcessingException | FileNotFoundException e) {
            System.err.println("There has been an error in writing the document index to file");
            e.printStackTrace();
        }
    }

    private boolean finished() {
        return finish;
    }

    private boolean availableMemory(long usedMemory, long startMemory) {
        // Returns if (usedMemory - starting memory) is less than a treshold
        return (usedMemory - startMemory) <= block_size;
    }

}
