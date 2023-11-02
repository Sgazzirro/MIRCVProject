package it.unipi.model;

import java.util.ArrayList;

public interface EncoderInterface {
    public ArrayList<Boolean> encode(ArrayList<Integer> list);

    public ArrayList<Integer> decode(ArrayList<Boolean> bitsList);
}
