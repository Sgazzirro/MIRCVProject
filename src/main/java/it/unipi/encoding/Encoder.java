package it.unipi.encoding;

import it.unipi.encoding.implementation.Simple9;
import it.unipi.encoding.implementation.UnaryEncoder;

import java.util.List;

public abstract class Encoder {

    // Field that represents if we are encoding doc ids or term frequencies
    public final EncodingType encoding;

    private static Encoder docIdsEncoder;
    private static Encoder termFrequenciesEncoder;

    public Encoder(EncodingType encoding) {
        this.encoding = encoding;
    }

    public static Encoder getDocIdsEncoder() {
        if (docIdsEncoder == null)
            docIdsEncoder = new Simple9(EncodingType.DOC_IDS);

        return docIdsEncoder;
    }

    public static void setDocIdsEncoder(Encoder docIdsEncoder) {
        Encoder.docIdsEncoder = docIdsEncoder;
    }

    public static Encoder getTermFrequenciesEncoder() {
        if (termFrequenciesEncoder == null)
            termFrequenciesEncoder = new UnaryEncoder(EncodingType.TERM_FREQUENCIES);

        return termFrequenciesEncoder;
    }

    public static void setTermFrequenciesEncoder(Encoder termFrequenciesEncoder) {
        Encoder.termFrequenciesEncoder = termFrequenciesEncoder;
    }

    public abstract byte[] encode(List<Integer> intList);
    public abstract List<Integer> decode(byte[] byteArray);
    public abstract IntegerListBlock getNextBlock(byte[] compressedList, int blockOffset);
}
