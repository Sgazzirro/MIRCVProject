package it.unipi.utils;

public class ByteBlock {

    private final byte[] bytes;
    private final long offset;

    public ByteBlock(byte[] bytes, long offset) {
        this.bytes = bytes;
        this.offset = offset;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public long getOffset() {
        return offset;
    }
}
