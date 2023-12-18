package it.unipi.io.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.encoding.implementation.EliasFano;
import it.unipi.encoding.implementation.Simple9;
import it.unipi.utils.Constants;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class DumperCompressed extends DumperBinary {

    private final Encoder docIdsEncoder = new EliasFano();
    private final Encoder tfEncoder = new Simple9(true);

    @Override
    protected int dumpDocIds(List<Integer> docIdList) throws IOException {
        return dumpEncodedList(docIdList, docIdsEncoder, docIdsWriter);
    }

    @Override
    protected int dumpTermFrequencies(List<Integer> termFrequencyList) throws IOException {
        return dumpEncodedList(termFrequencyList, tfEncoder, termFreqWriter);
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

        int written_size = 0;
        while (written_size != size)
            written_size += (int) writer.write(buffers.toArray(new ByteBuffer[0]));

        return written_size;
    }
}