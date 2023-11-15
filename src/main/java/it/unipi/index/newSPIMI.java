package it.unipi.index;

import it.unipi.model.DocumentStream;
import it.unipi.model.PostingList;
import it.unipi.model.implementation.Document;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.utils.Fetcher;
import it.unipi.utils.FetcherTXT;

import javax.print.Doc;
import java.io.*;
import java.util.*;

public class newSPIMI {
    // FIXME: UnderLock [Angelo]
    private DocumentStream stream;
    private InMemoryIndexing indexer;
    private long block_size = 10000;
    private int next_block = 0;
    private boolean finish = false;

    public newSPIMI(DocumentStream s, InMemoryIndexing i) {
        stream = s;
        indexer = i;
    }

    public void setLimit(int size) {
        block_size = size;
    }

    private boolean finished() {
        return finish;
    }

    private boolean availableMemory(long usedMemory, long startMemory) {
        // Returns if (usedMemory - starting memory) is less than a treshold
        return (usedMemory - startMemory) <= block_size;
    }

    public void buildIndexSPIMI() {
        // Preliminary flush of files
        for (File file : Objects.requireNonNull(new File("./data/blocks").listFiles()))
            if (!file.isDirectory())
                file.delete();

        // < Until documents are not finished >
        // < Create and Invert the block, write it to a file >
        // < Merge all blocks >
        while (!finished()) {
            invertBlock("data/blocks/_" + next_block);
        }

        // TODO: Implement
        mergeAllBlocks();
    }

    public void invertBlock(String filename) {
        // Get memory state
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMemory = startMemory;

        // Setup dumper
        indexer.setup(filename);

        while (availableMemory(usedMemory, startMemory)) {
            // Get the next document
            Optional<Document> doc = Optional.ofNullable(stream.nextDoc());
            if (doc.isEmpty()) {
                // When I have finished, I set the flag
                finish = true;
                break;
            }
            indexer.processDocument(doc.get());
            usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }

        // Dump when out of memory
        indexer.dumpVocabulary();
        //indexer.dumpDocumentIndex();

        // Reset dumper
        indexer.close();
        next_block++;
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

        List<Fetcher> readVocBuffers = new ArrayList<>();
        List<Boolean> processed = new ArrayList<>();
        indexer.setup("data/");

        for (int i = 0; i < next_block; i++) {
            readVocBuffers.add(new FetcherTXT());
            processed.add(true);
            readVocBuffers.get(i).start("data/blocks/_" + i);
        }

        int blocksClosed = 0;
        boolean[] closed = new boolean[next_block];

        String[] terms = new String[next_block];
        VocabularyEntry[] entries = new VocabularyEntry[next_block];
        String lowestTerm = null;

        while (true) {
            for (int k = 0; k < next_block; k++) {
                if (closed[k]) {
                    continue;
                }

                if (processed.get(k)) {
                    // Get a vocabulary entry
                    // Term | TermFrequency | UpperBound | #Posting | Offset
                    Map.Entry<String, VocabularyEntry> entry = readVocBuffers.get(k).loadVocEntry();
                    if (entry == null) {
                        blocksClosed++;
                        System.out.println("BLOCK CLOSED: " + k);
                        closed[k] = true;
                        continue;
                    }

                    terms[k] = entry.getKey();
                    entries[k] = entry.getValue();
                }
            }
            if (blocksClosed == next_block)
                break;

            lowestTerm = null;
            // Get the lowest lexicographically term
            for (int k = 0; k < next_block; k++) {
                if (lowestTerm == null) {
                    if (closed[k])
                        continue;
                    else
                        lowestTerm = terms[k];
                }
                if (lowestTerm.compareTo(terms[k]) > 0 && !closed[k]) {
                    lowestTerm = terms[k];
                }
            }
            //Merge entries with equal terms
            // Mark as processed correspondant blocks
            List<VocabularyEntry> toMerge = new ArrayList<>();
            for (int k = 0; k < next_block; k++) {
                if (lowestTerm.compareTo(terms[k]) == 0 && !closed[k]) {
                    toMerge.add(entries[k]);
                    processed.set(k, true);
                } else
                    processed.set(k, false);
            }

            // Write the merge onto the output file
            try {
                VocabularyEntry entry = mergeEntries(toMerge);
                indexer.dumpVocabularyLine(new AbstractMap.SimpleEntry<>(lowestTerm, entry));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        indexer.close();

    }


    private VocabularyEntry mergeEntries(List<VocabularyEntry> toMerge) throws IOException {

        // Make each entry to fetch relative postings
        PostingList mergedList = new PostingListImpl();
        Integer frequency = 0;
        Double upperBound = 0.0;
        for (int k = 0; k < toMerge.size(); k++) {
            int L = mergedList.mergePosting(toMerge.get(k).getPostingList());

            // Update term frequency and upper bound
            frequency += toMerge.get(k).getDocumentFrequency();
            if (toMerge.get(k).getUpperBound() > upperBound)
                upperBound = toMerge.get(k).getUpperBound();
        }

        VocabularyEntry result = new VocabularyEntry(frequency, upperBound, mergedList);

        return result;
    }
}
