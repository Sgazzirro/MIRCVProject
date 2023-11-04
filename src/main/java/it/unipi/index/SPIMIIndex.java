package it.unipi.index;

import it.unipi.model.DocumentStreamInterface;
import it.unipi.model.TokenizerInterface;
import it.unipi.model.implementation.*;
import it.unipi.utils.LoadStructures;

import java.io.*;
import java.util.*;

public class SPIMIIndex {
    public DocumentStreamInterface documentStreamInterface;
    public TokenizerInterface tokenizerInterface;
    private long block_size = 1000000; // Each block 10KB
    private int next_block = 0;
    private boolean finish = false;
    private int vocOffset;

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

        BufferedWriter writerDocId;
        BufferedWriter writerDocIndex;
        BufferedWriter writerTermFrequencies;
        BufferedWriter writerVocabulary;

        // Initialize a Boolean list to check if the block has been processed
        List<Boolean> processed = new ArrayList<>();
        for(int i = 0; i < next_block; i++)
            processed.add(true);

        try{
            System.out.println("NEXT BLOCK: " + next_block);
            for(int i = 0; i < next_block; i++){
                readVocabularyBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/vocabulary" + i + ".csv")));
               /* readFrequenciesBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/term_frequencies" + i + ".txt")));
                readDocIdBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/doc_ids" + i + ".txt")));

                */
                readFrequenciesBuffers.add(
                        new BufferedReader(
                                new FileReader("./data/blocks/document_index" + i + ".csv")));
            }
            writerDocId = new BufferedWriter(new FileWriter("./data/doc_ids.txt", true));
            writerDocIndex = new BufferedWriter(new FileWriter("./data/document_index.json", true));
            writerTermFrequencies = new BufferedWriter(new FileWriter("./data/term_frequencies.txt", true));
            writerVocabulary = new BufferedWriter(new FileWriter("./data/vocabulary.csv", true));

            int blockClosed = 0;
            boolean[] closed = new boolean[next_block];

            String[] terms = new String[next_block];
            VocabularyEntry[] entries = new VocabularyEntry[next_block];
            String lowestTerm = null;
            while(true){
                // Pick objects one bye one

                for(int k = 0; k < next_block; k++){
                    if(closed[k]) {
                        continue;
                    }

                    if(processed.get(k)){
                        // Get a vocabulary entry
                        // Term | TermFrequency | UpperBound | #Posting | Offset

                        String lineK = LoadStructures.loadLine(readVocabularyBuffers.get(k));
                        if(lineK == null) {
                            blockClosed++;
                            System.out.println("BLOCK CLOSED: " + k);
                            closed[k] = true;
                            continue;
                        }

                        // Else, store the informations of the line
                        String[] lineParam = lineK.split(",");

                        // LineParam[0] : the term
                        terms[k] = lineParam[0];
                        // The other params can create a vocabularyEntry
                        entries[k] = new VocabularyEntry(lineParam);
                    }
                }

                // TODO: Embed this function inside the above loop
                if(blockClosed == next_block) {
                    // Regarding documents indexes, It's just a concatenation
                    // TODO: CONCATENATION OF DOCUMENT INDEXES
                    break;
                }
                lowestTerm = null;
                // Get the lowest lexicographically term
                for(int k = 0; k < next_block; k++){
                    if(lowestTerm == null){
                        if(closed[k])
                            continue;
                        else
                            lowestTerm = terms[k];
                    }
                    if(lowestTerm.compareTo(terms[k]) > 0 && !closed[k]){
                        lowestTerm = terms[k];
                    }
                }

                //Merge entries with equal terms
                // Mark as processed correspondant blocks
                List<VocabularyEntry> toMerge = new ArrayList<>();
                List<Integer> involvedBlocks = new ArrayList<>();
                for(int k = 0; k < next_block; k++){
                    if(lowestTerm.compareTo(terms[k]) == 0 && !closed[k]){
                        toMerge.add(entries[k]);
                        involvedBlocks.add(k);
                        processed.set(k, true);
                    }
                    else
                        processed.set(k, false);


                }

                // Write the merge onto the output file
                mergeEntries(lowestTerm, toMerge, involvedBlocks, writerVocabulary, writerDocId, writerTermFrequencies);
            }
            writerDocId.close();
            writerDocIndex.close();
            writerVocabulary.close();
            writerTermFrequencies.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }

    private void mergeEntries(String term, List<VocabularyEntry> toMerge,
                              List<Integer> involvedBlocks,
                              BufferedWriter writerVocabulary, BufferedWriter writerDocId, BufferedWriter writerTermFrequencies) throws IOException {

        // Make each entry to fetch relative postings
        PostingList mergedList = new PostingList();
        Integer frequency = 0;
        Double upperBound = 0.0;
        Integer length = 0;
        StringJoiner docids = new StringJoiner("\n");
        StringJoiner termFrequencies = new StringJoiner("\n");
        for(int k = 0; k < toMerge.size(); k++){
            toMerge.get(k).getPostingList().loadPosting(involvedBlocks.get(k));
            mergedList.mergePosting(toMerge.get(k).getPostingList());

            // Update term frequency and upper bound
            frequency += toMerge.get(k).getFrequency();
            if(toMerge.get(k).getUpperBound() > upperBound)
                upperBound = toMerge.get(k).getUpperBound();

        }

        // Get length
        length = mergedList.dumpPostings(docids, termFrequencies);
        System.out.println("DUMPED DOCS : " + docids);
        StringBuilder result = new StringBuilder();
        result.append(term).append(",")
                .append(frequency).append(",")
                .append(upperBound).append(",")
                .append(vocOffset).append(",")
                .append(length).append("\n");
        vocOffset += length;   // Advance the offset by the length of the current posting list

        writerVocabulary.write(result.toString());
        writerDocId.write(docids.toString());
        writerTermFrequencies.write(termFrequencies.toString());
    }

}
