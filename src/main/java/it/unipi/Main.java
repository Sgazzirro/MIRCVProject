package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.DocumentStream;
import it.unipi.model.DocumentIndex;
import it.unipi.utils.*;

import javax.print.Doc;
import java.io.*;
import java.nio.file.Path;

public class Main {

    public static void main( String[] args ) throws IOException {
        Path indexPath = Constants.DATA_PATH;
        File collectionFile = Constants.COLLECTION_FILE;

        DocumentStream stream = new DocumentStream(collectionFile);

        SPIMIIndex spimi = new SPIMIIndex(CompressionType.DEBUG, stream);
        spimi.buildIndex(indexPath);
    }
}
