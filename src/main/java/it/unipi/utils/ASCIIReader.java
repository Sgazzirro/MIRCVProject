package it.unipi.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ASCIIReader implements ReadingInterface{
    BufferedReader stream;
    boolean opened = false;

    @Override
    public void open(String filename) throws IOException {
        stream = new BufferedReader(new FileReader(filename, StandardCharsets.UTF_8));
        opened = true;
    }

    @Override
    public void close() throws IOException {
        stream.close();
        opened = false;
    }

    @Override
    public String readLine() throws IOException {
        return stream.readLine();
    }

    @Override
    public byte[] read() {
        System.err.println("This reader is not capable to decode binary data");
        return null;
    }

    @Override
    public void skip(long bytes) throws IOException {
        stream.skip(bytes);
    }
}
