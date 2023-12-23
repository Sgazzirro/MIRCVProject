package it.unipi.index;

import it.unipi.encoding.CompressionType;
import it.unipi.io.DocumentStream;
import it.unipi.io.Fetcher;
import it.unipi.model.*;
import it.unipi.model.implementation.PostingListImpl;
import it.unipi.scoring.Scorer;
import it.unipi.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class SPIMIIndex {

    private static final Logger logger = LoggerFactory.getLogger(SPIMIIndex.class);

    /**
     * The stream that generates one Document at a time
     */
    private final DocumentStream stream;
    /**
     * The globalIndexer is demanded to dump the merged version of the index
     */
    private final InMemoryIndexing globalIndexer;
    /**
     * The limit is the percentage of memory allowed to be used before dumping the current block and creating a new one
     * Notice that, even in case of going ahead this threshold, the current document is however processed
     */
    private int limit = 80;

    /**
     * The number of the block being currently written. Since the index of the blocks starts at 0, this is
     * by fact the number of blocks written at the moment
     */
    private int blocksProcessed = 0;

    /**
     * Set to true when the DocumentStream no more generates documents
     */
    private boolean finished = false;

    /**
     * Describes the compression mode of the blocks
     */
    private final CompressionType blockCompression;


    public SPIMIIndex(CompressionType compression, DocumentStream stream) {
        Constants.setCompression(compression);
        this.stream = stream;
        this.globalIndexer = new InMemoryIndexing(compression);

        // Compression of blocks (we never write compressed blocks for merging ease)
        this.blockCompression = (compression == CompressionType.DEBUG) ? compression : CompressionType.BINARY;
    }

    /**
     * Sets the percentage of memory allowed for the current block
     * @param limit the percentage of used memory allowed for the block
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Checked for control purposes
     * @return whether the stream has still some documents
     */
    boolean finished() {
        return finished;
    }

    /**
     * Memory check for the current block
     * @param usedMemory the used memory for the block
     * @return whether we have enough memory to proceed with the current block
     */
    boolean availableMemory(long usedMemory) {
        // Returns if usedMemory is less than a threshold
        double threshold = ((double) limit / 100) * Runtime.getRuntime().maxMemory();

        return usedMemory <= threshold;
    }

    /**
     * Builds the inverted index
     * @param path the directory in which we want to store all needed file
     */
    public void buildIndex(Path path) {
        try {
            // Preliminary flush of files
            IOUtils.deleteDirectory(path);
            IOUtils.createDirectory(path);

            Path blocksPath = path.resolve("blocks/");

            // 1) create and invert a block. The block is then dumped in secondary memory
            while (!finished())
                invertBlock(blocksPath.resolve("" + blocksProcessed));

            // 2) Initialize one reader for each block
            List<Fetcher> blockFetchers = new ArrayList<>();
            for (int i = 0; i < blocksProcessed; i++) {
                Fetcher blockFetcher = Fetcher.getFetcher(blockCompression);
                blockFetcher.start(blocksPath.resolve("" + i));

                blockFetchers.add(blockFetcher);
            }

            logger.info("Starting merge of all blocks");
            globalIndexer.setup(path);

            // 3) Merge all document indexes
            Constants.N = mergeDocumentIndexes(blockFetchers);
            logger.info("Document index correctly merged");

            // 4) Merge all vocabularies (with relative posting lists)
            mergeVocabularies(blockFetchers);
            logger.info("Vocabulary correctly merged");

            // 5) Close all fetchers
            for (int i = 0; i < blocksProcessed; i++)
                try {
                    blockFetchers.get(i).close();
                } catch (Exception e) {
                    logger.warn("Something went wrong closing block indexers", e);
                }

            // Delete blocks temp directory
            IOUtils.deleteDirectory(blocksPath);
            globalIndexer.close();

        } catch (IOException ioException) {
            logger.error("Fatal error when building the index", ioException);
            System.exit(1);
        }
    }

    /**
     * Creates, Inverts and Dumps a block
     * @param blockPath the path prefix for the current block
     */
    public void invertBlock(Path blockPath) {
        // Get memory state
        Runtime.getRuntime().gc();
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        try (
                InMemoryIndexing blockIndexer = new InMemoryIndexing(blockCompression)
        ) {
            // Set up the indexer for this block
            if (!blockIndexer.setup(blockPath))
                throw new IOException("Could not open directory for dumping the blocks");

            // 1) Process documents until memory limit reached
            int docProcessed = 0;

            while (availableMemory(usedMemory)) {
                // Write the block if we reached a certain (high) number of entries
                if (docProcessed++ == Constants.MAX_ENTRIES_PER_SPIMI_BLOCK) {
                    logger.debug("Max number of entries per block reached (" + Constants.MAX_ENTRIES_PER_SPIMI_BLOCK + ")");
                    break;
                }

                // Get the next document
                Optional<Document> doc = Optional.ofNullable(stream.nextDoc());
                if (doc.isEmpty()) {
                    // When I have finished, I set the flag
                    finished = true;
                    break;
                }
                blockIndexer.processDocument(doc.get());
                usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                // System.out.println("USED MEMORY : " + (usedMemory - startMemory));
            }

            logger.debug("Block " + blocksProcessed + " correctly created, waiting for dump...");
            // ---------------------

            // 2.1) Dump the block
            blockIndexer.dumpDocumentIndex();
            blockIndexer.dumpVocabulary();
            logger.info("Block " + blocksProcessed + " dumped");

        } catch (IOException ioException) {
            logger.error("Fatal error happened while inverting block");
            logger.error("Block " + blocksProcessed, ioException);
        }

        blocksProcessed++;
    }

    private int mergeDocumentIndexes(List<Fetcher> readers) throws IOException {
        // 1) We accumulate length and total number of documents from blocks
        //    This information is the first thing to dump
        int N = 0;
        int L = 0;
        for (int i = 0; i < blocksProcessed; i++) {
            int[] documentIndexStats = readers.get(i).getDocumentIndexStats();

            N += documentIndexStats[0];
            L += documentIndexStats[1];
        }

        globalIndexer.getDocumentIndex().setNumDocuments(N);
        globalIndexer.getDocumentIndex().setTotalLength(L);
        globalIndexer.dumpDocumentIndex();
        // ------------------------------------

        // 2) We fetch one entry at a time from each block, and we dump it on the final file
        for (int i = 0; i < blocksProcessed; i++) {
            Map.Entry<Integer, DocumentIndexEntry> entry;

            while ( (entry = readers.get(i).loadDocEntry()) != null )
                globalIndexer.dumpDocumentIndexLine(entry);
        }
        // ------------------------------------
        globalIndexer.flushDocumentIndex();     // Flush residual entries in buffer

        return N;
    }

    /**
     * Read
     * @param readVocBuffers the list of blocks readers
     */
    private void mergeVocabularies(List<Fetcher> readVocBuffers) throws IOException {
        // Key idea to merge all blocks together
        // To do the merging, we open all block files simultaneously
        // We maintain small read buffers for blocks we are reading
        // and a write buffer for the final merged index we are writing.
        // In each iteration, we select the lowest termID that has not been processed yet
        // using a priority queue or a similar data structure.
        // All posting lists for this termID are read and merged, and the merged list is written back to disk.
        // Each read buffer is refilled from its file when necessary.

        List<Boolean> processed = new ArrayList<>();
        for (int i = 0; i < blocksProcessed; i++)
            processed.add(true);

        int blocksClosed = 0;
        boolean[] closed = new boolean[blocksProcessed];

        String[] terms = new String[blocksProcessed];
        VocabularyEntry[] entries = new VocabularyEntry[blocksProcessed];

        // For debug purposes
        char currentLetter = '\0';
        int  termsProcessed = 0;
        logger.info("Starting vocabulary merge");

        String lowestTerm;
        while (true) {
            for (int k = 0; k < blocksProcessed; k++) {
                if (closed[k])
                    continue;

                if (processed.get(k)) {
                    // Get a vocabulary entry
                    // Term | TermFrequency | UpperBound | #Posting | Offset
                    Map.Entry<String, VocabularyEntry> entry = readVocBuffers.get(k).loadVocEntry();
                    if (entry == null) {
                        blocksClosed++;

                        logger.debug("Merge for block " + k + " completed");
                        logger.debug("Total number of closed blocks: " + blocksClosed);

                        closed[k] = true;
                        continue;
                    }

                    terms[k] = entry.getKey();
                    entries[k] = entry.getValue();
                }
            }
            if (blocksClosed == blocksProcessed)
                break;

            lowestTerm = null;
            // Get the lowest lexicographically term
            for (int k = 0; k < blocksProcessed; k++) {
                if (lowestTerm == null) {
                    if (closed[k])
                        continue;
                    else
                        lowestTerm = terms[k];
                }
                if (!closed[k] && lowestTerm.compareTo(terms[k]) > 0)
                    lowestTerm = terms[k];
            }

            // Merge entries with equal terms
            // Mark as processed correspondent blocks
            List<VocabularyEntry> toMerge = new ArrayList<>();
            for (int k = 0; k < blocksProcessed; k++) {
                if (terms[k].equals(lowestTerm) && !closed[k]) {
                    toMerge.add(entries[k]);
                    processed.set(k, true);
                } else
                    processed.set(k, false);
            }

            // Write the merge onto the output file
            VocabularyEntry entry = mergeEntries(toMerge);
            globalIndexer.dumpVocabularyLine(new AbstractMap.SimpleEntry<>(lowestTerm, entry));

            termsProcessed++;

            if (lowestTerm == null || lowestTerm.isEmpty())
                continue;

            char firstLetter = lowestTerm.charAt(0);
            if (firstLetter != currentLetter && Character.isLetter(firstLetter)) {
                currentLetter = firstLetter;
                logger.debug((termsProcessed - 1) + " terms processed, dumping letter  -->  " + currentLetter);
            }
        }

    }


    /**
     * Merge the entries related to the same term in different blocks
     * @param toMerge the list of vocabulary entries to merge
     * @return a merged entry
     */
    private VocabularyEntry mergeEntries(List<VocabularyEntry> toMerge) {
        VocabularyEntry mergedEntry = new VocabularyEntry();
        PostingListImpl mergedList = new PostingListImpl(mergedEntry);

        int frequency = 0;

        for (VocabularyEntry vocabularyEntry : toMerge) {
            // Merge the (decompressed) posting lists of the entries
            mergedList.mergePosting(vocabularyEntry.getPostingList());

            // Update term frequency
            frequency += vocabularyEntry.getDocumentFrequency();
        }

        mergedEntry.setPostingList(mergedList);
        mergedEntry.setDocumentFrequency(frequency);

        // final term upper bound computation
        Scorer scorer = Scorer.getScorer(globalIndexer.getDocumentIndex());
        mergedEntry.computeUpperBound(scorer);

        return mergedEntry;
    }
}
