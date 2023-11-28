package it.unipi.model.implementation;

import it.unipi.utils.ByteUtils;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Simple9Test extends TestCase {

    Simple9 simple9;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        simple9 = new Simple9();
    }

    public void testSkippingPointer() {
        Simple9 simple9Skipping = new Simple9(true);
        List<Integer> list = Arrays.asList(1,3,6,2,10,11,2,
                500, 128, 109);
        byte[] bytes = simple9Skipping.encode(list);
        int bytesToSkip = ByteUtils.bytesToInt(bytes);

        assertEquals(bytesToSkip, 4 + 2*4);
    }

    public void testEncodeAll1() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 27; i++)
            list.add(1);
        byte[] bytes = simple9.encode(list);
        int block = ByteUtils.bytesToInt(bytes);

        assertEquals(getBinaryRepresentation(block), "00001111111111111111111111111110");
    }

    public void testEncodeSecondConfiguration() {
        List<Integer> list = Arrays.asList(1,2,3,1,1,3,3,2,1,2,1);
        byte[] bytes = simple9.encode(list);
        int block = ByteUtils.bytesToInt(bytes);

        assertEquals(getBinaryRepresentation(block), "00010110110101111110011001000000");
    }

    public void testFifthConfiguration() {
        List<Integer> list = Arrays.asList(6, 19, 31, 1, 10);
        byte[] bytes = simple9.encode(list);
        int block = ByteUtils.bytesToInt(bytes);

        assertEquals(getBinaryRepresentation(block), "01000011010011111110000101010000");
    }

    public void testLastConfiguration() {
        List<Integer> list = List.of(128 * 128);
        byte[] bytes = simple9.encode(list);
        int block = ByteUtils.bytesToInt(bytes);

        assertEquals(getBinaryRepresentation(block), "10000000000000000100000000000000");
    }

    public void testMultipleBytes() {
        List<Integer> list = Arrays.asList(1,3,6,2,10,11,2,
                500, 128, 109);
        byte[] bytes = simple9.encode(list);
        int block0 = ByteUtils.bytesToInt(bytes, 0);
        int block1 = ByteUtils.bytesToInt(bytes, 4);

        assertEquals(getBinaryRepresentation(block0), "00110001001101100010101010110010");
        assertEquals(getBinaryRepresentation(block1), "01101111101000100000000011011010");
    }

    private String getBinaryRepresentation(int n) {
        return String.format("%32s", Integer.toBinaryString(n)).replace(" ", "0");
    }

    public void testDecode() {
        byte[] byteList = {
                0b00110001, 0b00110110, 0b00101010, (byte) 0b10110010,
                0b01101111, (byte) 0b10100010, 0b00000000, (byte) 0b11011010,
                (byte) 0b10000000, 0b00000000, 0b01000000, 0b00000000
        };
        List<Integer> intList = simple9.decode(byteList);

        assertEquals(
                intList,
                Arrays.asList(1,3,6,2,10,11,2, 500,128,109, 128*128)
        );
    }
}