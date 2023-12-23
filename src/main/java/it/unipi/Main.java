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

        Path indexPath = Paths.get("./data/DEBUG");
        Constants.setPath(indexPath);

        DocumentStream stream = new DocumentStream();

        SPIMIIndex spimi = new SPIMIIndex(CompressionType.DEBUG, stream);
        spimi.buildIndex(indexPath);
    }
}
