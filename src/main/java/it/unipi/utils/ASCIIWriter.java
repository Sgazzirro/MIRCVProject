package it.unipi.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ASCIIWriter implements WritingInterface {
    String filename;
    BufferedWriter stream;
    boolean opened = false;

    long written;


    public ASCIIWriter(){}
    public ASCIIWriter(String filename) throws IOException {
        this();
        open(filename);
    }
    public void open(String f) throws IOException {
        stream = new BufferedWriter(new FileWriter(f, StandardCharsets.UTF_8));
        filename = f;
        opened = true;
        written = 0;
    }

    @Override
    public void write(String buffer) throws IOException {
        System.out.println("Trying to write " + buffer + "at filename" + filename);
        stream.write(buffer);
        written += buffer.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public void writeBinary(byte[] bytes) throws IOException {
        System.err.println("This writer is not able to write in binary format");
    }

    @Override
    public void close() throws IOException {
        stream.close();
        opened = false;
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    @Override
    public long getFilePointer() {
        return written;
    }


}
