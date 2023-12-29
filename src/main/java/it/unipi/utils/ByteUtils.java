package it.unipi.utils;

import it.unipi.encoding.CompressionType;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.PostingList;
import it.unipi.model.VocabularyEntry;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ByteUtils {

    public static void intToBytes(byte[] bytes, int offset, int... numbers) {
        // Convert number n in its byte representation and put it in the byte array starting from offset:
        //     n -> bytes[offset], bytes[offset+1], bytes[offset+2], bytes[offset+3]
        for (int n : numbers) {
            bytes[offset]     = (byte) (n >>> 24);
            bytes[offset + 1] = (byte) (n >>> 16);
            bytes[offset + 2] = (byte) (n >>> 8);
            bytes[offset + 3] = (byte) n;

            offset += 4;
        }
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
            if ( (bytes[i] >>> 7 & 0b1) == 0)
                break;

            // Count the continuation bytes
            if ( ( (bytes[i] >> 6) & 0b11 ) == 0b10)
                continuationBytes++;
            // Check if the number of continuation bytes match the first byte of multibyte sequence
            else if ( ( (bytes[i] >> 6) & 0b11 ) == 0b11) {
                // 1 continuation byte  -> first byte is 110xxxxx
                // 2 continuation bytes -> first byte is 1110xxxx
                if ( ((bytes[i] & 0xFF) >>> (6 - continuationBytes)) != ((1 << (continuationBytes+2)) - 2) ) {
                    // There is not a correct number of continuation bytes, remove all trailing bytes
                    bytes[i] = '\0';
                    len = i+1;
                }

                // Everything is okay
                break;
            }
        }

        bytes = Arrays.copyOfRange(bytes, offset, offset + len);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }

    public static VocabularyEntry bytesToVocabularyEntry(byte[] byteList, CompressionType compression) {
        ByteBuffer bytes = ByteBuffer.wrap(byteList);
        bytes.position(Constants.BYTES_STORED_STRING);
        int documentFrequency = bytes.getInt();
        double upperBound = bytes.getDouble();
        long docIdsOffset = bytes.getLong();
        int docIdsLength = bytes.getInt();
        long termFreqOffset = bytes.getLong();
        int termFreqLength = bytes.getInt();

        VocabularyEntry entry = new VocabularyEntry();
        entry.setDocumentFrequency(documentFrequency);
        entry.setUpperBound(upperBound);
        entry.setDocIdsOffset(docIdsOffset);
        entry.setTermFreqOffset(termFreqOffset);
        entry.setDocIdsLength(docIdsLength);
        entry.setTermFreqLength(termFreqLength);

        PostingList postingList = PostingList.getInstance(compression, entry);

        entry.setPostingList(postingList);

        return entry;
    }

    public static DocumentIndexEntry bytesToDocumentIndexEntry(byte[] documentIndexEntryBytes) {
        int documentLength = bytesToInt(documentIndexEntryBytes, 0);
        int docNo = bytesToInt(documentIndexEntryBytes, Integer.BYTES);

        DocumentIndexEntry entry = new DocumentIndexEntry();
        entry.setDocumentLength(documentLength);
        entry.setDocNo(docNo);
        return entry;
    }

    public static byte[] subArray(byte[] src, int offset, int length) {
        byte[] dst = new byte[length];
        System.arraycopy(src, offset, dst, 0, length);
        return dst;
    }
}
