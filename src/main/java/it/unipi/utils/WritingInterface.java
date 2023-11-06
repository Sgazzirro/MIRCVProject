package it.unipi.utils;

import java.io.IOException;

public interface WritingInterface{

    public void open(String filename) throws IOException;

    public void write(String str) throws IOException;

    public void writeBinary(byte[] bytes) throws IOException;
    public void close() throws IOException;

    public boolean isOpen();

    public long getFilePointer();
}
