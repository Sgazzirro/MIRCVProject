package it.unipi.encoding;

import it.unipi.encoding.implementation.UnaryEncoder;
import it.unipi.utils.Constants;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UnaryEncoderTest {

    private byte[] test(List<Integer> list, Encoder encoder) {
        byte[] bytes = encoder.encode(list);
        assertEquals(list, encoder.decode(bytes));

        return bytes;
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testBasic(EncodingType encoding) {
        Encoder encoder = new UnaryEncoder(encoding);
        List<Integer> list = List.of(1,2,3,4);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testNextBlock(EncodingType encoding) {
        Encoder encoder = new UnaryEncoder(encoding);
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
    public void testHeader() {
        Encoder encoder = new UnaryEncoder(EncodingType.DOC_IDS);
        List<Integer> list = List.of(0,1,2,3,4,5,6,7,8, 16, 24);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        assertEquals(15, buffer.getInt());              // block length
        assertEquals(24, buffer.getInt());              // upper bound
        assertEquals(0,  buffer.getInt());              // first number

        assertEquals("00000000", getBinaryRepresentation(buffer.get()));
        assertEquals("11111110", getBinaryRepresentation(buffer.get()));
        assertEquals("11111110", getBinaryRepresentation(buffer.get()));
    }

    @Test
    public void testAll1() {
        Encoder encoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,1,1,1,1,1,1,1);

        test(list, encoder);
    }

    @Test
    public void testStarting0() {
        Encoder encoder = new UnaryEncoder(EncodingType.DOC_IDS);
        List<Integer> list = List.of(0,1,2,3);

        test(list, encoder);
    }

    @Test
    public void testEncodeAll1() {
        Encoder encoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,1,1,1,1,1,1,1);
        byte[] bytes = test(list, encoder);

        assertEquals("00000000", getBinaryRepresentation(bytes[Integer.BYTES]));
    }

    @Test
    public void testEncodeAll1Gaps() {
        Encoder encoder = new UnaryEncoder(EncodingType.DOC_IDS);
        List<Integer> list = List.of(10,11,12,13,14,15,16,17,18);
        byte[] bytes = test(list, encoder);

        assertEquals(10, bytes[11]);
        assertEquals("00000000", getBinaryRepresentation(bytes[3*Integer.BYTES]));
    }

    @Test
    public void testEncodeBigNumber() {
        Encoder encoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(10);
        byte[] bytes = test(list, encoder);

        assertEquals("11111111", getBinaryRepresentation(bytes[4]));
        assertEquals("10111111", getBinaryRepresentation(bytes[5]));
    }

    @Test
    public void testDecodeBigNumber() {
        Encoder encoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(10);

        byte[] bytes = new byte[] {
                0,0,0,3,
                (byte) Integer.parseInt("11111111", 2),
                (byte) Integer.parseInt("10111111", 2),
        };

        assertEquals(list, encoder.decode(bytes));
    }

    @Test
    public void testEncodeNotFullBlock() {
        Encoder encoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,2,3);
        byte[] bytes = test(list, encoder);

        assertEquals("01011011", getBinaryRepresentation(bytes[4]));
    }

    @Test
    public void testDecodeNotFullBlock() {
        Encoder encoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,2,3);
        byte[] bytes = new byte[] {
                0,0,0,2,
                0b01011011
        };

        assertEquals(list, encoder.decode(bytes));
    }

    @Test
    public void testEncodeFullBlock() {
        Encoder encoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,2,3,2);
        byte[] bytes = encoder.encode(list);

        assertEquals("01011010", getBinaryRepresentation(bytes[4]));
    }

    @Test
    public void testDecodeFullBlock() {
        Encoder encoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,2,3,2);
        byte[] bytes = new byte[] {
                0,0,0,2,
                0b01011010
        };

        assertEquals(list, encoder.decode(bytes));
    }

    @Test
    public void testEncode() {
        Encoder encoder = new UnaryEncoder(EncodingType.DOC_IDS);
        List<Integer> list = List.of(1000, 1001,1003,1006,1008, 1013,1014,1015,1016, 1020,1023);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        assertEquals(bytes.length, buffer.getInt());        // block length
        assertEquals(1023, buffer.getInt());        // upper bound
        assertEquals(1000, buffer.getInt());        // first number

        assertEquals("01011010", getBinaryRepresentation(buffer.get()));
        assertEquals("11110000", getBinaryRepresentation(buffer.get()));
        assertEquals("11101101", getBinaryRepresentation(buffer.get()));
    }

    @Test
    public void testDecode() {
        Encoder encoder = new UnaryEncoder(EncodingType.DOC_IDS);
        List<Integer> list = List.of(1000, 1001,1003,1006,1008, 1013,1014,1015,1016, 1020,1023);
        byte[] bytes = new byte[] {
                0,0,0,15,                       // block length
                0,0,0b11, (byte) 0b11111111,    // upper bound
                0,0,0b11, (byte) 0b11101000,    // first number
                (byte) Integer.parseInt("01011010", 2),
                (byte) Integer.parseInt("11110000", 2),
                (byte) Integer.parseInt("11101101", 2)
        };

        assertEquals(list, encoder.decode(bytes));
    }

    private static String getBinaryRepresentation(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }
}
