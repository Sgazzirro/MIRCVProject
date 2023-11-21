package it.unipi.utils;

public class Constants {
    private static boolean COMPRESSION = false;

    public static final String COLLECTION_FILE = "data/reduced_collection.tar.gz";
    public static final String TEST_COLLECTION_FILE = "data/test_collection.tsv";

    public static final String VOCABULARY_FILENAME = "vocabulary.dat";
    public static final String DOCUMENT_INDEX_FILENAME = "data/document_index.dat";
    public static final String DOC_IDS_POSTING_FILENAME = "data/doc_ids.txt";
    public static final String TF_POSTING_FILENAME = "data/term_frequencies.txt";

    // Stopwords downloaded from https://raw.githubusercontent.com/stopwords-iso/stopwords-en/master/stopwords-en.txt
    public static final String STOPWORDS_FILE = "data/stopwords-en.txt";

    public static final int BLOCK_SIZE = 100;
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

