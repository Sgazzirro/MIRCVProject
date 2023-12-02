package it.unipi.index;

import it.unipi.model.*;
import it.unipi.model.implementation.*;
import it.unipi.utils.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class newSPIMI {
    private DocumentStream stream;
    private InMemoryIndexing indexer;
    private long block_size = 10000;
    private int next_block = 0;
    private boolean finish = false;
    private String path;
    private String mode;

    private InMemoryIndexing blockIndexer;

    public newSPIMI(String mode,DocumentStream s, InMemoryIndexing i) {
        this.mode = mode;
        stream = s;
        indexer = i;

        DocumentStream ds = new DocumentStreamImpl(Constants.COLLECTION_FILE);
        DocumentIndex di = new DocumentIndexImpl();
        Vocabulary v = new VocabularyImpl();
        Dumper d = mode.equals("DEBUG") ? new DumperTXT() : new DumperBinary();
        blockIndexer = new InMemoryIndexing(ds, v, d, di);
    }

    public void setLimit(int size) {
        block_size = size;
    }

    boolean finished() {
        return finish;
    }

    int getNext_block(){return next_block;}

    boolean availableMemory(long usedMemory, long startMemory) {
        // Returns if (usedMemory - starting memory) is less than a treshold
        return (usedMemory - startMemory) <= block_size;
    }

    public void buildIndexSPIMI(String path) {
        this.path = path;
        IOUtils.createDirectory(path + "blocks/");

        // Preliminary flush of files
        for (File file : Objects.requireNonNull(new File(path + "blocks").listFiles()))
            if (!file.isDirectory())
                file.delete();

        // < Until documents are not finished >
        // < Create and Invert the block, write it to a file >
        // < Merge all blocks >
        while (!finished())
            invertBlock(path + "blocks/_" + next_block);

        // Intermediate blocks are generated in debug mode
        List<Fetcher> readVocBuffers = new ArrayList<>();
        for (int i = 0; i < next_block; i++) {
             if(mode.equals("DEBUG"))
                readVocBuffers.add(new FetcherTXT());
             else
                readVocBuffers.add(new FetcherBinary());


            readVocBuffers.get(i).start(path + "blocks/_" + i);
        }
        mergeAllBlocks(readVocBuffers);
        concatenateDocIndexes(readVocBuffers);
    }



    public void invertBlock(String filename) {
        // Get memory state
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMemory = startMemory;

        // Setup block index
        blockIndexer.setup(filename);

        while (availableMemory(usedMemory, startMemory)) {
            // Get the next document
            Optional<Document> doc = Optional.ofNullable(stream.nextDoc());
            if (doc.isEmpty()) {
                // When I have finished, I set the flag
                finish = true;
                break;
            }
            blockIndexer.processDocument(doc.get());
            usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }

        // Dump when out of memory
        blockIndexer.dumpVocabulary();
        blockIndexer.dumpDocumentIndex();

        // Reset dump
        blockIndexer.close();
        next_block++;
    }

    void mergeAllBlocks(List<Fetcher> readVocBuffers) {
        // Key idea to merge all blocks together
        // To do the merging, we open all block files simultaneously
        // We maintain small read buffers for blocks we are reading
        // and a write buffer for the final merged index we are writing.
        // In each iteration, we select the lowest termID that has not been processed yet
        // using a priority queue or a similar data structure.
        // All postings lists for this termID are read and merged, and the merged list is written back to disk.
        // Each read buffer is refilled from its file when necessary.
        //List<Fetcher> readVocBuffers = new ArrayList<>();
        List<Boolean> processed = new ArrayList<>();
        int next_block = getNext_block();
        indexer.setup(path);

        for (int i = 0; i < next_block; i++) {
            processed.add(true);
        }

        int blocksClosed = 0;
        boolean[] closed = new boolean[next_block];

        String[] terms = new String[next_block];
        VocabularyEntry[] entries = new VocabularyEntry[next_block];
        String lowestTerm;
        while (true) {
            for (int k = 0; k < next_block; k++) {
                if (closed[k])
                    continue;

                if (processed.get(k)) {
                    // Get a vocabulary entry
                    // Term | TermFrequency | UpperBound | #Posting | Offset
                    Map.Entry<String, VocabularyEntry> entry = readVocBuffers.get(k).loadVocEntry();
                    if (entry == null) {
                        System.out.println("CLOSED BLOCKS: " + blocksClosed);
                        blocksClosed++;
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
            // Merge entries with equal terms
            // Mark as processed correspondent blocks
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
    }

    private void concatenateDocIndexes(List<Fetcher> readers) {
        if(!mode.equals("DEBUG")) {
            int N = 0;
            int L = 0;
            for (int i = 0; i < next_block; i++) {
                int[] info = readers.get(i).getInformations();
                N += info[0];
                L += info[1];
            }
            DocumentIndex di = new DocumentIndexImpl();
            indexer.getDocIndex().setNumDocuments(N);
            indexer.getDocIndex().setTotalLength(L);
            indexer.dumpDocumentIndex();
        }
            for (int i = 0; i < next_block; i++) {
                Map.Entry<Integer, DocumentIndexEntry> entry;
                while ((entry = readers.get(i).loadDocEntry()) != null) {
                    indexer.dumpDocumentIndexLine(entry);
                }
                readers.get(i).end();
            }

        indexer.close();
    }

    VocabularyEntry mergeEntries(List<VocabularyEntry> toMerge) throws IOException {

        // Make each entry to fetch relative postings
        PostingListImpl mergedList = new PostingListImpl();
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
