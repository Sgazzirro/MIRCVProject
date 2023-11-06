package it.unipi.utils;

import java.io.IOException;

public interface ReadingInterface {

    public void open(String filename) throws IOException;

    public void close() throws IOException;
    public String readLine() throws IOException;
    public byte[] read();

    public void skip(long bytes) throws IOException;


}
