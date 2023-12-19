package it.unipi.utils;

import it.unipi.encoding.CompressionType;
import it.unipi.model.DocumentIndex;
import it.unipi.model.Vocabulary;
import it.unipi.scoring.ScoringType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Constants {

    public static final String COLLECTION_FILE = "data/collection.tar.gz";
    public static final String TEST_COLLECTION_FILE = "data/test_collection.tsv";

    public static final Path dataPath = Paths.get("./data/");
    public static final Path testPath = Paths.get("./data/", "test/");

    public static final String VOCABULARY_FILENAME = "vocabulary.bin";
    public static final String DOCUMENT_INDEX_FILENAME = "document_index.bin";
    public static final String DOC_IDS_POSTING_FILENAME = "doc_ids.bin";
    public static final String TF_POSTING_FILENAME = "term_frequencies.bin";

    // Stopwords downloaded from https://raw.githubusercontent.com/stopwords-iso/stopwords-en/master/stopwords-en.txt
    public static final Path STOPWORDS_FILE = Paths.get("data/", "400stops.txt");
    public static final List<String> STOPWORDS = IOUtils.loadStopwords();

    public static int BLOCK_SIZE = 100;
    public static int MAX_ENTRIES_PER_SPIMI_BLOCK = 1_000_000;
    public static final int BYTES_STORED_STRING = 32;

    public static int N = 8841823;
    public static final int VOCABULARY_ENTRY_BYTES_SIZE =
            BYTES_STORED_STRING + 3*Integer.BYTES + Double.BYTES + 2*Long.BYTES;
    public static final int DOCUMENT_INDEX_ENTRY_BYTES_SIZE =
            2*Integer.BYTES;
    public static final int VOCABULARY_HEADER_BYTES = 0;
    public static final int DOCUMENT_INDEX_HEADER_BYTES = 2*Integer.BYTES;

    // BM25 constants
    public static double BM25_b = 0.75;
    public static double BM25_k = 1.6;
    private static ScoringType scoringType = ScoringType.TFIDF;

    private static Path currentPath;
    private static CompressionType currentCompression = CompressionType.DEBUG;

    // Global structures
    public static Vocabulary vocabulary;
    public static DocumentIndex documentIndex;

    public static CompressionType getCompression() {
        return currentCompression;
    }

    public static void setCompression(CompressionType compression) {
        Constants.currentCompression = compression;

        vocabulary = Vocabulary.getInstance();
        documentIndex = DocumentIndex.getInstance();
    }

    public static Path getPath() {
        return currentPath;
    }

    public static void setPath(Path path) {
        Constants.currentPath = path;
    }

    public static ScoringType getScoring() {
        return scoringType;
    }

    public static void setScoring(ScoringType scoring) {
        Constants.scoringType = scoring;
    }
}

