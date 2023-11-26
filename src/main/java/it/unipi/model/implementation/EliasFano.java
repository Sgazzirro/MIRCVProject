package it.unipi.model.implementation;

import it.unipi.model.Encoder;
import it.unipi.utils.ByteUtils;
import it.unipi.utils.Constants;

import java.io.*;
import java.util.*;

public class EliasFano implements Encoder {

    public byte[] encode(List<Integer> array) {
        // data needed to compress
        int originalU = array.get(array.size() - 1);
        int U = originalU > 0 ? originalU : 1;
        // It could happen that U is 0 (if we only have one docId=0), in this case set U=1
        // TODO - Check that in this way we don't break anything
        int n = array.size();

        // number of bits
        int lowHalfLength = (int) Math.ceil(Math.log((float) U / n) / Math.log(2));
        int highHalfLength = (int) Math.ceil(Math.log(U) / Math.log(2)) - lowHalfLength;
        int totBits = lowHalfLength+highHalfLength;

        // structures to maintain encoded numbers
        BitSet highBitset = new BitSet((int) (n+Math.pow(2,highHalfLength)));
        BitSet lowBitset = new BitSet(lowHalfLength*n);

        // utility
        int index = 0;
        Map<Integer, Integer> clusterComp = new HashMap<>();

        for(int number: array){
            // parte bassa
            BitSet extractedLowBitset = extractLowBitset(number, lowHalfLength);
            append(lowBitset, extractedLowBitset, lowHalfLength, index);
            index += lowHalfLength;

            // cluster composition of high part
            int highHalfInt = (number >> (totBits - highHalfLength) & (1 << highHalfLength)-1); // computes the integer corresponding to the higher bits
            clusterComp.compute(highHalfInt, (key, value) -> (value == null) ? 1 : value + 1);  // keeps track of the number of integers in the clusters
        }

        // high part
        index = 0;
        for (int i=0; i<Math.pow(2,highHalfLength); i++){
            int value = clusterComp.get(i) == null ? 0 : clusterComp.get(i);
            append(highBitset, integerToUnary(value), integerToUnary(value).length()+1, index);
            index+=integerToUnary(value).length()+1;
        }

        // Convert everything as byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            DataOutputStream dos = new DataOutputStream(outputStream);
            dos.writeInt(originalU);
            dos.writeInt(n);
            outputStream.write(highBitset.toByteArray());
            outputStream.write(lowBitset.toByteArray());
        } catch (IOException e) {
            // This exception can never be thrown
            System.err.println("Unknown error: Elias Fano");
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    public List<Integer> decode(byte[] byteList) {
        List<Integer> decoded = new ArrayList<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(byteList);
        DataInputStream dis = new DataInputStream(bais);
        try {
            int originalU = dis.readInt();
            int U = originalU > 0 ? originalU : 1;
            int n = dis.readInt();

            // number of bits
            int lowHalfLength = (int) Math.ceil(Math.log((float) U / n) / Math.log(2));
            int highHalfLength = (int) Math.ceil(Math.log(U) / Math.log(2)) - lowHalfLength;
            int nTotHighBits = (int) (n + Math.pow(2, highHalfLength));
            int nTotLowBits = lowHalfLength*n;

            byte[] lowBytes = new byte[(int) Math.ceil((float) nTotLowBits/8)];
            byte[] highBytes = new byte[(int) Math.ceil((float) nTotHighBits/8)];

            dis.read(highBytes);
            dis.read(lowBytes);

            BitSet lowBitset = BitSet.valueOf(lowBytes);
            BitSet highBitset = BitSet.valueOf(highBytes);

            int groupValue = 0;
            int lowBitsetIndex =0;
            for(int i=0; i<nTotHighBits; i++){
                if(highBitset.get(i)){
                    int shifted = groupValue;
                    for(int j=lowBitsetIndex*lowHalfLength; j<lowBitsetIndex*lowHalfLength+lowHalfLength; j++){
                        int bitValue = lowBitset.get(j)? 1:0;
                        shifted = shifted << 1 | bitValue;
                    }
                    lowBitsetIndex++;
                    decoded.add(shifted);
                } else groupValue++;
            }

            return decoded;
        } catch (IOException ie){
            ie.printStackTrace();
            return null;
        }
    }

    private static BitSet extractLowBitset(int number, int nLowBits) {
        // Create a BitSet with nLowBits bits
        BitSet bitSet = new BitSet(0);
        int j = nLowBits-1;
        // Iterate over the lowest nLowBits bits of the number
        for (int i = 0; i < nLowBits; i++) {
            boolean bitValue = ((number >> i) & 1) == 1;
            bitSet.set(j--, bitValue);
        }

        return bitSet;
    }

    private static BitSet integerToUnary(int num) {
        BitSet unaryBitset = new BitSet();

        // Add 'true' to the list 'num' times
        for (int i = 0; i < num; i++)
            unaryBitset.set(unaryBitset.length(), true);

        unaryBitset.set(unaryBitset.length(), false);

        return unaryBitset;
    }

    private static void append(BitSet bitset1, BitSet bitset2, int nbits, int index) {
        // appends bitset2 to bitset1, for nbits, starting from index.
        for (int i=0; i<nbits; i++)
            bitset1.set(index++, bitset2.get(i));
    }

    public static void main(String[] args) {
        EliasFano test = new EliasFano();
        byte[] bytes12 = test.encode(List.of(1, 2));
        byte[] bytes01 = test.encode(List.of(0, 1));
        System.out.println(ByteUtils.byteArrayToBinaryString(bytes01));
        System.out.println(ByteUtils.byteArrayToBinaryString(bytes12));
    }
}
