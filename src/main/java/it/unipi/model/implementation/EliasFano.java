package it.unipi.model.implementation;

import it.unipi.model.EncoderInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EliasFano implements EncoderInterface {
    @Override
    public ArrayList<Boolean> encode(ArrayList<Integer> list) {
        int U = list.get(list.size()-1);
        int n = list.size();
        ArrayList<Boolean> highBitset = new ArrayList<>();
        ArrayList<Boolean> lowBitset = new ArrayList<>();
        int lowHalfLength = (int) Math.ceil(Math.log((float)U/n)/Math.log(2));
        int highHalfLength = (int) Math.ceil(Math.log(U)/Math.log(2))-lowHalfLength;
        int totBits = lowHalfLength+highHalfLength;

        Map<Integer,Integer> clusterFreq = new HashMap<>();

        for(int number: list){
            // low bits
            lowBitset.addAll(extractBits(number, lowHalfLength));

            // high bits
            int highHalfInt = (number >> (totBits - highHalfLength) & (1 << highHalfLength)-1); // computes the integer corresponding to the higher bits
            clusterFreq.compute(highHalfInt, (key, value) -> (value == null) ? 1 : value + 1);  // keeps track of the number of integers in the clusters
        }
        for(Map.Entry<Integer,Integer> entry: clusterFreq.entrySet()){
            int value = entry.getValue();
            highBitset.addAll(integerToUnary(value));
        }
        // append low bitset to highbitset
        highBitset.addAll(lowBitset);
        return highBitset;
    }

    @Override
    public ArrayList<Integer> decode(ArrayList<Boolean> bitsList) {
        // TO DO
        return null;
    }

    private ArrayList<Boolean> extractBits(int num, int n) {
        ArrayList<Boolean> bitList = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            // Use bitwise AND operation to extract the lowest bit
            boolean bit = ((num >> i) & 1) == 1;
            bitList.add(bit);
        }

        // The bits are added to the list from least significant to most significant, so reverse the list.
        Collections.reverse(bitList);

        return bitList;
    }

    private ArrayList<Boolean> integerToUnary(int num) {
        ArrayList<Boolean> unaryList = new ArrayList<>();

        // Add 'true' to the list 'num' times
        for (int i = 0; i < num; i++) {
            unaryList.add(true);
        }
        unaryList.add(false);

        return unaryList;
    }
}
