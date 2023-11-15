package it.unipi.model.implementation;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Simple9Test extends TestCase {

    public void testEncodeAll1() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 27; i++)
            list.add(1);
        int[] bytes = Simple9.encode(list);

        assertEquals(getBinaryRepresentation(bytes[0]), "00001111111111111111111111111110");
    }

    public void testEncodeSecondConfiguration() {
        List<Integer> list = Arrays.asList(1,2,3,1,1,3,3,2,1,2,1);
        int[] bytes = Simple9.encode(list);

        assertEquals(getBinaryRepresentation(bytes[0]), "00010110110101111110011001000000");
    }

    public void testFifthConfiguration() {
        List<Integer> list = Arrays.asList(6, 19, 31, 1, 10);
        int[] bytes = Simple9.encode(list);

        assertEquals(getBinaryRepresentation(bytes[0]), "01000011010011111110000101010000");
    }

    public void testLastConfiguration() {
        List<Integer> list = List.of(128 * 128);
        int[] bytes = Simple9.encode(list);

        assertEquals(getBinaryRepresentation(bytes[0]), "10000000000000000100000000000000");
    }

    public void testMultipleBytes() {
        List<Integer> list = Arrays.asList(1,3,6,2,10,11,2,
                500, 128, 109);
        int[] bytes = Simple9.encode(list);

        assertEquals(getBinaryRepresentation(bytes[0]), "00110001001101100010101010110010");
        assertEquals(getBinaryRepresentation(bytes[1]), "01101111101000100000000011011010");
    }

    private String getBinaryRepresentation(int n) {
        return String.format("%32s", Integer.toBinaryString(n)).replace(" ", "0");
    }

    public void testDecode() {
        int[] byteList = {
                Integer.parseUnsignedInt("00110001001101100010101010110010", 2),
                Integer.parseUnsignedInt("01101111101000100000000011011010", 2),
                Integer.parseUnsignedInt("10000000000000000100000000000000", 2)
        };
        List<Integer> intList = Simple9.decode(byteList);

        assertEquals(
                intList,
                Arrays.asList(1,3,6,2,10,11,2, 500,128,109, 128*128)
        );
    }
}