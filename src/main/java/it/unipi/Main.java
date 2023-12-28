package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.DocumentStream;
import it.unipi.utils.*;

import java.io.*;
import java.nio.file.Path;

public class Main {

    public static void main( String[] args ) throws IOException {
        Path indexPath = Path.of("COMPRESSEDELIAS100010K");
        File collectionFile = Constants.COLLECTION_FILE;

        DocumentStream stream = new DocumentStream(collectionFile);

        SPIMIIndex spimi = new SPIMIIndex(CompressionType.COMPRESSED, stream);
        spimi.buildIndex(indexPath);
    }
}
