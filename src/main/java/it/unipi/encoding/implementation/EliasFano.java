package it.unipi.encoding.implementation;

import it.unipi.encoding.Encoder;

import java.nio.ByteBuffer;
import java.util.*;

public class EliasFano extends Encoder {

    @Override
    public byte[] encode(List<Integer> list) {
        // data needed to compress
        int originalU = list.get(list.size() - 1);
        int U = originalU > 0 ? originalU : 1;
        // It could happen that U is 0 (if we only have one docId=0), in this case set U=1
        int n = list.size();

        // number of bits
        int lowHalfLength = (int) Math.ceil(Math.log((float) U / n) / Math.log(2));
        if (lowHalfLength < 0)      // Fix when list = [0, 1]
            lowHalfLength = 0;
        int highHalfLength = (int) Math.ceil(Math.log(U) / Math.log(2)) - lowHalfLength;
        int nTotHighBits = n + ((int) Math.pow(2, highHalfLength));
        int nTotLowBits =  lowHalfLength*n;
        int nTotHighBytes = (int) Math.ceil((float) nTotHighBits / 8);
        int nTotLowBytes =  (int) Math.ceil((float)  nTotLowBits / 8);

        // structures to maintain encoded numbers
        BitSet highBitset = new BitSet(nTotHighBits);
        BitSet lowBitset = new BitSet(nTotLowBits);


        // utility
        int index = 0;
        Map<Integer, Integer> clusterComp = new HashMap<>();

        for (int number : list) {
            // low part
            BitSet extractedLowBitset = extractLowBitset(number, lowHalfLength);
            append(lowBitset, extractedLowBitset, lowHalfLength, index);
            index += lowHalfLength;

            // cluster composition of high part
            int highHalfInt = number >> (lowHalfLength); // computes the integer corresponding to the higher bits
            clusterComp.compute(highHalfInt, (key, value) -> (value == null) ? 1 : value + 1);  // keeps track of the number of integers in the clusters
        }

        // last byte (otherwise 0 bit at the end won't be written to buffer)
        BitSet mask = new BitSet(8);
        mask.set(7);
        append(lowBitset, mask, 8, index);

        // high part
        index = 0;
        for (int i=0; i<=Math.pow(2,highHalfLength); i++){
            int value = clusterComp.get(i) == null ? 0 : clusterComp.get(i);
            append(highBitset, integerToUnary(value), integerToUnary(value).length()+1, index);
            index += integerToUnary(value).length() + 1;
        }
        append(highBitset, mask, 8, index);

        // Convert everything as byte list
        ByteBuffer buffer = ByteBuffer.allocate(2 * Integer.BYTES + nTotHighBytes + nTotLowBytes);
        buffer.putInt(originalU)
                .putInt(n)
                .put(highBitset.toByteArray(), 0, nTotHighBytes)
                .put(lowBitset.toByteArray(), 0, nTotLowBytes);

        return buffer.flip().array();
    }

    @Override
    public List<Integer> decode(byte[] byteList) {
        List<Integer> decoded = new ArrayList<>();
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteList);

        int originalU = byteBuffer.getInt();
        int U = originalU > 0 ? originalU : 1;
        int n = byteBuffer.getInt();

        // number of bits
        int lowHalfLength = (int) Math.ceil(Math.log((float) U / n) / Math.log(2));
        if (lowHalfLength < 0)
            lowHalfLength = 0;
        int highHalfLength = (int) Math.ceil(Math.log(U) / Math.log(2)) - lowHalfLength;
        int nTotHighBits = (int) (n + Math.pow(2, highHalfLength));
        int nTotLowBits = lowHalfLength*n;

        byte[] lowBytes = new byte[(int) Math.ceil((float) nTotLowBits / 8)];
        byte[] highBytes = new byte[(int) Math.ceil((float) nTotHighBits / 8)];
        byteBuffer.get(highBytes);
        byteBuffer.get(lowBytes);

        BitSet lowBitset = BitSet.valueOf(lowBytes);
        BitSet highBitset = BitSet.valueOf(highBytes);

        int groupValue = 0;
        int lowBitsetIndex =0;
        for (int i=0; i<nTotHighBits; i++) {
            if (highBitset.get(i)) {
                int shifted = groupValue;

                for (int j = lowBitsetIndex * lowHalfLength;
                     j < lowBitsetIndex*lowHalfLength + lowHalfLength;
                     j++)
                {
                    int bitValue = lowBitset.get(j)? 1:0;
                    shifted = shifted << 1 | bitValue;
                }

                lowBitsetIndex++;
                decoded.add(shifted);
            } else
                groupValue++;
        }

        return decoded;
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

    public static void append(BitSet bitset1, BitSet bitset2, int len, int offset) {
        // appends bitset2 to bitset1, starting from offset for len bits
        for (int i = 0; i<len; i++)
            bitset1.set(offset++, bitset2.get(i));
    }
}
