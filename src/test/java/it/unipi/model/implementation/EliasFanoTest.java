package it.unipi.model.implementation;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EliasFanoTest extends TestCase {
    public void testEncode1(){
        Integer[] array = {3,4,7,13,14,15,21,25,36,38,54,62};
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(array));
        byte [] compressedBytes = EliasFano.encode(list);
        // U | n | list
        assertEquals(byteArrayToBinaryString(compressedBytes), "000000000000000000000000001111100000000000000000000000000000110011101110101011001010011100111101110111101001100110110110");
    }

    public void testDecode1(){
        String binaryString = "000000000000000000000000001111100000000000000000000000000000110011101110101011001010011100111101110111101001100110110110";
        byte[] byteArray = binaryStringToByteArray(binaryString);
        assertEquals(EliasFano.decode(byteArray), Arrays.asList(3,4,7,13,14,15,21,25,36,38,54,62));
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

        for (int i = 0; i < length; i += 8) {
            String byteString = binaryString.substring(i, i + 8);
            byte b = (byte) Integer.parseInt(byteString, 2);
            byteArray[i / 8] = b;
        }

        return byteArray;
    }
}
