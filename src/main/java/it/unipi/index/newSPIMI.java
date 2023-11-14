package it.unipi.index;

import it.unipi.model.DocumentStream;
import it.unipi.model.implementation.Document;
import it.unipi.utils.Fetcher;

import javax.print.Doc;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class newSPIMI {
    // FIXME: UnderLock [Angelo]
    private DocumentStream stream;
    private InMemoryIndexing indexer;
    private long block_size = 10000;
    private int next_block = 0;
    private boolean finish = false;

    public newSPIMI(DocumentStream s,InMemoryIndexing i){
        stream = s;
        indexer = i;
    }

    public void setLimit(int size){
        block_size = size;
    }

    private boolean finished() {
        return finish;
    }

    private boolean availableMemory(long usedMemory, long startMemory) {
        // Returns if (usedMemory - starting memory) is less than a treshold
        return (usedMemory - startMemory) <= block_size;
    }

    public void buildIndexSPIMI(){
        // Preliminary flush of files
        for(File file: Objects.requireNonNull(new File("./data/blocks").listFiles()))
            if (!file.isDirectory())
                file.delete();

        // < Until documents are not finished >
        // < Create and Invert the block, write it to a file >
        // < Merge all blocks >
        while(!finished()){
            invertBlock("data/blocks/_" + next_block);
        }

        // TODO: Implement
        mergeAllBlocks();
    }

    public void invertBlock(String filename){
        // Get memory state
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMemory = startMemory;

        // Setup dumper
        indexer.setup(filename);

        while(availableMemory(usedMemory, startMemory)){
            // Get the next document
            Optional<Document> doc = Optional.ofNullable(stream.nextDoc());
            if(doc.isEmpty()){
                // When I have finished, I set the flag
                finish = true;
                break;
            }
            indexer.processDocument(doc.get());
            usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }

        // Dump when out of memory
        indexer.dumpVocabulary();

        // Reset dumper
        indexer.close();
        next_block++;
    }

    private void mergeAllBlocks() {
        // TODO
    }

}
