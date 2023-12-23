package it.unipi.encoding;

import it.unipi.encoding.implementation.EliasFano;
import it.unipi.encoding.implementation.Simple9;

import java.util.List;

public abstract class Encoder {

    private static Encoder docIdsEncoder;
    private static Encoder termFrequenciesEncoder;

    public static Encoder getDocIdsEncoder() {
        if (docIdsEncoder == null)
            docIdsEncoder = new EliasFano();

        return docIdsEncoder;
    }

    public static Encoder getTermFrequenciesEncoder() {
        if (termFrequenciesEncoder == null)
            termFrequenciesEncoder = new Simple9(true);

        return termFrequenciesEncoder;
    }

    public abstract byte[] encode(List<Integer> intList);
    public abstract List<Integer> decode(byte[] byteArray);
}
