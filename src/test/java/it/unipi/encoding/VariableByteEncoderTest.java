package it.unipi.encoding;

import it.unipi.encoding.implementation.VariableByteEncoder;
import it.unipi.utils.Constants;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class VariableByteEncoderTest {

    private byte[] test(List<Integer> list, Encoder encoder) {
        byte[] bytes = encoder.encode(list);
        assertEquals(list, encoder.decode(bytes));

        return bytes;
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testBasic(EncodingType encoding) {
        Encoder encoder = new VariableByteEncoder(encoding);
        List<Integer> list = List.of(1,2,3,4);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testNextBlock(EncodingType encoding) {
        Encoder encoder = new VariableByteEncoder(encoding);
        List<Integer> list1 = new ArrayList<>(),
                list2 = new ArrayList<>(),
                list3 = new ArrayList<>();
        for (int i = 0; i < Constants.BLOCK_SIZE; i++) {
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
        Encoder encoder = new VariableByteEncoder(EncodingType.DOC_IDS);
        List<Integer> list = List.of(0,4,8, 16, 24);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        assertEquals(13, buffer.getInt());               // block length
        assertEquals(24, buffer.getInt());                 // upper bound

        assertEquals("00000000", getBinaryRepresentation(buffer.get()));
        assertEquals("00000100", getBinaryRepresentation(buffer.get()));
        assertEquals("00000100", getBinaryRepresentation(buffer.get()));
        assertEquals("00001000", getBinaryRepresentation(buffer.get()));
        assertEquals("00001000", getBinaryRepresentation(buffer.get()));
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testAll1(EncodingType encoding) {
        Encoder encoder = new VariableByteEncoder(encoding);
        List<Integer> list = List.of(1,1,1,1,1,1,1,1);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testStarting0(EncodingType encoding) {
        Encoder encoder = new VariableByteEncoder(encoding);
        List<Integer> list = List.of(0,1,2,3);

        test(list, encoder);
    }

    @Test
    public void testMultiByteNumber() {
        Encoder encoder = new VariableByteEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(128);
        byte[] bytes = encoder.encode(list);

        assertEquals("00000001", getBinaryRepresentation(bytes[4]));
        assertEquals("10000000", getBinaryRepresentation(bytes[5]));
    }

    @Test
    public void testDecodeMultiByteNumber() {
        Encoder encoder = new VariableByteEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(128);

        byte[] bytes = new byte[] {
                0,0,0,6,
                (byte) Integer.parseInt("00000001", 2),
                (byte) Integer.parseInt("10000000", 2),
        };

        assertEquals(list, encoder.decode(bytes));
    }

    @Test
    public void testEncode() {
        Encoder encoder = new VariableByteEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(63, 22, 513, 17000);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        assertEquals(bytes.length, buffer.getInt());        // block length

        assertEquals("00111111", getBinaryRepresentation(buffer.get()));
        assertEquals("00010110", getBinaryRepresentation(buffer.get()));
        assertEquals("00000100", getBinaryRepresentation(buffer.get()));
        assertEquals("10000001", getBinaryRepresentation(buffer.get()));
        assertEquals("00000001", getBinaryRepresentation(buffer.get()));
        assertEquals("10000100", getBinaryRepresentation(buffer.get()));
        assertEquals("11101000", getBinaryRepresentation(buffer.get()));
    }

    @Test
    public void testDecode() {
        Encoder encoder = new VariableByteEncoder(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(63, 22, 513, 17000);
        byte[] bytes = new byte[] {
                0,0,0,11,                       // block length
                0b00111111,
                0b00010110,
                0b00000100, (byte) 0b10000001,
                0b00000001, (byte) 0b10000100, (byte) 0b11101000
        };

        assertEquals(list, encoder.decode(bytes));
    }

    private static String getBinaryRepresentation(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }
}
