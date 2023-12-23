package it.unipi.encoding;

import it.unipi.encoding.implementation.Simple9;
import it.unipi.utils.ByteUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Simple9Test {

    private static Encoder simple9;

    @BeforeClass
    public static void setUp() {
        simple9 = new Simple9();
    }

    @Test
    public void testSkippingPointer() {
        List<Integer> list = Arrays.asList(1, 3, 6, 2, 10, 11, 2,
                500, 128, 109);
        byte[] bytes = simple9.encode(list);
        int bytesToSkip = ByteUtils.bytesToInt(bytes);

        assertEquals(bytesToSkip, 2*4);
    }

    @Test
    public void testEncodeAll1() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 27; i++)
            list.add(1);
        byte[] bytes = simple9.encode(list);
        int block = ByteUtils.bytesToInt(bytes, Integer.BYTES);

        assertEquals(getBinaryRepresentation(block), "00001111111111111111111111111110");
    }

    @Test
    public void testEncodeSecondConfiguration() {
        List<Integer> list = Arrays.asList(1,2,3,1,1,3,3,2,1,2,1);
        byte[] bytes = simple9.encode(list);
        int block = ByteUtils.bytesToInt(bytes, Integer.BYTES);

        assertEquals(getBinaryRepresentation(block), "00010110110101111110011001000000");
    }

    @Test
    public void testFifthConfiguration() {
        List<Integer> list = Arrays.asList(6, 19, 31, 1, 10);
        byte[] bytes = simple9.encode(list);
        int block = ByteUtils.bytesToInt(bytes, Integer.BYTES);

        assertEquals(getBinaryRepresentation(block), "01000011010011111110000101010000");
    }

    @Test
    public void testLastConfiguration() {
        List<Integer> list = List.of(128 * 128);
        byte[] bytes = simple9.encode(list);
        int block = ByteUtils.bytesToInt(bytes, Integer.BYTES);

        assertEquals(getBinaryRepresentation(block), "10000000000000000100000000000000");
    }

    @Test
    public void testMultipleBytes() {
        List<Integer> list = Arrays.asList(1, 3, 6, 2, 10, 11, 2,
                500, 128, 109);
        byte[] bytes = simple9.encode(list);
        int block0 = ByteUtils.bytesToInt(bytes, Integer.BYTES);
        int block1 = ByteUtils.bytesToInt(bytes, 2*Integer.BYTES);

        assertEquals(getBinaryRepresentation(block0), "00110001001101100010101010110010");
        assertEquals(getBinaryRepresentation(block1), "01101111101000100000000011011010");
    }

    @Test
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

    private String getBinaryRepresentation(int n) {
        return String.format("%32s", Integer.toBinaryString(n)).replace(" ", "0");
    }
}