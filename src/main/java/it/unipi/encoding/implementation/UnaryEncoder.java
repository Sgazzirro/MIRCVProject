package it.unipi.encoding.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.encoding.EncodingType;
import it.unipi.encoding.IntegerListBlock;
import it.unipi.utils.ByteUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnaryEncoder extends Encoder {

    public UnaryEncoder(EncodingType encoding) {
        super(encoding);
    }

    @Override
    public byte[] encode(List<Integer> intList) {
        List<Integer> listToEncode;

        // If we encode gaps, encode the first number separately on 4 bytes
        int firstNumber = 0;

        // Encode gaps if we have to encode doc ids
        if (encoding == EncodingType.DOC_IDS) {
            listToEncode = Utils.computeGaps(intList);
            firstNumber = listToEncode.remove(0);
        } else
            listToEncode = intList;

        List<Byte> byteList = new ArrayList<>();

        int index = 0;
        byte currentByte = 0;
        for (int n : listToEncode) {
            if (n <= 0)
                throw new UnsupportedOperationException("Unary encoder: cannot encode n = " + n);

            do {
                currentByte = (byte) (currentByte << 1);

                // Add (n-1) 1 bits
                if (n > 1)
                    currentByte = (byte) (currentByte | 1);

                if (index % 8 == 7) {
                    byteList.add(currentByte);
                    currentByte = 0;
                }
                index++;

            } while (n-- > 1);
        }

        // Add last byte by right padding with ones
        // (if we padded with zeros we would add fictitious 1 at the end of the list in decoding phase)
        if (index % 8 > 0) {
            int bitsToShift = 8 - (index % 8);
            currentByte = (byte) ((currentByte << bitsToShift) | ((1 << bitsToShift) - 1));

            byteList.add(currentByte);
        }

        // Encoded block has structure
        //     block length | upper bound | first number | gaps   (if we encode gaps)
        //     block length | list                                (if we encode the whole numbers)
        int blockLength = Integer.BYTES + byteList.size();
        if (encoding == EncodingType.DOC_IDS)
            blockLength += 2 * Integer.BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(blockLength);

        // Add skipping pointer at the beginning with structure
        //     block length | upper bound | first number
        buffer.putInt(blockLength);
        if (encoding == EncodingType.DOC_IDS) {
            buffer.putInt(Collections.max(intList));
            buffer.putInt(firstNumber);
        }

        // Add actual bytes
        for (byte b : byteList)
            buffer.put(b);

        return buffer.flip().array();
    }

    @Override
    public List<Integer> decode(byte[] byteArray) {
        List<Integer> intList = new ArrayList<>();

        int num = 0, offset = Integer.BYTES;
        // If we encoded gaps, 8 bytes after the block length are reserved for upper bound and first number
        if (encoding == EncodingType.DOC_IDS) {
            num = ByteUtils.bytesToInt(byteArray, 2*Integer.BYTES);
            intList.add(num);

            offset = 3*Integer.BYTES;
        }

        int gap = 1;
        for (byte b : byteArray) {
            // Skip first 12 bytes if we encoded gaps, first 4 otherwise
            if (offset-- > 0)
                continue;

            // Iterate over all bits of the byte
            for (int _i = 0; _i < 8; _i++) {
                if ( ((b >> 7) & 1) == 1 )      // nextBit = 1
                    gap++;
                else {
                    // Decode gaps if we encoded doc ids
                    num = (encoding == EncodingType.DOC_IDS) ? num + gap : gap;
                    intList.add(num);
                    gap = 1;
                }

                b <<= 1;        // shift bit
            }
        }

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
