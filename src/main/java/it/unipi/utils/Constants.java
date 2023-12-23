package it.unipi.utils;

import it.unipi.encoding.CompressionType;
import it.unipi.model.DocumentIndex;
import it.unipi.model.Vocabulary;
import it.unipi.scoring.ScoringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Constants {

    private static final Logger logger = LoggerFactory.getLogger(Constants.class);

    public static final String COLLECTION_FILE = "data/collection.tar.gz";

    public static final Path dataPath = Paths.get("./data/");
    public static final Path testPath = Paths.get("./data/", "test/");

    public static final String VOCABULARY_FILENAME = "vocabulary.bin";
    public static final String DOCUMENT_INDEX_FILENAME = "document_index.bin";
    public static final String DOC_IDS_POSTING_FILENAME = "doc_ids.bin";
    public static final String TF_POSTING_FILENAME = "term_frequencies.bin";

    // Stopwords downloaded from https://raw.githubusercontent.com/stopwords-iso/stopwords-en/master/stopwords-en.txt
    public static final Path STOPWORDS_FILE = Paths.get("data/", "stopwords-en.txt");
    public static final List<String> STOPWORDS = IOUtils.loadStopwords();

    public static int BLOCK_SIZE = 5000;
    public static int NUM_THREADS_SPIMI = 8;
    public static int MAX_ENTRIES_PER_SPIMI_BLOCK = 1_000_000;

    public static int N = 8841823;

    public static final int BYTES_STORED_STRING = 32;
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
        logger.info("Compression set to " + compression);
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


    // FROM THIS ON, IS ALL FOR CACHING PURPOSES
    // -----------------------------------------------------------
    public static boolean CACHING = false;

    public static Map<String, Map.Entry<String, Long>> saved = new HashMap<>();
    public static PriorityQueue<Map.Entry<String, Long>> influencers =
            new PriorityQueue<>(Map.Entry.comparingByValue());

    private static final int SIZE_INFLUENCERS = 1000;

    public static double MEMORY_USED(){
        return (double) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Runtime.getRuntime().maxMemory());
    }

    public static void CACHE(String token, Long touches){
        if (!CACHING)
            return;
        // FIXME: Strange things will happen

        if(influencers.size() >= SIZE_INFLUENCERS && influencers.peek().getValue() < touches) {
            influencers.poll();
        }
        if(saved.containsKey(token)){
            influencers.remove(saved.get(token));
        }
        influencers.offer(Map.entry(token, touches));
        saved.put(token, Map.entry(token, touches));

    }
    public static void startSession(){
        if (CACHING) {
            PriorityQueue<Map.Entry<String, Long>> influencersR = new PriorityQueue<>(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            try (
                    BufferedReader reader = new BufferedReader(new FileReader("./data/cache/influencers.txt"))
            ) {
                String influence;

                while ( (influence = reader.readLine()) != null && MEMORY_USED() < 50 ) {
                    String[] params = influence.split(" ");
                    influencersR.offer(Map.entry(params[0], Long.parseLong(params[1])));
                    influencers.offer(Map.entry(params[0], Long.parseLong(params[1])));
                    saved.put(params[0], Map.entry(params[0], Long.parseLong(params[1])));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("QUI CI VADO");
            vocabulary = new Vocabulary(influencersR);
        }
        else
            vocabulary = new Vocabulary();

        documentIndex = new DocumentIndex();
    }

    public static void onExit(){
        if(!CACHING)
            return;
        // Save influencers
        try(
                BufferedWriter writer = new BufferedWriter(new FileWriter("./data/cache/influencers.txt", false))
        ){
            while (!influencers.isEmpty()) {
                Map.Entry<String, Long> entry = influencers.poll();
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

