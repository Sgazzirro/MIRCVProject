package it.unipi.encoding;

import it.unipi.encoding.implementation.Simple9;
import it.unipi.utils.ByteUtils;
import it.unipi.utils.Constants;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Simple9Test {

    private byte[] test(List<Integer> list, Encoder encoder) {
        byte[] bytes = encoder.encode(list);
        assertEquals(list, encoder.decode(bytes));

        return bytes;
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testBasic(EncodingType encoding) {
        Encoder encoder = new Simple9(encoding);
        List<Integer> list = List.of(1,2,3,4);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testNextBlock(EncodingType encoding) {
        Encoder encoder = new Simple9(encoding);
        List<Integer> list1 = new ArrayList<>(),
                list2 = new ArrayList<>(),
                list3 = new ArrayList<>();
        for (int i = 1; i <= Constants.BLOCK_SIZE; i++) {
            list1.add(                         i);
            list2.add(  Constants.BLOCK_SIZE + i);
            list3.add(2*Constants.BLOCK_SIZE + i);
        }

        byte[] bytes1 = test(list1, encoder);
        byte[] bytes2 = test(list2, encoder);
        byte[] bytes3 = test(list3, encoder);
        ByteBuffer buffer = ByteBuffer.allocate(bytes1.length + bytes2.length + bytes3.length);
        buffer.put(bytes1).put(bytes2).put(bytes3);

        byte[] blocks = buffer.flip().array();
        int length1 = encoder.getNextBlock(blocks, 0).length();
        int length2 = encoder.getNextBlock(blocks, length1).length();
        int length3 = encoder.getNextBlock(blocks, length1+length2).length();

        assertEquals(bytes1.length, length1);
        assertEquals(bytes2.length, length2);
        assertEquals(bytes3.length, length3);
    }

    @Test
    public void testAll1() {
        Encoder encoder = new Simple9(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,1,1,1,1,1,1,1);

        test(list, encoder);
    }

    @Test
    public void testSecondConfiguration() {
        Encoder encoder = new Simple9(EncodingType.DOC_IDS);
        List<Integer> list = Arrays.asList(1,3,6,7,8,11,14,16,17,19,20);

        byte[] bytes = test(list, encoder);
        int block = ByteUtils.bytesToInt(bytes, 2*Integer.BYTES);

        assertEquals(getBinaryRepresentation(block), "00011010110101111110011001000000");
    }

    @Test
    public void testLastConfiguration() {
        Encoder encoder = new Simple9(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(128 * 128);

        byte[] bytes = test(list, encoder);
        int block = ByteUtils.bytesToInt(bytes, Integer.BYTES);

        assertEquals(getBinaryRepresentation(block), "10000000000000000100000000000000");
    }

    @Test
    public void testMultipleBytes() {
        Encoder encoder = new Simple9(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = Arrays.asList(1,3,6,2,10,11,2, 500,128,109);

        byte[] bytes = test(list, encoder);
        int block0 = ByteUtils.bytesToInt(bytes, Integer.BYTES);
        int block1 = ByteUtils.bytesToInt(bytes, 2*Integer.BYTES);

        assertEquals(getBinaryRepresentation(block0), "00110001001101100010101010110010");
        assertEquals(getBinaryRepresentation(block1), "01101111101000100000000011011010");
    }

    @Test
    public void testDecode() {
        Encoder encoder = new Simple9(EncodingType.TERM_FREQUENCIES);
        byte[] byteList = {
                0,0,0,4,
                0b00110001, 0b00110110, 0b00101010, (byte) 0b10110010,
                0b01101111, (byte) 0b10100010, 0b00000000, (byte) 0b11011010,
                (byte) 0b10000000, 0b00000000, 0b01000000, 0b00000000
        };
        List<Integer> intList = encoder.decode(byteList);

        assertEquals(
                Arrays.asList(1,3,6,2,10,11,2, 500,128,109, 128*128),
                intList
        );
    }

    @Test
    public void testDecodeGaps() {
        Encoder encoder = new Simple9(EncodingType.DOC_IDS);
        byte[] byteList = {
                0,0,0,11,        // length
                0,0,0b01000011,0b00000100,
                0b00110010, 0b00110110, 0b00101010, (byte) 0b10110010,
                0b01101111, (byte) 0b10100010, 0b00000000, (byte) 0b11011010,
                (byte) 0b10000000, 0b00000000, 0b01000000, 0b00000000
        };
        List<Integer> intList = encoder.decode(byteList);

        assertEquals(
                Arrays.asList(1,4,10,12,22,33,35, 535,663,772, 772+128*128),
                intList
        );
    }

    @Test
    public void testHeader() {
        Encoder encoder = new Simple9(EncodingType.DOC_IDS);
        List<Integer> list = Arrays.asList(100,200,201,204);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        assertEquals(bytes.length, buffer.getInt());
        assertEquals(204, buffer.getInt());
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testGeneral(EncodingType encoding) {
        Encoder encoder = new Simple9(encoding);
        List<Integer> list = List.of(10923,10939,14960,15094,15561,59781,192059,221235);

        test(list, encoder);
    }

    private static String getBinaryRepresentation(int n) {
        return String.format("%32s", Integer.toBinaryString(n)).replace(' ', '0');
    }
}