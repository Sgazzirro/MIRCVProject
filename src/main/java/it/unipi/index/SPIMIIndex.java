package it.unipi.index;

import it.unipi.model.*;
import it.unipi.model.implementation.*;
import it.unipi.utils.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class SPIMIIndex {

    /**
     * The stream that generates one Document at a time
     */
    private DocumentStream stream;
    /**
     * The globalIndexer is demanded to dump the merged version of the index
     */
    private InMemoryIndexing globalIndexer;
    /**
     * The block_size is number of bytes allowed to be used before creating a new block
     * Notice that, even in case of going ahead this threshold, the current document is however processed
     */
    private long block_size = 80;

    /**
     * The next number of block to write. Since the index of the blocks starts at 0, this is
     * by fact the number of blocks written at the moment
     */
    private int next_block = 0;

    /**
     * Set to true when the DocumentStream no more generates documents
     */
    private boolean finish = false;

    /**
     * Set to the directory in which we want to save all files
     */
    private Path path;

    /**
     * The indexer demand to invert the current block. Compression is not
     * predicted at this stage
     */
    private final InMemoryIndexing blockIndexer;

    /**
     * Describes the mode in which we are operating
     * DEBUG: Both globalIndexer and blockIndexer operates in ASCII mode
     * BINARY: Both globalIndexer and blockIndexer write binary objects
     * COMPRESSED: blockIndexer writes binary objects, the globalIndexer writes a compressed
     *              representation of doc_ids and term_frequencies lists
     */
    private final CompressionType compression;



    public SPIMIIndex(CompressionType compression, DocumentStream s, InMemoryIndexing i) {
        // Parameter Allocation
        // -------------------
        this.compression = (compression == CompressionType.DEBUG) ? compression : CompressionType.BINARY;
        stream = s;
        globalIndexer = i;
        // -------------------

        // Creation of the block indexer
        // -------------------
        DocumentIndex di = new DocumentIndexImpl();
        Vocabulary v = new VocabularyImpl();
        Dumper d = Dumper.getInstance(this.compression);
        blockIndexer = new InMemoryIndexing(v, d, di);
        // -------------------
    }

    /**
     * Sets the estimation of bytes allowed for the current block
     * @param limit the limit of bytes allowed for the block
     */
    public void setLimit(int limit) {
        block_size = limit;
    }

    /**
     * Checked for control purposes
     * @return whether the stream has still some documents
     */
    boolean finished() {
        return finish;
    }

    /**
     * Returns the current number of blocks
     * @return the next block number to generate
     */
    int getNext_block(){return next_block;}

    /**
     * Memory check for the current block
     * @param usedMemory the used memory for the block
     * @return whether we have enough memory to proceed with the current block
     */
    boolean availableMemory(long usedMemory) {
        // Returns if usedMemory is less than a threshold
        double threshold = ((double) block_size / 100) * Runtime.getRuntime().maxMemory();
        // threshold = Math.min(threshold, 1e9);

        return usedMemory <= threshold;
    }

    /**
     * Builds the inverted index
     * @param path the directory in which we want to store all needed file
     */
    public void buildIndexSPIMI(Path path) {
        this.path = path;

        Path blocksPath = path.resolve("blocks/");

        // Preliminary flush of files
        IOUtils.deleteDirectory(blocksPath);

        IOUtils.createDirectory(blocksPath);

        // 1) create and invert a block. The block is then dumped in secondary memory
        // ---------------------
        while (!finished()) {
            /*
            if (next_block == 2)
                break;
            */
            invertBlock(blocksPath.resolve("" + next_block));
        }
        // ---------------------

        // 2) Initialize one reader for each block
        // ---------------------
        List<Fetcher> readVocBuffers = new ArrayList<>();
        for (int i = 0; i < next_block; i++) {
            Fetcher f = Fetcher.getFetcher(compression);
            f.start(blocksPath.resolve("" + i));

            readVocBuffers.add(f);
        }
        // ---------------------

        globalIndexer.setup(path);

        // 3) Merge all document indexes
        // ---------------------
        int N = concatenateDocIndexes(readVocBuffers);
        // ---------------------

        // 4) Merge all vocabularies (with relative posting lists)
        // ---------------------
        mergeAllBlocks(readVocBuffers, N);
        // ---------------------

        for(int i = 0; i < next_block; i++)
            readVocBuffers.get(i).end();
        globalIndexer.close();

        Constants.N=N;
    }


    /**
     * Creates, Inverts and Dumps a block
     * @param blockPath the path prefix for the current block
     */
    public void invertBlock(Path blockPath) {
        // Get memory state
        Runtime.getRuntime().gc();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMemory = startMemory;

        // Set up the indexer for this block
        blockIndexer.setup(blockPath);

        // 1) Process documents until memory limit reached
        int docProcessed = 0;
        // ---------------------
        while (availableMemory(usedMemory)) {
            // Write the block if we reached a certain (high) number of entries
            if (docProcessed++ == Constants.MAX_ENTRIES_PER_SPIMI_BLOCK) {
                System.out.println("Max number of entries per block reached (" + Constants.MAX_ENTRIES_PER_SPIMI_BLOCK + ")");
                break;
            }

            // Get the next document
            Optional<Document> doc = Optional.ofNullable(stream.nextDoc());
            if (doc.isEmpty()) {
                // When I have finished, I set the flag
                finish = true;
                break;
            }
            blockIndexer.processDocument(doc.get());
            usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            // System.out.println("USED MEMORY : " + (usedMemory - startMemory));
        }
        // ---------------------
        System.out.println("BLOCK CREATED");
        // ---------------------
        // 2.0) set the partial term upper bound
        blockIndexer.computePartialTermUB();

        // 2.1) Dump the block
        // ---------------------
        blockIndexer.dumpVocabulary();
        blockIndexer.dumpDocumentIndex();
        // ---------------------

        // Reset the block indexer
        blockIndexer.close();
        next_block++;
    }

    private Integer concatenateDocIndexes(List<Fetcher> readers) {
        // 1) We accumulate length and total number of documents from blocks
        //      This information is the first thing to dump
        // ------------------------------------
        int N = 0;
        int L = 0;
        for (int i = 0; i < next_block; i++) {
            int[] info = readers.get(i).getInformations();
            N += info[0];
            L += info[1];
        }
        globalIndexer.getDocIndex().setNumDocuments(N);
        globalIndexer.getDocIndex().setTotalLength(L);
        globalIndexer.dumpDocumentIndex();
        // ------------------------------------

        // 2) We fetch one entry at a time from block by block and we dump it on the final file
        // ------------------------------------
        for (int i = 0; i < next_block; i++) {
            Map.Entry<Integer, DocumentIndexEntry> entry;
            while ( (entry = readers.get(i).loadDocEntry()) != null )
                globalIndexer.dumpDocumentIndexLine(entry);
        }
        // ------------------------------------
        return N;
    }

    /**
     * Read
     * @param readVocBuffers the list of blocks readers
     */
    void mergeAllBlocks(List<Fetcher> readVocBuffers, Integer numDocuments) {
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
                VocabularyEntry entry = mergeEntries(toMerge, numDocuments);
                globalIndexer.dumpVocabularyLine(new AbstractMap.SimpleEntry<>(lowestTerm, entry));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }


    /**
     * Merge the entries related to the same term in different blocks
     * @param toMerge the list of vocabulary entries to merge
     * @return a merged entry
     */
    VocabularyEntry mergeEntries(List<VocabularyEntry> toMerge, int numDocuments){
        PostingListImpl mergedList = new PostingListImpl();
        Integer frequency = 0;
        Double upperBound = 0.0;

        for (VocabularyEntry vocabularyEntry : toMerge) {

            // Merge the (decompressed) posting lists of the entries
            int L = mergedList.mergePosting(vocabularyEntry.getPostingList());

            // Update term frequency and upper bound
            frequency += vocabularyEntry.getDocumentFrequency();
            if (vocabularyEntry.getUpperBound() > upperBound)
                upperBound = vocabularyEntry.getUpperBound();
        }

        // final term upper bound computation
        Double to_multiply = Math.log10( ((double) numDocuments/ mergedList.getDocIdsDecompressedList().size()));
        upperBound *= to_multiply;
        VocabularyEntry result = new VocabularyEntry(frequency, upperBound, mergedList);

        return result;
    }
}
