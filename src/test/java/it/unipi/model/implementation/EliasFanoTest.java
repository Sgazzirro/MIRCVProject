package it.unipi.model.implementation;

import it.unipi.model.Encoder;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.unipi.utils.ByteUtils.binaryStringToByteArray;
import static it.unipi.utils.ByteUtils.byteArrayToBinaryString;

public class EliasFanoTest extends TestCase {

    private Encoder eliasFano = new EliasFano();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testEncodeOnly0() {
        List<Integer> list = List.of(0);
        byte[] compressedBytes = eliasFano.encode(list);
        assertEquals("000000000000000000000000000000000000000000000000000000000000000100000001", byteArrayToBinaryString(compressedBytes));
    }

    public void testDecodeOnly0() {
        String binaryString = "000000000000000000000000000000000000000000000000000000000000000100000001";
        byte[] byteArray = binaryStringToByteArray(binaryString);
        List<Integer> list = eliasFano.decode(byteArray);
        assertEquals(List.of(0), list);
    }

    public void testEncode1(){
        Integer[] array = {3,4,7,13,14,15,21,25,36,38,54,62};
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(array));
        byte [] compressedBytes = eliasFano.encode(list);
        // U | n | list
        assertEquals( "00000000000000000000000000111110000000000000000000000000000011000111011100110101000001011100111010111011100101111101100100000110",byteArrayToBinaryString(compressedBytes));
    }

    public void testDecode1(){
        String binaryString = "00000000000000000000000000111110000000000000000000000000000011000111011100110101000001011100111010111011100101111101100100000110";
        byte[] byteArray = binaryStringToByteArray(binaryString);
        assertEquals(Arrays.asList(3,4,7,13,14,15,21,25,36,38,54,62), eliasFano.decode(byteArray));
    }

    public void testEncodeDecode() {
        List<Integer> list = List.of(1, 2);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }
}
