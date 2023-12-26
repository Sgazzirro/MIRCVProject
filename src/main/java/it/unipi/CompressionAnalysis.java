package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.EncodingType;
import it.unipi.encoding.implementation.*;
import it.unipi.index.InMemoryIndex;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.DocumentStream;
import it.unipi.io.implementation.DumperCompressed;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class CompressionAnalysis {

    static class CompressionAnalysisIndexer extends InMemoryIndex {

        private final DumperCompressed[] dumpers = new DumperCompressed[] {
                new DumperCompressed(new PForDelta(EncodingType.DOC_IDS), new PForDelta(EncodingType.TERM_FREQUENCIES)),
                new DumperCompressed(new Simple9(EncodingType.DOC_IDS), new Simple9(EncodingType.TERM_FREQUENCIES)),
                new DumperCompressed(new EliasFano(EncodingType.DOC_IDS), new UnaryEncoder(EncodingType.TERM_FREQUENCIES)),
                new DumperCompressed(new VariableByteEncoder(EncodingType.DOC_IDS), new VariableByteEncoder(EncodingType.TERM_FREQUENCIES))
        };
        private final String[] encodings = new String[] {"PForDelta", "Simple9", "Unary", "VBE"};

        private Path path;

        public CompressionAnalysisIndexer() {
            super(CompressionType.COMPRESSED);
        }

        @Override
        public boolean setup(Path path) {
            super.setup(path);
            this.path = path;
            boolean ret = true;

            for (int i = 0; i < dumpers.length; i++)
                ret = ret && dumpers[i].start(path.resolve(encodings[i]));
            return ret;
        }

        @Override
        public void close() {
            super.close();

            try {
                for (int i = 0; i < dumpers.length; i++) {
                    dumpers[i].close();
                    Path vocPath = path.resolve(encodings[i]).resolve(Constants.VOCABULARY_FILENAME);
                    Path docPath = path.resolve(encodings[i]).resolve(Constants.DOCUMENT_INDEX_FILENAME);
                    Path docIdsPath = path.resolve(encodings[i]).resolve(Constants.DOC_IDS_FILENAME);
                    Path termFreqPath = path.resolve(encodings[i]).resolve(Constants.TERM_FREQ_FILENAME);

                    vocPath.toFile().delete(); docPath.toFile().delete();
                    System.out.printf("%15s encoding: doc ids size = %9d, term frequencies size = %9d\n", encodings[i], Files.size(docIdsPath), Files.size(termFreqPath));
                }
            } catch (Exception ignored) { }
        }

        @Override
        public void dumpVocabularyLine(Map.Entry<String, VocabularyEntry> entry) throws IOException {
            for (DumperCompressed dumper : dumpers)
                dumper.dumpVocabularyEntry(entry);
        }
    }

    public static void main(String[] args) throws IOException {
        Path indexPath = Constants.DATA_PATH.resolve("compression");
        File collectionFile = Constants.COLLECTION_FILE;

        DocumentStream stream = new DocumentStream(collectionFile);
        InMemoryIndex globalIndexer = new CompressionAnalysisIndexer();

        SPIMIIndex spimi = new SPIMIIndex(globalIndexer, stream);
        spimi.buildIndex(indexPath);
    }
}
