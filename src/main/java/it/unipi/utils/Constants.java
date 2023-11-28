package it.unipi.utils;

public class Constants {
    private static boolean COMPRESSION = false;

    public static final String COLLECTION_FILE = "data/reduced_collection.tar.gz";
    public static final String TEST_COLLECTION_FILE = "data/test_collection.tsv";

    public static final String VOCABULARY_FILENAME = "vocabulary.bin";
    public static final String DOCUMENT_INDEX_FILENAME = "document_index.bin";
    public static final String DOC_IDS_POSTING_FILENAME = "doc_ids.bin";
    public static final String TF_POSTING_FILENAME = "term_frequencies.bin";

    // Stopwords downloaded from https://raw.githubusercontent.com/stopwords-iso/stopwords-en/master/stopwords-en.txt
    public static final String STOPWORDS_FILE = "data/stopwords-en.txt";

    public static int BLOCK_SIZE = 100;
    public static final int BYTES_STORED_STRING = 32;
    public static final int VOCABULARY_ENTRY_BYTES_SIZE =
            BYTES_STORED_STRING + 3*Integer.BYTES + 2*Double.BYTES + 2*Long.BYTES;

    public static void setCompression(boolean c) {
        COMPRESSION = c;
    }

    public static boolean getCompression(){
        return COMPRESSION;
    }
}

