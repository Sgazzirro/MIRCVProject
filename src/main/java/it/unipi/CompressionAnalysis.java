package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Encoder;
import it.unipi.encoding.EncodingType;
import it.unipi.encoding.implementation.*;
import it.unipi.index.InMemoryIndex;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.DocumentStream;
import it.unipi.io.implementation.BlockFetcherBinary;
import it.unipi.io.implementation.DumperCompressed;
import it.unipi.model.VocabularyEntry;
import it.unipi.scoring.ScoringType;
import it.unipi.utils.Constants;
import it.unipi.utils.Session;
import it.unipi.utils.Timing;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CompressionAnalysis {

    public static final int RUNS = 10;

    static class CompressionStats {

        public String docIdsEncoder;
        public String termFreqEncoder;
        public int blockSize;
        public ScoringType scoring;
        public long docIdsSize;
        public long termFreqSize;
        public long buildTime;

        public boolean cache;
        public double scoreTime;
        public int  runs;       // How many times the queries have been executed
        public double variance; // The variance between runs

        public String getIndexFolder() {
            return docIdsEncoder + "_" + termFreqEncoder + "_" + blockSize + "_" + scoring;
        }

        public static CompressionStats parseTXT(String line) {
            String[] split = line.split(",");

            CompressionStats stats = new CompressionStats();
            stats.docIdsEncoder = split[0]; stats.termFreqEncoder = split[1];
            stats.blockSize = Integer.parseInt(split[2]);
            stats.scoring = ScoringType.valueOf(split[3]);
            stats.docIdsSize = Long.parseLong(split[4]); stats.termFreqSize = Long.parseLong(split[5]);
            stats.buildTime = Long.parseLong(split[6]);
            stats.scoreTime = Double.parseDouble(split[7]);
            //stats.runs      = Integer.parseInt(split[8]);
            //stats.variance  = Double.parseDouble(split[9]);

            return stats;
        }

        @Override
        public String toString() {
            StringJoiner out = new StringJoiner(",");
            out.add(docIdsEncoder).add(termFreqEncoder)
                    .add(String.valueOf(blockSize)).add(scoring.toString())
                    .add(String.valueOf(docIdsSize)).add(String.valueOf(termFreqSize))
                    .add(String.valueOf(buildTime)).add(String.valueOf(scoreTime))
                    .add(String.valueOf(runs)).add(String.valueOf(variance))
                    .add(String.valueOf(cache));

            return out.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompressionStats stats = (CompressionStats) o;
            return blockSize == stats.blockSize && Objects.equals(docIdsEncoder, stats.docIdsEncoder) && Objects.equals(termFreqEncoder, stats.termFreqEncoder) && scoring == stats.scoring;
        }

        @Override
        public int hashCode() {
            return Objects.hash(docIdsEncoder, termFreqEncoder, blockSize, scoring);
        }
    }

    private static final Path pathOfBuiltIndex = Constants.DATA_PATH.resolve("uncompressed_index");

    private static final Map<String, Encoder> docIdsEncoders = new HashMap<>();
    private static final Map<String, Encoder> termFreqEncoders = new HashMap<>();
    static {
        docIdsEncoders.put("EliasFano", new EliasFano(EncodingType.DOC_IDS));
        docIdsEncoders.put("PForDelta", new PForDelta(EncodingType.DOC_IDS));
        docIdsEncoders.put("Simple9",   new Simple9(EncodingType.DOC_IDS));
    }
    static {
        termFreqEncoders.put("Unary",     new UnaryEncoder(EncodingType.TERM_FREQUENCIES));
        termFreqEncoders.put("Simple9",   new Simple9(EncodingType.TERM_FREQUENCIES));
        termFreqEncoders.put("PForDelta", new PForDelta(EncodingType.TERM_FREQUENCIES));
    }

    private static final int[] BLOCK_SIZES = {10, 100, 1000, 5000, 10000, 50000};

    public static void main(String[] args) throws IOException {
        File statisticsFile = Constants.DATA_PATH.resolve("index_statistics_timing_3.csv").toFile(),
                tempFile = Constants.DATA_PATH.resolve("temp.csv").toFile();

        buildUncompressedIndex();

        // 1) Build multiple indexes with different compression combinations and different block sizes
        for (String docIdsEncoder : docIdsEncoders.keySet())
            for (String termFreqEncoder : termFreqEncoders.keySet())
                for (int blockSize : BLOCK_SIZES) {
                    CompressionStats stats = new CompressionStats();
                    stats.docIdsEncoder = docIdsEncoder;
                    stats.termFreqEncoder = termFreqEncoder;
                    stats.blockSize = blockSize;
                    stats.scoring = ScoringType.TFIDF;

                    buildIndex(stats);
                    writeStats(stats, tempFile);
                }

        // 2) Measure query time for each index
        for (String docIdsEncoder : docIdsEncoders.keySet())
            for (String termFreqEncoder : termFreqEncoders.keySet())
                for (int blockSize : BLOCK_SIZES) {
                    CompressionStats stats = new CompressionStats();
                    stats.docIdsEncoder = docIdsEncoder;
                    stats.termFreqEncoder = termFreqEncoder;
                    stats.blockSize = blockSize;
                    stats.scoring = ScoringType.TFIDF;

                    stats = readStats(stats, tempFile);
                    assert stats != null;

                    stats.cache = true;
                    analyzeIndexPerformances(stats);
                    writeStats(stats, statisticsFile);

                    stats.cache = false;
                    analyzeIndexPerformances(stats);
                    writeStats(stats, statisticsFile);
                }
        //tempFile.delete();
    }

    private static void buildUncompressedIndex() throws IOException {
        // If no index has been created, create one (uncompressed) for each scoring

        for (ScoringType scoringType : new ScoringType[]{ScoringType.TFIDF}) {
            if (!pathOfBuiltIndex.resolve(scoringType.toString()).toFile().exists()) {
                System.out.printf("Creating uncompressed index (%s)...\n", scoringType);
                File collectionFile = Constants.COLLECTION_FILE;

                DocumentStream stream = new DocumentStream(collectionFile);
                InMemoryIndex globalIndexer = new InMemoryIndex(CompressionType.BINARY);

                SPIMIIndex spimi = new SPIMIIndex(globalIndexer, stream);
                spimi.buildIndex(pathOfBuiltIndex.resolve(scoringType.toString()));
            }
        }
    }

    private static void buildIndex(CompressionStats stats) throws IOException {
        String indexFolder = stats.getIndexFolder();
        Path indexPath = Constants.DATA_PATH.resolve(indexFolder);
        Session.setScoring(stats.scoring);
        Constants.BLOCK_SIZE = stats.blockSize;

        long startTime = System.currentTimeMillis();

        // If index has already been created skip the creation
        boolean alreadyExists = indexPath.toFile().exists();
        if (!alreadyExists) {
            try (
                    BlockFetcherBinary blockFetcher = new BlockFetcherBinary();
                    DumperCompressed dumper = new DumperCompressed(docIdsEncoders.get(stats.docIdsEncoder), termFreqEncoders.get(stats.termFreqEncoder))
            ) {
                System.out.printf("Building index %5s %5d %10s %10s\n", stats.scoring, stats.blockSize, stats.docIdsEncoder, stats.termFreqEncoder);

                // Read the already built index and dump a new one with a new compression
                blockFetcher.start(pathOfBuiltIndex.resolve(stats.scoring.toString()));

                // Copy document index (it's the same for all indices)
                FileUtils.copyFile(
                        pathOfBuiltIndex.resolve(stats.scoring.toString()).resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile(),
                        indexPath.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()
                );

                dumper.start(indexPath);

                // Load one vocabulary entry at a time and dump it
                Map.Entry<String, VocabularyEntry> entry;
                while ((entry = blockFetcher.loadNextVocabularyEntry()) != null)
                    dumper.dumpVocabularyEntry(entry);
            }
            stats.buildTime = System.currentTimeMillis() - startTime;

            Path docIdsPath = indexPath.resolve(Constants.DOC_IDS_FILENAME);
            Path termFreqPath = indexPath.resolve(Constants.TERM_FREQ_FILENAME);
            stats.docIdsSize = Files.size(docIdsPath);
            stats.termFreqSize = Files.size(termFreqPath);
        }
    }

    private static void analyzeIndexPerformances(CompressionStats stats) throws IOException {
        String indexFolder = stats.getIndexFolder();
        Path indexPath = Constants.DATA_PATH.resolve(indexFolder);

        List<Double> times = new ArrayList<>(RUNS);
        for (int i = 0; i < RUNS; i++) {
            Constants.BLOCK_SIZE = stats.blockSize;
            Session.setScoring(stats.scoring);
            Session.setPath(indexPath);
            Encoder.setDocIdsEncoder(docIdsEncoders.get(stats.docIdsEncoder));
            Encoder.setTermFrequenciesEncoder(termFreqEncoders.get(stats.termFreqEncoder));
            //Session.start();

            times.add(Timing.TimeIT(stats.cache, false, 3));
        }

        // Compute average time
        double meanTime = 0;
        for (double t : times)
            meanTime += t;
        meanTime = meanTime / RUNS;

        // Compute variance
        double var = 0;
        for (double t : times)
            var += (meanTime - t) * (meanTime - t);
        var = Math.sqrt(var / RUNS);

        stats.scoreTime = meanTime;
        stats.runs = RUNS;
        stats.variance = var;
    }

    private static void writeStats(CompressionStats stats, File csvOutputFile) throws IOException {
        String header = "";
        if (!csvOutputFile.exists())
            header = "doc_ids_compression,tf_compression,block_size,scoring,doc_ids_size,tf_size,build_time,avg_query_time,runs,variance,cache\n";

        try (
                PrintWriter pw = new PrintWriter(new FileOutputStream(csvOutputFile, true))
        ) {
            pw.print(header);
            pw.println(stats);
            System.out.println(header + stats);
        }
    }

    private static CompressionStats readStats(CompressionStats stats, File csvInputFile) throws IOException {
        if (!csvInputFile.exists())
            return null;

        try (
                BufferedReader reader = new BufferedReader(new FileReader(csvInputFile))
        ) {
            // Skip header
            reader.readLine();

            CompressionStats newStats;
            String line;
            while ( (line = reader.readLine()) != null ) {
                newStats = CompressionStats.parseTXT(line);
                if (newStats.equals(stats))
                    return newStats;
            }
        }

        return null;
    }
}
