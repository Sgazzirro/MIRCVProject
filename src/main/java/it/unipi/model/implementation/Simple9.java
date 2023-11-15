package it.unipi.model.implementation;

import java.util.ArrayList;
import java.util.List;

public class Simple9 {

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

    // Encode a list of numbers using the Simple9 compression algorithm
    public static int[] encode(List<Integer> intList) {
        List<Integer> byteList = new ArrayList<>();

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

            byteList.add(block);
        }

        return byteList.stream().mapToInt(Integer::intValue).toArray();
    }

    public static List<Integer> decode(int[] byteList) {
        List<Integer> intList = new ArrayList<>();

        int conf;
        for (int b : byteList) {
            // The first 4 bits represent the configuration of the block
            conf = b >>> 28;

            int nBits = CONFIGURATIONS[conf][0];
            // Get only the bits we are interested in: for example if nBits = 4
            // then mask = [0....00001111]_2
            int mask = (1 << nBits) - 1;

            for (int shift = 28-nBits; shift >= 0; shift -= nBits)
                intList.add( (b >>> shift) & mask);
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
        throw new RuntimeException("Simple9 error: cannot encode n = " + intList.get(0) + " (n > 2^28)");
    }
}
