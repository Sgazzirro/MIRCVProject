package it.unipi.utils;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.VocabularyEntry;
import it.unipi.model.implementation.PostingListCompressed;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

    public static int bytesToInt(byte[] bytes) {
        return bytesToInt(bytes, 0);
    }

    public static String bytesToString(byte[] bytes, int offset, int len) {
        int continuationBytes = 0;      // They start with 10xxxxxx

        for (int i=offset+len-1; i >= offset; i--) {
            if ( (bytes[i] >>> 7 & 0b1) == 0) {
                break;
            }

            // Count the continuation bytes
            if ( ( (bytes[i] >> 6) & 0b11 ) == 0b10)
                continuationBytes++;
            // Check if the number of continuation bytes match the first byte of multibyte sequence
            else if ( ( (bytes[i] >> 6) & 0b11 ) == 0b11) {
                // 1 continuation byte  -> first byte is 110xxxxx
                // 2 continuation bytes -> first byte is 1110xxxx
                if ( ((bytes[i] & 0xFF) >>> (6 - continuationBytes)) != ((1 << (continuationBytes+2)) - 2) ) {
                    // There is not a correct number of continuation bytes, set to 0 all trailing bytes
                    for (int j = i; j < bytes.length; j++) {
                        bytes[j] = '\0';
                    }
                }

                // Everything is okay
                break;
            }
        }

        byte[] marcoIdea = new byte[32];
        System.arraycopy(bytes, 0, marcoIdea, 0, 32);
        return new String(marcoIdea, StandardCharsets.UTF_8).trim();
    }

    public static VocabularyEntry bytesToVocabularyEntry(byte[] byteList, CompressionType compression) {
        ByteBuffer bytes = ByteBuffer.wrap(byteList);
        bytes.position(Constants.BYTES_STORED_STRING);
        int documentFrequency = bytes.getInt();
        double upperBound = bytes.getDouble();
        double idf = bytes.getDouble();
        long docIdsOffset = bytes.getLong();
        int docIdsLength = bytes.getInt();
        long termFreqOffset = bytes.getLong();
        int termFreqLength = bytes.getInt();

        VocabularyEntry entry = new VocabularyEntry();
        entry.setDocumentFrequency(documentFrequency);
        entry.setUpperBound(upperBound);

        PostingList postingList = PostingList.getInstance(compression);
        postingList.setDocIdsOffset(docIdsOffset);
        postingList.setTermFreqOffset(termFreqOffset);
        postingList.setDocIdsLength(docIdsLength);
        postingList.setTermFreqLength(termFreqLength);
        postingList.setIdf(idf);
        /// modifica che ho fatto per calcolare lo score
        postingList.setDocumentFrequency(documentFrequency);
        ///
        entry.setPostingList(postingList);

        return entry;
    }

    private static String byteToBinary(int b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(" ", "0");
    }

    public static String byteArrayToBinaryString(byte[] byteArray) {
        StringBuilder binaryStringBuilder = new StringBuilder();

        for (byte b : byteArray) {
            // Convert each byte to binary and append to the StringBuilder
            binaryStringBuilder.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        return binaryStringBuilder.toString();
    }

    public static byte[] binaryStringToByteArray(String binaryString) {
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

    public static PostingListCompressed.ByteBlock fetchDocIdsBlock(byte[] compressedDocIds, int docId, long docIdsBlockOffset) {
        // skips unnecessary lists
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedDocIds);
                DataInputStream dis = new DataInputStream(bais)
        ) {
            if (docIdsBlockOffset != dis.skip(docIdsBlockOffset))
                throw new IOException();
            while (true) {
                // Read integers from the byte array
                int U = dis.readInt();
                U = (U == 0) ? 1 : U;
                int n = dis.readInt();
                docIdsBlockOffset += (2*Integer.BYTES);
                // computing number of bytes to skip
                int lowHalfLength = (int) Math.ceil(Math.log((float) U / n) / Math.log(2));
                int highHalfLength = (int) Math.ceil(Math.log(U) / Math.log(2)) - lowHalfLength;
                int nTotLowBits = lowHalfLength * n;
                int nTotHighBits = (int) (n + Math.pow(2, highHalfLength));
                int bytesToSkip = (int) Math.ceil((float) nTotLowBits / 8) + (int) Math.ceil((float) nTotHighBits / 8);
                if (U < docId) {
                    // I have to read the next block
                    docIdsBlockOffset += bytesToSkip;
                    if (bytesToSkip != bais.skip(bytesToSkip))
                        throw new IOException();

                } else {
                    int numLowBytes = (int) Math.ceil((float) nTotLowBits/8);
                    int numHighBytes = (int) Math.ceil((float) nTotHighBits/8);
                    byte[] byteArray = new byte[4 + 4 + numLowBytes + numHighBytes];
                    ByteUtils.intToBytes(U, byteArray, 0);
                    ByteUtils.intToBytes(n, byteArray, 4);
                    if (numHighBytes!=0) docIdsBlockOffset += dis.read(byteArray, 8, numHighBytes);
                    if (numLowBytes!=0) docIdsBlockOffset += dis.read(byteArray, 8+numHighBytes, numLowBytes);
                    dis.close();

                    return new PostingListCompressed.ByteBlock(byteArray, docIdsBlockOffset);
                }
            }
        } catch (EOFException e) {
            // end of file reached
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PostingListCompressed.ByteBlock fetchNextTermFrequenciesBlock(byte[] compressedTermFrequencies, long termFrequenciesBlockOffset) {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedTermFrequencies);
                DataInputStream dis = new DataInputStream(bais)
        ) {
            if (termFrequenciesBlockOffset != dis.skip(termFrequenciesBlockOffset))
                throw new IOException();

            // Read length of the block
            int length = dis.readInt();

            byte[] byteArray = new byte[4 + length];
            ByteUtils.intToBytes(length, byteArray, 0);
            termFrequenciesBlockOffset += dis.read(byteArray, 4, length);
            termFrequenciesBlockOffset += 4;            // Consider also the first 4 bytes describing the block length
            dis.close();

            return new PostingListCompressed.ByteBlock(byteArray, termFrequenciesBlockOffset);

        } catch (EOFException e) {
            // end of file reached
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        String str = "ciao€";
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] truncatedBytes = new byte[6];
        System.arraycopy(bytes, 0, truncatedBytes, 0, 4);
        String newStr = bytesToString(truncatedBytes, 0, 6);
    }
}
