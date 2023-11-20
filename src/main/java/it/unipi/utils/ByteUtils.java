package it.unipi.utils;

public class ByteUtils {

    public static void intToBytes(int i, byte[] bytes, int offset) {
        // Convert the i in its byte representation and put it in the byte array starting from offset:
        //     i -> bytes[offset], bytes[offset+1], bytes[offset+2], bytes[offset+3]
        bytes[offset]   = (byte) (i >>> 24);
        bytes[offset+1] = (byte) (i >>> 16);
        bytes[offset+2] = (byte) (i >>> 8);
        bytes[offset+3] = (byte) (i);
    }

    public static int bytesToInt(byte[] bytes, int offset) {
        return (bytes[offset+3] & 0xFF) | ((bytes[offset+2] << 8) & 0xFF00) | ((bytes[offset+1] << 16) & 0xFF0000) | ((bytes[offset] << 24) & 0xFF000000);
    }
}
