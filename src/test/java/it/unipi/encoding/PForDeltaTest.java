package it.unipi.encoding;

import it.unipi.encoding.implementation.PForDelta;
import it.unipi.utils.ByteUtils;
import it.unipi.utils.Constants;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PForDeltaTest {

    private byte[] test(List<Integer> list, Encoder encoder) {
        byte[] bytes = encoder.encode(list);
        assertEquals(list, encoder.decode(bytes));

        return bytes;
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testBasic(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
        List<Integer> list = List.of(1,2,3,4);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testNextBlock(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
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
        Encoder encoder = new PForDelta(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(5,2,10,11,7,1000000,12,13,9,10,4);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        assertEquals(2, buffer.getInt());               // b
        assertEquals(4, buffer.get());                  // k
        assertEquals(list.size(), buffer.getInt());             // list.size()
        assertEquals(Integer.BYTES, buffer.getInt() >>> 8);  // outliers block length
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testAll1(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
        List<Integer> list = List.of(1,1,1,1,1,1,1,1);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testStarting0(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
        List<Integer> list = List.of(0,1,2,3);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testOutliers(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
        List<Integer> list = List.of(1,2,3,4,5,1000000,1000001,1000002,1000003,1000004,1000005);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testBasicFirstBigNumber(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
        List<Integer> list = List.of(10000001,10000002,10000003,10000004);

        test(list, encoder);
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testBasicFirstBigNumberWithOutliers(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
        List<Integer> list = List.of(10000001,10000002,10000003,10000004,10000005,
                10000006,10000007,10000008,10000009,10000010);

        test(list, encoder);
    }

    @Test
    public void testEncode() {
        Encoder encoder = new PForDelta(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,2,3, 2,5,1, 1,1, 4,3);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        assertEquals(1, buffer.getInt());               // b
        assertEquals(3, buffer.get());                  // k
        assertEquals(list.size(), buffer.getInt());             // list.size()
        assertEquals(0, buffer.getInt() >>> 8);  // outliers block length

        assertEquals("00000101", getBinaryRepresentation(bytes[12]));
        assertEquals("00011000", getBinaryRepresentation(bytes[13]));
        assertEquals("00000000", getBinaryRepresentation(bytes[14]));
        assertEquals("01101000", getBinaryRepresentation(bytes[15]));
    }

    @Test
    public void testEncodeGaps() {
        Encoder encoder = new PForDelta(EncodingType.DOC_IDS);
        List<Integer> list = List.of(1,3,6, 8,13,14, 15,16, 20,23);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        assertEquals(23, buffer.getInt());              // upper bound
        assertEquals(1, buffer.getInt());               // b
        assertEquals(3, buffer.get());                  // k
        assertEquals(list.size(), buffer.getInt());             // list.size()
        assertEquals(0, buffer.getInt() >>> 8);  // outliers block length

        assertEquals("00000101", getBinaryRepresentation(bytes[16]));
        assertEquals("00011000", getBinaryRepresentation(bytes[17]));
        assertEquals("00000000", getBinaryRepresentation(bytes[18]));
        assertEquals("01101000", getBinaryRepresentation(bytes[19]));
    }

    @Test
    public void testDecode() {
        Encoder encoder = new PForDelta(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,2,3, 2,5,1, 1,1, 4,3);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putInt(1)                        // b
                .put((byte) 3)                  // k
                .putInt(list.size())            // length
                .put(new byte[] {0,0,0});       // outliers block length

        buffer.put(new byte[] {
                (byte) Integer.parseInt("00000101", 2),
                (byte) Integer.parseInt("00011000", 2),
                (byte) Integer.parseInt("00000000", 2),
                (byte) Integer.parseInt("01101000", 2)
        });

        assertEquals(list, encoder.decode(buffer.flip().array()));
    }

    @Test
    public void testDecodeGaps() {
        Encoder encoder = new PForDelta(EncodingType.DOC_IDS);
        List<Integer> list = List.of(1,3,6, 8,13,14, 15,16, 20,23);

        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.putInt(23)
                .putInt(1)                      // b
                .put((byte) 3)                  // k
                .putInt(list.size())            // length
                .put(new byte[] {0,0,0});       // outliers block length

        buffer.put(new byte[] {
                (byte) Integer.parseInt("00000101", 2),
                (byte) Integer.parseInt("00011000", 2),
                (byte) Integer.parseInt("00000000", 2),
                (byte) Integer.parseInt("01101000", 2)
        });

        assertEquals(list, encoder.decode(buffer.flip().array()));
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testNumberLongerThanByte(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
        // k = 13
        List<Integer> list = List.of(1, (1 << 13) - 1);

        test(list, encoder);
    }

    @Test
    public void testOutliers() {
        Encoder encoder = new PForDelta(EncodingType.TERM_FREQUENCIES);
        List<Integer> list = List.of(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,128,128,1,1,1,1,1);

        byte[] bytes = test(list, encoder);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Preliminary info
        int b = buffer.getInt(),
                k = buffer.get(),
                length = buffer.getInt();
        int codeSectionBytes = (int) Math.ceil(k * length / 8.);
        assertEquals(1, b);
        assertEquals(1, k);
        assertEquals(list.size(), length);

        int outliersBlockLength = ByteUtils.bytesToInt(new byte[] {0, buffer.get(), buffer.get(), buffer.get()}, 0);
        assertEquals(2*Integer.BYTES, outliersBlockLength);

        buffer.position(3*Integer.BYTES + codeSectionBytes);
        assertEquals(127, buffer.getInt());
        assertEquals(127, buffer.getInt());
    }

    @ParameterizedTest
    @EnumSource( names = {"DOC_IDS", "TERM_FREQUENCIES"} )
    public void testGeneral(EncodingType encoding) {
        Encoder encoder = new PForDelta(encoding);
        List<Integer> list = List.of(10923,10939,14960,15094,15561,59781,192059,221235);

        test(list, encoder);
    }

    private static String getBinaryRepresentation(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }
}
