package it.unipi.encoding.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.encoding.EncodingType;
import it.unipi.encoding.IntegerListBlock;
import it.unipi.utils.ByteUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VariableByteEncoder extends Encoder {

    public VariableByteEncoder(EncodingType encoding) {
        super(encoding);
    }

    @Override
    public byte[] encode(List<Integer> intList) {
        List<Integer> listToEncode;

        // Encode gaps if we have to encode doc ids
        if (encoding == EncodingType.DOC_IDS)
            listToEncode = Utils.computeGaps(intList);
        else
            listToEncode = intList;

        List<Byte> byteList = new ArrayList<>();

        for (int n : listToEncode) {
            if (n < 0)
                throw new UnsupportedOperationException("VariableByte encoder: cannot encode n = " + n);

            List<Byte> variableBytes = new ArrayList<>(4);

            byte currentByte;

            // Encode 7 bits at a time and
            // set the first bit to 1 if we must use another byte
            while (true) {
                currentByte = (byte) (n & 0b01111111);

                n >>>= 7;
                if (n == 0) {
                    variableBytes.add(0, currentByte);
                    break;
                } else
                    variableBytes.add(0, (byte) (currentByte | 0b10000000));
            }

            byteList.addAll(variableBytes);
        }

        // Encoded block has structure
        //     block length | upper bound | gaps    (if we encode gaps)
        //     block length | list                  (if we encode the whole numbers)
        int blockLength = Integer.BYTES + byteList.size();
        if (encoding == EncodingType.DOC_IDS)
            blockLength += Integer.BYTES;

        byte[] bytes = new byte[blockLength];

        // Add skipping pointer at the beginning with structure
        //     block length | upper bound
        ByteUtils.intToBytes(bytes, 0, blockLength);
        if (encoding == EncodingType.DOC_IDS)
            ByteUtils.intToBytes(bytes, Integer.BYTES, Collections.max(intList));

        // Add actual bytes
        int offset = Integer.BYTES + (encoding == EncodingType.DOC_IDS ? Integer.BYTES : 0);
        for (byte b : byteList)
            bytes[offset++] = b;

        return bytes;
    }

    @Override
    public List<Integer> decode(byte[] byteArray) {
        List<Integer> intList = new ArrayList<>();

        int num = 0, offset = Integer.BYTES;
        // If we encoded gaps, 4 bytes after the block length are reserved for upper bound
        if (encoding == EncodingType.DOC_IDS)
            offset += Integer.BYTES;

        // Start reading the first byte
        int gap = byteArray[offset++];

        for (byte b : byteArray) {
            // Skip initial bytes
            if (offset-- > 0)
                continue;

            // When we see a byte whose first bit is 0 it means that we finished decoding
            // the previous number, and we can start decoding a new one
            // Otherwise we have to decode its last 7 bits
            if ( ((b >>> 7) & 1) == 1 )
                gap = (gap << 7) | (b & 0b01111111);
            else {
                num = (encoding == EncodingType.DOC_IDS) ? num + gap : gap;
                intList.add(num);
                gap = b;
            }
        }

        // Add last number since we saw all bytes
        num = (encoding == EncodingType.DOC_IDS) ? num + gap : gap;
        intList.add(num);

        return intList;
    }

    @Override
    public IntegerListBlock getNextBlock(byte[] compressedList, int blockOffset) {
        // Read length of the block
        int length = ByteUtils.bytesToInt(compressedList, blockOffset);

        int upperBound = -1;
        if (encoding == EncodingType.DOC_IDS)
            upperBound = ByteUtils.bytesToInt(compressedList, blockOffset + Integer.BYTES);

        return new IntegerListBlock(length, upperBound);
    }
}
