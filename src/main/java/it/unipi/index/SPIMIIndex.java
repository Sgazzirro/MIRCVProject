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
    private long block_size = 1000000; // Each block 10KB
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
        InMemoryIndexing intermediateIndex = new InMemoryIndexing(documentStreamInterface, new DocumentIndex(), new Vocabulary(), tokenizerInterface);

        while(availableMemory(usedMemory, startMemory)){
            // Get the next document
            Document doc = documentStreamInterface.nextDoc();
            if(doc == null){
                // When I have finished, I set the flag
                finish = true;
                break;
            }
            intermediateIndex.processDocument(doc);
            usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }

        // When out of memory:
        // 1) sort vocabulary by term (already done by the tree map)
        // 2) write block to the disk

        writeBlock(intermediateIndex);
        next_block++;
    }

    private void writeBlock(InMemoryIndexing intermediateIndex) {
        intermediateIndex.dumpDocumentIndex("data/blocks/document_index"+next_block+".csv");
        intermediateIndex.dumpVocabulary("data/blocks/vocabulary" + next_block + ".csv",
                "data/blocks/doc_ids" + next_block + ".txt",
                "data/blocks/term_frequencies" + next_block + ".txt");
    }
    /*
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
            // int length = vocEntry.getPostingList().score(;

            output.append(term).append(",").append(termFrequency).append(",").append(upperBound).append(",")
                    .append(offset).append("\n");

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
    */
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
        List<BufferedReader> readVocabularyBuffers = new ArrayList<>();
        List<BufferedReader> readFrequenciesBuffers = new ArrayList<>();
        List<BufferedReader> readDocIdBuffers = new ArrayList<>();
        List<BufferedReader> readDocIndexBuffers = new ArrayList<>();

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
                readVocabularyBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/vocabulary" + i + ".csv")));
                readFrequenciesBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/term_frequencies" + i + ".txt")));
                readDocIdBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/doc_ids" + i + ".txt")));
                readFrequenciesBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/document_index" + i + ".json")));
            }
            writerDocId = new PrintWriter("./data/doc_ids.txt");
            writerDocIndex = new PrintWriter("./data/document_index.json");
            writerTermFrequencies = new PrintWriter("./data/term_frequencies.txt");
            writerVocabulary = new PrintWriter("./data/vocabulary.csv");

            // Now that they are all open, check the lowest term_ids
            while(true){
                // Pick objects one bye one
                String[] terms = new String[next_block];
                List<VocabularyEntry> entries = new ArrayList<>();
                for(int k = 0; k < next_block; k++){
                    System.out.println("BLOCCO: " + processed.get(k));
                    if(!processed.get(k)){
                        // Get a vocabulary entry
                        // Term | TermFrequency | UpperBound | #Posting | Offset

                        String lineK = readVocabularyBuffers.get(k).readLine();
                        // If block is finished, just close its buffer
                        if(lineK == null){
                            readVocabularyBuffers.get(k).close();
                        }

                        // Else, store the informations of the line
                        String[] lineParam = lineK.split(",");

                        // LineParam[0] : the term
                        terms[k] = lineParam[0];

                        // The other params can create a vocabularyEntry
                        entries.add(new VocabularyEntry(Integer.parseInt(lineParam[1]),
                                Double.parseDouble(lineParam[2]),
                                new PostingList(Integer.parseInt(lineParam[3]), Integer.parseInt(lineParam[4]))));
                    }
                }

                // Now write onto the writing file the correct things

                // Iter
                // Merge if equal term

            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
