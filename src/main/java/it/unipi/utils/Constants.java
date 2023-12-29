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
    ////////////////////////////////// PATHS //////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(Constants.class);
    public static final Path DATA_PATH = Paths.get("./data");
    public static final Path TEST_PATH = DATA_PATH.resolve("test");
    public static final File COLLECTION_FILE = DATA_PATH.resolve("collection.tar.gz").toFile();
    public static final String VOCABULARY_FILENAME = "vocabulary.bin";
    public static final String DOCUMENT_INDEX_FILENAME = "document_index.bin";
    public static final String DOC_IDS_FILENAME = "doc_ids.bin";
    public static final String TERM_FREQ_FILENAME = "term_frequencies.bin";
    public static final Path STOPWORDS_FILE = DATA_PATH.resolve("stopwords-en.txt"); // Stopwords downloaded from https://raw.githubusercontent.com/stopwords-iso/stopwords-en/master/stopwords-en.txt
    public static final List<String> STOPWORDS = IOUtils.loadStopwords();
    private static Path currentPath;

    ////////////////////////////////// BYTES ENTRIES //////////////////////////////////
    public static final int BYTES_STORED_STRING = 32;
    public static final int VOCABULARY_ENTRY_BYTES_SIZE = BYTES_STORED_STRING + 3*Integer.BYTES + Double.BYTES + 2*Long.BYTES;
    public static final int DOCUMENT_INDEX_ENTRY_BYTES_SIZE = 3*Integer.BYTES;
    public static final int VOCABULARY_HEADER_BYTES = 0;
    public static final int DOCUMENT_INDEX_HEADER_BYTES = 2*Integer.BYTES;

    ////////////////////////////////// COMPRESSION //////////////////////////////////
    public static int BLOCK_SIZE = 10_000;
    private static CompressionType currentCompression = CompressionType.COMPRESSED;

    ////////////////////////////////// SPIMI //////////////////////////////////
    public static int MAX_ENTRIES_PER_SPIMI_BLOCK = 1_000_000;

    ////////////////////////////////// SCORING //////////////////////////////////
    public static double BM25_b = 0.75;
    public static double BM25_k = 1.2;
    private static ScoringType scoringType = ScoringType.BM25;

    ////////////////////////////////// GLOBAL STRUCTURES //////////////////////////////////

    public static Vocabulary vocabulary;

    public static DocumentIndex documentIndex;

    public static int N;

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
            vocabulary = new Vocabulary(influencersR);
        }
        else
            vocabulary = new Vocabulary();

        documentIndex = new DocumentIndex();
        if(currentCompression==CompressionType.DEBUG){
            documentIndex.chargeInMemory(); // speed reasons in TXT
        }
        else documentIndex.chargeHeader();
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

