package it.unipi.encoding.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.encoding.EncodingType;
import it.unipi.encoding.IntegerListBlock;
import it.unipi.utils.ByteUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PForDelta extends Encoder {

    private static final double PERCENTAGE_ENCODED = 0.9;

    public PForDelta(EncodingType encoding) {
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

        // Structure of encoded bytes
        //  - 4 bytes               for U (if we are encoding doc ids)
        //  - 4 bytes               for b
        //  - 1 byte                for k
        //  - 4 bytes               for list.size()
        //  - 3 bytes               for outliersBlock length
        //  - k * list.size() bits  for listToEncode
        //  - remaining bytes       for outliers

        int b = Collections.min(listToEncode);
        int k = findOptimalK(listToEncode);
        int maxGap = (1 << k) - 2, placeholder = (1 << k) - 1;

        List<Integer> outliers = new ArrayList<>();

        // Encode code section (section where numbers are encoded)
        int codeSectionBytes = (int) Math.ceil(k * listToEncode.size() / 8.);
        byte[] codeSection = new byte[codeSectionBytes];

        int offset = 0;
        for (int n : listToEncode) {
            int gap = n - b;

            if (gap > maxGap) {
                addIntToByteArray(placeholder, codeSection, offset, k);
                outliers.add(gap);
            } else
                addIntToByteArray(gap, codeSection, offset, k);

            offset += k;
        }

        // Outliers block length
        int outliersBytes = Integer.BYTES * outliers.size();

        // Upper bound (if we are encoding doc ids)
        int upperBoundBytes = (encoding == EncodingType.DOC_IDS) ? Integer.BYTES : 0;

        offset = 0;
        byte[] bytes = new byte[upperBoundBytes + 3*Integer.BYTES + codeSectionBytes + outliersBytes];
        // Dump upper bound (4 bytes)
        if (encoding == EncodingType.DOC_IDS) {
            ByteUtils.intToBytes(bytes, 0, Collections.max(intList));
            offset += upperBoundBytes;
        }
        // Dump b (4 bytes)
        ByteUtils.intToBytes(bytes, offset, b);
        offset += Integer.BYTES;
        // Dump k (1 byte)
        bytes[offset] = (byte) k;
        offset += Byte.BYTES;
        // Dump length of the list to encode (4 bytes)
        ByteUtils.intToBytes(bytes, offset, listToEncode.size());
        offset += Integer.BYTES;
        // Dump length of the outliers block (3 bytes, shift by 8 bit in order to remove last byte)
        ByteUtils.intToBytes(bytes, offset, outliersBytes << 8);
        offset += (Integer.BYTES - Byte.BYTES);
        // Dump listToEncode
        System.arraycopy(codeSection, 0, bytes, offset, codeSectionBytes);
        offset += codeSectionBytes;
        // Dump outliers list
        for (int n : outliers) {
            ByteUtils.intToBytes(bytes, offset, n);
            offset += Integer.BYTES;
        }

        return bytes;
    }

    @Override
    public List<Integer> decode(byte[] bytes) {
        List<Integer> intList = new ArrayList<>();

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        // Skip first 4 bytes of upper bound if we are encoding doc ids
        int upperBoundBytes = (encoding == EncodingType.DOC_IDS) ? Integer.BYTES : 0;
        buffer.position(upperBoundBytes);

        int b = buffer.getInt(),
                k = buffer.get(),
                length = buffer.getInt(),
                outliersBlockLength = buffer.getInt() >>> 8;
        int placeholder = (1 << k) - 1;
        int codeSectionBytes = (int) Math.ceil(k * length / 8.);

        // Decode outliers
        int startOutliersOffset = upperBoundBytes + 3*Integer.BYTES + codeSectionBytes;
        List<Integer> outliers = new ArrayList<>();
        for (int offset = 0; offset < outliersBlockLength; offset += Integer.BYTES)
            outliers.add(ByteUtils.bytesToInt(bytes, startOutliersOffset + offset));

        // Decode list
        int offset = (upperBoundBytes + 3*Integer.BYTES) * 8;

        // Iterate over all bits of codeSection
        int num = 0, gap;
        for (int bitOffset = offset; bitOffset < offset + k*length; bitOffset += k) {
            gap = getIntFromByteArray(bytes, bitOffset, k);

            // Check if we have an outlier
            if (gap == placeholder)
                gap = outliers.remove(0);

            // Decode gaps if we encoded doc ids
            num = (encoding == EncodingType.DOC_IDS) ? (num + b + gap) : b + gap;
            intList.add(num);
        }

        return intList;
    }

    @Override
    public IntegerListBlock getNextBlock(byte[] compressedList, int blockOffset) {// Structure of encoded bytes
        // Upper bound (if we are encoding doc ids)
        int upperBound = -1, upperBoundBytes = 0;
        if (encoding == EncodingType.DOC_IDS) {
            upperBoundBytes = Integer.BYTES;
            upperBound = ByteUtils.bytesToInt(compressedList, blockOffset);
        }

        ByteBuffer buffer = ByteBuffer.wrap(compressedList);
        buffer.position(blockOffset + upperBoundBytes + Integer.BYTES);     // Skip b and upperBound
        int k = buffer.get(),
                length = buffer.getInt(),
                outliersBlockLength = buffer.getInt() >>> 8;
        int codeSectionLength = (int) Math.ceil(k * length / 8.);

        // Get offset of next block
        return new IntegerListBlock(
                upperBoundBytes + 3*Integer.BYTES + codeSectionLength + outliersBlockLength,
                upperBound
        );
    }

    /**
     * Encode an int into a byte array starting from a certain offset given its bit length
     * @param num the number to encode
     * @param array the byte array
     * @param bitOffset the offset in bits where to encode the number
     * @param k number of bits encoding
     */
    private static void addIntToByteArray(int num, byte[] array, int bitOffset, int k) {
        // Given the bit offset:
        //  - byte offset in array: offset / 8
        //  - bit offset in byte:   offset % 8
        int arrayOffset = bitOffset / 8,
                byteOffset = bitOffset % 8;

        // Examples:
        //  - number on only 1 byte:
        //    k = 5, bitOffset = 10 -->  ________ __+++++_
        //  - number between 2 bytes:
        //    k = 5, bitOffset = 5  -->  _____+++ ++______
        //  - number between multiple bytes:
        //    k = 13, bitOffset = 13  -->
        //      ________ _____+++ ++++++++ ++______

        // Get the first byte to work onto
        //  - ________ __+++++_  --> __+++++_
        //  - _____+++           --> _____+++
        byte currentByte = array[arrayOffset];

        // Add bits to the current byte
        int bitsToAdd = 0;
        while ( (arrayOffset+1)*8 < bitOffset + k ) {
            bitsToAdd += (8 - byteOffset);

            // If we must add the first 3 bits of num then we must shift num by k - 3 bits
            currentByte = (byte) (currentByte | ( (num >>> (k - bitsToAdd)) & 0xFF ));
            array[arrayOffset++] = currentByte;

            currentByte = 0; byteOffset = 0;
        }

        // At this point we just have to add the bits to the last byte
        //  - ... __+++++_  -->  bitsToShiftLeft = 1
        //  - ... _____+++  -->  bitsToShiftLeft = 0
        //  - ... ++______  -->  bitsToShiftLeft = 6
        int bitsToShift = 8 - ((bitOffset + k) % 8);
        bitsToShift = (bitsToShift == 8) ? 0 : bitsToShift;

        currentByte = (byte) (currentByte | (num << bitsToShift));
        array[arrayOffset] = currentByte;
    }

    /**
     * Get the next int from a byte array starting from a certain offset given its bit length
     * @param array the byte array
     * @param bitOffset the offset in bits where the number is encoded
     * @param k number of bits encoding
     * @return the number decoded
     */
    private static int getIntFromByteArray(byte[] array, int bitOffset, int k) {
        // Given the bit offset:
        //  - byte offset in array: offset / 8
        //  - bit offset in byte:   offset % 8
        int arrayOffset = bitOffset / 8,
                byteOffset = bitOffset % 8;

        // Examples:
        //  - number on only 1 byte:
        //    k = 5, bitOffset = 10 -->  ________ __10101_
        //  - number between 2 bytes:
        //    k = 5, bitOffset = 5  -->  _____101 01______
        //  - number between multiple bytes:
        //    k = 13, bitOffset = 13  -->
        //      ________ _____101 01010101 01______

        // Get the beginning in bits of our number
        //  - __10101_  --> 0010101_
        //  - _____101  --> 00000101
        int n = array[arrayOffset] & ( (1 << (8 - (byteOffset))) - 1 );

        // Concatenate bits until we reach the last byte
        while ( (++arrayOffset)*8 < bitOffset + k )
            n = (n << 8) | (array[arrayOffset] & 0xFF);

        // At this point we just have to remove excess bits at the end
        //  - ... 01______  -->  bitsToRemove = 6
        //  - ... ___10101  -->  bitsToRemove = 0
        int bitsToRemove = 8 - ((bitOffset + k) % 8);
        bitsToRemove = (bitsToRemove == 8) ? 0 : bitsToRemove;

        return n >>> bitsToRemove;
    }

    // Find k such that a fixed percentage of numbers (90%) falls in the range [b, b+2^k-1)
    private static int findOptimalK(List<Integer> intList) {
        int k = 1, b = Collections.min(intList);

        int feasibleNumbers = 0, listSize = intList.size();
        while (true) {
            int maxN = (1 << k) - 1;
            for (int n : intList) {
                if (n - b < maxN)
                    feasibleNumbers++;
            }

            if ( ((double) feasibleNumbers) / listSize >= PERCENTAGE_ENCODED )
                return k;

            feasibleNumbers = 0;
            k++;
        }
    }
}
