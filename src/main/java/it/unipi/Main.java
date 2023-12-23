package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.DocumentStream;
import it.unipi.utils.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main( String[] args ) throws IOException {

        Path indexPath = Paths.get("./data/compr_debug");
        Constants.setPath(indexPath);

        // TEST
        Constants.BLOCK_SIZE = 200;
        DocumentStream stream = new DocumentStream(Constants.COLLECTION_FILE, 0, 100000);

        SPIMIIndex spimi = new SPIMIIndex(CompressionType.COMPRESSED, stream);
        spimi.buildIndex(indexPath);
    }
}
