package it.unipi.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.Tree;
import it.unipi.model.DocumentIndexInterface;
import it.unipi.model.DocumentStreamInterface;
import it.unipi.model.TokenizerInterface;
import it.unipi.model.VocabularyInterface;
import it.unipi.model.implementation.*;
import opennlp.tools.stemmer.PorterStemmer;

import java.io.*;
import java.util.*;

public class SPIMIIndex {
    public DocumentStreamInterface documentStreamInterface;
    public TokenizerInterface tokenizerInterface;
    private long block_size = Runtime.getRuntime().totalMemory(); // Each block 10KB
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
        mergeAllBlocks();

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

    private void mergeAllBlocks() {
        // Key idea to merge all blocks together
        // To do the merging, we open all block files simultaneously
        // We maintain small read buffers for blocks we are reading
        // and a write buffer for the final merged index we are writing.
        // In each iteration, we select the lowest termID that has not been processed yet
        // using a priority queue or a similar data structure.
        // All postings lists for this termID are read and merged, and the merged list is written back to disk.
        // Each read buffer is refilled from its file when necessary.

        // Initialize all buffers
        List<ObjectInputStream> readVocabularyBuffers = new ArrayList<>();
        List<ObjectInputStream> readFrequenciesBuffers = new ArrayList<>();
        List<BufferedReader> readDocIdBuffers = new ArrayList<>();
        List<ObjectInputStream> readDocIndexBuffers = new ArrayList<>();

        PrintWriter writerDocId;
        PrintWriter writerDocIndex;
        PrintWriter writerTermFrequencies;
        PrintWriter writerVocabulary;

        // Initialize a Boolean list to check if the block has been processed
        List<Boolean> processed = new ArrayList<>();
        for(int i = 0; i < next_block; i++)
            processed.add(false);

        try{
            System.out.println("NEXT BLOCK: " + next_block);
            for(int i = 0; i < next_block; i++){
/*                readVocabularyBuffers.add(
                        new ObjectInputStream(
                                new FileInputStream("./data/blocks/vocabulary" + (next_block - 1) + ".csv")));
                readFrequenciesBuffers.add(
                        new ObjectInputStream(
                                new FileInputStream("./data/blocks/term_frequencies" + (next_block - 1) + ".txt")));
     */           readDocIdBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/doc_ids" + (next_block - 1) + ".txt")));
    /*            readFrequenciesBuffers.add(
                        new ObjectInputStream(
                                new FileInputStream("./data/blocks/document_index" + (next_block - 1) + ".txt")));
      */      }
            writerDocId = new PrintWriter("./data/doc_ids.txt");
            writerDocIndex = new PrintWriter("./data/document_index.json");
            writerTermFrequencies = new PrintWriter("./data/term_frequencies.txt");
            writerVocabulary = new PrintWriter("./data/vocabulary.csv");

            // Now that they are all open, check the lowest term_ids
            //while(true){
                // Pick objects one bye one
                List<String> terms = new ArrayList<>();
                List<VocabularyEntry> entries = new ArrayList<>();
                for(int k = 0; k < next_block; k++){
                    System.out.println("BLOCCO: " + processed.get(k));
                    if(!processed.get(k)){
                        // Vocabulary prova = (Vocabulary) readVocabularyBuffers.get(k).readObject();
                        int prova = Integer.parseInt(readDocIdBuffers.get(k).readLine());
                        System.out.println(prova);
                    }

                }
                // Merge if equal term
            // Advance only blocks that have been parsed at this iteration
            for(int i = 0; i < next_block; i++){
                readVocabularyBuffers.get(i).close();
                readDocIdBuffers.get(i).close();
                readDocIndexBuffers.get(i).close();
                readFrequenciesBuffers.get(i).close();
            }
            //}
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
