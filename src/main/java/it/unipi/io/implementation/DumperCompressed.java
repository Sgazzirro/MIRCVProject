package it.unipi.io.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.utils.Constants;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class DumperCompressed extends DumperBinary {

    private final Encoder docIdsEncoder;
    private final Encoder termFreqEncoder;

    public DumperCompressed(Encoder docIdsEncoder, Encoder termFreqEncoder) {
        this.docIdsEncoder = docIdsEncoder;
        this.termFreqEncoder = termFreqEncoder;
    }

    public DumperCompressed() {
        this(Encoder.getDocIdsEncoder(), Encoder.getTermFrequenciesEncoder());
    }

    @Override
    protected int dumpDocIds(List<Integer> docIdList) throws IOException {
        return dumpEncodedList(docIdList, docIdsEncoder, docIdsWriter);
    }

    @Override
    protected int dumpTermFrequencies(List<Integer> termFrequencyList) throws IOException {
        return dumpEncodedList(termFrequencyList, termFreqEncoder, termFreqWriter);
    }

    private int dumpEncodedList(List<Integer> list, Encoder encoder, FileChannel writer) throws IOException {
        List<ByteBuffer> buffers = new ArrayList<>();
        int size = 0;

        for (int i = 0; i < list.size(); i += Constants.BLOCK_SIZE) {
            List<Integer> blockList = list.subList(i, Math.min(list.size(), i + Constants.BLOCK_SIZE));
            byte[] byteList = encoder.encode(blockList);
            size += byteList.length;

            buffers.add(ByteBuffer.wrap(byteList));
        }

        int written = 0;
        for (ByteBuffer buffer : buffers)
            written += writer.write(buffer);

        if (written != size)
            throw new IOException("Could not dump posting list");

        return written;
    }
}