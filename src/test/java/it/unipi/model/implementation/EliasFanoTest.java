package it.unipi.model.implementation;

import it.unipi.model.Encoder;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EliasFanoTest extends TestCase {

    private Encoder eliasFano = new EliasFano();

    @Override
    public void setUp() throws Exception {
        super.setUp();
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
        assertEquals(Arrays.asList(3,4,7,13,14,15,21,25,36,38,54,62),eliasFano.decode(byteArray));
    }
    private String byteArrayToBinaryString(byte[] byteArray) {
        StringBuilder binaryStringBuilder = new StringBuilder();

        for (byte b : byteArray) {
            // Convert each byte to binary and append to the StringBuilder
            binaryStringBuilder.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        return binaryStringBuilder.toString();
    }
    private byte[] binaryStringToByteArray(String binaryString) {
        int length = binaryString.length();
        byte[] byteArray = new byte[length / 8];
        int index = 0; // Added index variable

        for (int i = 0; i < length; i += 8) {
            String byteString = binaryString.substring(i, i + 8);
            byte b = (byte) Integer.parseInt(byteString, 2);
            byteArray[index++] = b; // Use separate index variable
        }

        return byteArray;
    }

}
