package it.unipi.model.implementation;

import it.unipi.model.EncoderInterface;

import java.io.*;
import java.util.*;

public class EliasFano {

    public long encode(String filename, ArrayList<Integer> list) {
        // data needed for compression
        int U = list.get(list.size()-1);
        int n = list.size();

        // number of bits
        int lowHalfLength = (int) Math.ceil(Math.log((float)U/n)/Math.log(2));          //number of bits needed to represent each group in low_bits
        int highHalfLength = (int) Math.ceil(Math.log(U)/Math.log(2))-lowHalfLength;    //number of bits needed to represent each group in high_bits
        int totBits = lowHalfLength+highHalfLength;                                     // tot number of bits

        // structures to maintain encoded integers
        BitSet highBitset = new BitSet((int) (n+Math.pow(2,highHalfLength)));
        BitSet lowBitset = new BitSet(lowHalfLength*n);

        // utility
        int index =0;
        Map<Integer,Integer> clusterComp = new HashMap<>();

        for(int number: list){
            // low bits compression
            BitSet extractedLowBitset = extractLowBitset(number,lowHalfLength);
            append(lowBitset, extractedLowBitset, lowHalfLength, index);
            index += lowHalfLength;

            // high bits cluster composition
            int highHalfInt = (number >> (totBits - highHalfLength) & (1 << highHalfLength)-1); // computes the integer corresponding to the higher bits
            clusterComp.compute(highHalfInt, (key, value) -> (value == null) ? 1 : value + 1);  // keeps track of the number of integers in the clusters
        }
        // high bits compression
        index = 0;
        for(int i=0; i<Math.pow(2,highHalfLength); i++){
            int value = clusterComp.get(i) == null? 0:clusterComp.get(i);
            append(highBitset, integerToUnary(value), integerToUnary(value).length()+1, index);
            index+=integerToUnary(value).length()+1;
        }
        // dump on file
        return dump(highBitset.toByteArray(), lowBitset.toByteArray(), filename);
    }

    public ArrayList<Integer> decode(String filename, long byteOffset, int U, int n) {
        // STRUCTURE: low_bits | high_bits //

        ArrayList<Integer> decoded = null;
        try {
            // Open the file for reading
            FileInputStream fis = new FileInputStream(filename);
            DataInputStream dis = new DataInputStream(fis);

            // skip lists not required
            if (fis.skip(byteOffset)!=byteOffset){
                throw new IOException();
            }

            // number of bits
            int lowHalfLength = (int) Math.ceil(Math.log((float)U/n)/Math.log(2));          // number of bits for each group in low_bits
            int highHalfLength = (int) Math.ceil(Math.log(U)/Math.log(2))-lowHalfLength;    // number of bits for each group in high_bits
            int nTotLowBits = lowHalfLength*n;                                              // total length (in bits) of low_bits
            int nTotHighBits = (int) (n+Math.pow(2,highHalfLength));                        // total length (in bits) of high_bits

            // read low_bits and high_bits and cast them to BitSets
            byte [] lowBytes = new byte[(int) Math.ceil((float) nTotLowBits/8)];
            byte [] highBytes = new byte[(int) Math.ceil((float) nTotHighBits/8)];
            fis.read(lowBytes);
            fis.read(highBytes);
            BitSet lowBitset = BitSet.valueOf(lowBytes);
            BitSet highBitset = BitSet.valueOf(highBytes);

            // Close the streams
            dis.close();
            fis.close();

            // list containing uncompressed integers
            decoded=new ArrayList<>();

            // utility
            int groupValue = 0;
            int lowBitsetIndex =0;

            // DECODE //
            for(int i=0; i<nTotHighBits; i++){
                if(highBitset.get(i)){
                    int shifted = groupValue;
                    for(int j=lowBitsetIndex*lowHalfLength; j<lowBitsetIndex*lowHalfLength+lowHalfLength; j++){
                        int bitValue = lowBitset.get(j)? 1:0;
                        shifted = shifted<<1|bitValue;
                    }
                    lowBitsetIndex++;
                    decoded.add(shifted);
                } else groupValue++;
            }
        } catch (IOException e) {
            System.err.println("Error in decoding EliasFano");
            e.printStackTrace();
        }
        return decoded;
    }

    private BitSet extractLowBitset(int number, int nLowBits) {
        // Extracts the lower part of the binary representation of the number

        BitSet bitSet = new BitSet();
        int j = nLowBits-1;
        // Iterate over the lowest nLowBits bits of the number
        for (int i = 0; i < nLowBits; i++) {
            boolean bitValue = ((number >> i) & 1) == 1;
            bitSet.set(j--, bitValue);
        }
        return bitSet;
    }

    private BitSet integerToUnary(int num) {
        // converts the number to a unary representation bitset

        BitSet unaryBitset = new BitSet();

        // Add 'true' to the list 'num' times
        for (int i = 0; i < num; i++) {
            unaryBitset.set(unaryBitset.length(), true);
        }
        unaryBitset.set(unaryBitset.length(), false);

        return unaryBitset;
    }

    private BitSet append(BitSet bitset1, BitSet bitset2, int nbits, int index){
        // appends bitset2 to bitset1, for nbits, starting from index.

        for(int i=0; i<nbits; i++){
            bitset1.set(index++, bitset2.get(i));
        }
        return bitset1;
    }

    private long dump(byte[] highBitset, byte[] lowBitset, String filename){
        // dumps the encoded list on file

        // STRUCTURE: low_bits | high_bits //
        try (FileOutputStream fos = new FileOutputStream(filename,true)) {
            long byteOffset = fos.getChannel().position();
            fos.write(lowBitset);
            fos.write(highBitset);
            return byteOffset;
        } catch (IOException e) {
            System.err.println("Error in dumping EliasFano");
            e.printStackTrace();
            return -1;
        }
    }
    /*
    private void skipLists(FileInputStream fis, DataInputStream dis, int index) throws IOException {
        // skips unnecessary lists

        for(int i=0; i<index; i++){
            // Read integers from the file
            int U = dis.readInt();
            int n = dis.readInt();

            // computing number of bytes to skip
            int lowHalfLength = (int) Math.ceil(Math.log((float)U/n)/Math.log(2));
            int highHalfLength = (int) Math.ceil(Math.log(U)/Math.log(2))-lowHalfLength;
            int nTotLowBits = lowHalfLength*n;
            int nTotHighBits = (int) (n+Math.pow(2,highHalfLength));
            int bytesToSkip = (int) Math.ceil((float) nTotLowBits/8) + (int) Math.ceil((float) nTotHighBits/8);

            if (bytesToSkip != fis.skip(bytesToSkip)){
                throw new IOException();
            }
        }
    }
     */
}
