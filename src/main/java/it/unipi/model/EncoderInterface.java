package it.unipi.model;

import java.util.ArrayList;

public interface EncoderInterface {
    public void encode(String filename, ArrayList<Integer> list);

    public ArrayList<Integer> decode(String filename, int index);
}
