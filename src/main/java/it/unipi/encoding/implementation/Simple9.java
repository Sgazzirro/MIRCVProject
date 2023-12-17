package it.unipi.encoding.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;

public class Simple9 implements Encoder {

    // Each block is 4 byte large
    //  - First 4 bit describe the layout of the other 28 bits
    //    with 9 possible configurations
    //  - A matrix describes these configurations: for 0 <= i <= 8
    //        CONFIGURATIONS[i] = {l, k, waste, maxN}
    //    where
    //      -- l is the length in bit of each code
    //      -- k is the number of codes in the block
    //      -- waste is the number of wasted bits at the end of the block
    //      -- maxN is the highest number representable in l bits

    private static final int[][] CONFIGURATIONS = {
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

    private final boolean useSkippingPointer;

    public Simple9(boolean useSkippingPointer) {
        this.useSkippingPointer = useSkippingPointer;
    }

    public Simple9() {
        this(false);
    }

    public byte[] encode(List<Integer> intList) {
        // Encode a list of numbers using the Simple9 compression algorithm
        List<Integer> blockList = new ArrayList<>();

        int conf, block; // Encode a block as an int (4 byte long)
        while (!intList.isEmpty()) {
            conf = findConfiguration(intList);
            block = conf;

            // Encode numbers until the block or the list end
            int blockEnd = Math.min(intList.size(), CONFIGURATIONS[conf][1]);
            int nBits = CONFIGURATIONS[conf][0];
            for (int i = 0; i < blockEnd; i++)
                // Shift n bits and add the n bits of the number in the list
                block = (block << nBits) | intList.get(i);
            intList = intList.subList(blockEnd, intList.size());

            // Complete the block by adding 0 bits
            block <<= CONFIGURATIONS[conf][2];
            block <<= (CONFIGURATIONS[conf][1] - blockEnd) * nBits;     // Only if the list is ended and the block is incomplete

            blockList.add(block);
        }

        if (useSkippingPointer) {
            // Add a skipping pointer at the beginning with structure
            //     number of bytes to skip
            int byteToSkip;
            byteToSkip = 4 + 4 * blockList.size();

            // Encode byteToSkip as the first block
            blockList.add(0, byteToSkip);
        }

        int skippingPointerSize = (useSkippingPointer) ? 4 : 0;
        byte[] byteArray = new byte[skippingPointerSize + 4 * blockList.size()];
        int offset = 0;
        for (int b : blockList) {
            ByteUtils.intToBytes(b, byteArray, offset);
            offset += 4;
        }

        return byteArray;
    }

    public List<Integer> decode(byte[] byteList) {
        List<Integer> intList = new ArrayList<>();

        // Read 4 bytes at a time and skip the first 4 bytes if skipping pointer is used
        int offset = (useSkippingPointer) ? 4 : 0;
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
            for (int shift = 28-nBits; shift >= 0; shift -= nBits)
                intList.add( (b >>> shift) & mask );
        }

        return intList;
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
