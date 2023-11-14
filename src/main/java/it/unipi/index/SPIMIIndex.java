package it.unipi.index;

import it.unipi.model.DocumentStream;
import it.unipi.model.Tokenizer;
import it.unipi.model.implementation.*;
import it.unipi.utils.*;

import java.io.*;
import java.util.*;

public class SPIMIIndex {
    /*
    public DocumentStream documentStream;
    public Tokenizer tokenizer;
    private long block_size = 10000; // Each block 10KB
    private int next_block = 0;
    private boolean finish = false;

    public SPIMIIndex(DocumentStream d, Tokenizer tok){
        documentStream = d;
        tokenizer = tok;
    }

    public void setLimit(int size){
        block_size = size;
    }

    public void buildIndexSPIMI() throws IOException {
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


    public void invertBlock() throws IOException {
        // Get the current state of memory
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMemory = startMemory;

        // Initialize structures
        InMemoryIndexing intermediateIndex = new InMemoryIndexing(documentStream, new DocumentIndexImpl(), new VocabularyImpl(), tokenizer);

        while(availableMemory(usedMemory, startMemory)){
            // Get the next document
            Document doc = documentStream.nextDoc();
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
        InMemoryIndexing.closeWriters();
        next_block++;
    }

    private void writeBlock(InMemoryIndexing intermediateIndex) throws IOException {
        intermediateIndex.dumpDocumentIndex("data/blocks/document_index"+next_block+".csv");
        InMemoryIndexing.assignWriters("data/blocks/vocabulary" + next_block + ".csv",
                "data/blocks/doc_ids" + next_block + ".txt",
                "data/blocks/term_frequencies" + next_block + ".txt", 0);
        intermediateIndex.dumpVocabulary();
    }

    private boolean finished() {
        return finish;
    }

    private boolean availableMemory(long usedMemory, long startMemory) {
        // Returns if (usedMemory - starting memory) is less than a treshold
        return (usedMemory - startMemory) <= block_size;
    }

    private void mergeAllBlocks() throws IOException {
        // Key idea to merge all blocks together
        // To do the merging, we open all block files simultaneously
        // We maintain small read buffers for blocks we are reading
        // and a write buffer for the final merged index we are writing.
        // In each iteration, we select the lowest termID that has not been processed yet
        // using a priority queue or a similar data structure.
        // All postings lists for this termID are read and merged, and the merged list is written back to disk.
        // Each read buffer is refilled from its file when necessary.

        // Initialize all buffers
        List<ReadingInterface> readVocabularyBuffers = new ArrayList<>();
        List<ReadingInterface> readDocIndexBuffers = new ArrayList<>();

        InMemoryIndexing.assignWriters(Constants.VOCABULARY_FILE, Constants.DOC_IDS_POSTING_FILE, Constants.TF_POSTING_FILE,0);

        // Initialize a Boolean list to check if the block has been processed
        List<Boolean> processed = new ArrayList<>();
        for(int i = 0; i < next_block; i++)
            processed.add(true);

        try{
            for(int i = 0; i < next_block; i++){
                readVocabularyBuffers.add(new ASCIIReader());
                readVocabularyBuffers.get(i).open("./data/blocks/vocabulary" + i + ".csv");
                // TODO: DocIndex reader
            }


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
                        System.out.println("PARAM:"+ lineK);
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
                VocabularyEntry entry = mergeEntries(toMerge, involvedBlocks);
                InMemoryIndexing.dumpVocabularyLine(new AbstractMap.SimpleEntry<>(lowestTerm, entry));
            }
            InMemoryIndexing.closeWriters();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }

    private VocabularyEntry mergeEntries(List<VocabularyEntry> toMerge,
                                         List<Integer> involvedBlocks
                                         ) throws IOException {

        // Make each entry to fetch relative postings
        PostingListImpl mergedList = new PostingListImpl();
        Integer frequency = 0;
        Double upperBound = 0.0;
        for(int k = 0; k < toMerge.size(); k++){
            toMerge.get(k).getPostingList().loadPosting(involvedBlocks.get(k));
            int L = mergedList.mergePosting(toMerge.get(k).getPostingList());

            // Update term frequency and upper bound
            frequency += toMerge.get(k).getDocumentFrequency();
            if(toMerge.get(k).getUpperBound() > upperBound)
                upperBound = toMerge.get(k).getUpperBound();
        }

        VocabularyEntry result = new VocabularyEntry();
        result.setDocumentFrequency(frequency);
        result.setUpperBound(upperBound);
        result.setPostingList(mergedList);

        return result;
    }
    */
}
