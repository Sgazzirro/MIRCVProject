package it.unipi.encoding.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.encoding.EncodingType;
import it.unipi.encoding.IntegerListBlock;
import it.unipi.utils.ByteUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Simple9 extends Encoder {

    /**
     *  Each block is 4 byte large
     *  - First 4 bit describe the layout of the other 28 bits
     *    with 9 possible configurations
     *  - A matrix describes these configurations: for 0 <= i <= 8
     *        CONFIGURATIONS[i] = {l, k, waste, maxN}
     *    where
     *      -- l is the length in bit of each code
     *      -- k is the number of codes in the block
     *      -- waste is the number of wasted bits at the end of the block
     *      -- maxN is the highest number representable in l bits
     */
    static final int[][] CONFIGURATIONS = {
            {1,  28, 0, 1},
            {2,  14, 0, 3},
            {3,  9,  1, 7},
            {4,  7,  0, 15},
            {5,  5,  3, 31},
            {7,  4,  0, 127},
            {9,  3,  1, 511},
            {14, 2,  0, 128*128-1},
            {28, 1,  0, 128*128*128*128-1}
    };

    public Simple9(EncodingType encoding) {
        super(encoding);
    }

    @Override
    public byte[] encode(List<Integer> intList) {
        List<Integer> listToEncode;
        // Encode gaps if we have to encode doc ids (and add 1 to the first docId to avoid the case docId = 0)
        if (encoding == EncodingType.DOC_IDS) {
            listToEncode = Utils.computeGaps(intList);
            listToEncode.set(0, listToEncode.get(0)+1);
        } else
            listToEncode = intList;

        List<Integer> blockList = new ArrayList<>();

        int conf, block; // Encode a block as an int (4 byte long)
        while (!listToEncode.isEmpty()) {
            conf = findConfiguration(listToEncode);
            block = conf;

            // Encode numbers until the block or the list end
            int blockEnd = Math.min(listToEncode.size(), CONFIGURATIONS[conf][1]);
            int nBits = CONFIGURATIONS[conf][0];
            for (int i = 0; i < blockEnd; i++)
                // Shift n bits and add the n bits of the number in the list
                block = (block << nBits) | listToEncode.get(i);
            listToEncode = listToEncode.subList(blockEnd, listToEncode.size());

            // Complete the block by adding 0 bits
            block <<= CONFIGURATIONS[conf][2];
            block <<= (CONFIGURATIONS[conf][1] - blockEnd) * nBits;     // Only if the list is ended and the block is incomplete

            blockList.add(block);
        }

        // Add a skipping pointer at the beginning with structure
        //     block length | upper bound
        if (encoding == EncodingType.DOC_IDS)
            blockList.add(0, Collections.max(intList));

        // We add 4 bytes that represent the block length
        int blockLength = 4 * blockList.size() + Integer.BYTES;

        // Encode byteToSkip as the first block
        blockList.add(0, blockLength);

        byte[] byteArray = new byte[4 * blockList.size()];
        int offset = 0;
        for (int b : blockList) {
            ByteUtils.intToBytes(byteArray, offset, b);
            offset += 4;
        }

        return byteArray;
    }

    @Override
    public List<Integer> decode(byte[] byteList) {
        List<Integer> intList = new ArrayList<>();

        int num = -1,       // Subtract 1 to the first docId since we added 1 in encoding phase
                gap;

        // Read 4 bytes at a time (starting after skipping pointer bytes)
        int offset = Integer.BYTES + ((encoding == EncodingType.DOC_IDS) ? Integer.BYTES : 0);
        for ( ; offset < byteList.length; offset += 4) {
            // Convert the 4 bytes to int
            int b = ByteUtils.bytesToInt(byteList, offset);
            // The first 4 bits represent the configuration of the block
            int conf = b >>> 28;

            int nBits = CONFIGURATIONS[conf][0];
            // Get only the bits we are interested in: for example if nBits = 3
            // then mask = [0....00000111]_2
            int mask = (1 << nBits) - 1;

            // Example: if nBits = 3
            //  - first get  0000xxx0000000...
            //  - second get 0000000xxx0000... and so on
            for (int shift = 28-nBits; shift >= 0; shift -= nBits) {
                gap = (b >>> shift) & mask;
                // If we start decoding zeros we are at the end of the list
                if (gap == 0)
                    break;

                // Decode gaps if we encoded doc ids
                num = (encoding == EncodingType.DOC_IDS) ? num + gap : gap;
                intList.add(num);
            }
        }

        return intList;
    }

    @Override
    public IntegerListBlock getNextBlock(byte[] compressedList, int blockOffset) {
        // Read length of the block
        int length = ByteUtils.bytesToInt(compressedList, blockOffset);

        int upperBound = -1;
        if (encoding == EncodingType.DOC_IDS)
            upperBound = ByteUtils.bytesToInt(compressedList, blockOffset + Integer.BYTES);

        return new IntegerListBlock(length, upperBound);
    }

    private static int findConfiguration(List<Integer> intList) {
        configurationSearch:
        for (int conf = 0; conf < CONFIGURATIONS.length; conf++) {
            // Select most appropriate configuration i.e. the one that can store
            // the largest number of codes

            // Check until the end of the block or the end of the list
            int listEnd = Math.min(intList.size(), CONFIGURATIONS[conf][1]);

            for (int i = 0; i < listEnd; i++) {
                // If this number is not suitable for this configuration
                // go the next configuration
                //         list[i] > maxN
                if (intList.get(i) > CONFIGURATIONS[conf][3])
                    continue configurationSearch;
            }
            return conf;
        }

        // If we reached this point it means that the first number of the list
        // is not representable in 28 bits
        throw new RuntimeException("Simple9 error: cannot encode n = " + intList.get(0) + " (n >= 2^28)");
    }
}
