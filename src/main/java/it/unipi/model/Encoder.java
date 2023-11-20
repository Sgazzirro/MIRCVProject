package it.unipi.model;

import java.util.List;

public interface Encoder {

    byte[] encode(List<Integer> intList);

    List<Integer> decode(byte[] byteArray);
}
