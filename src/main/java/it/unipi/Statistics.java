package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.io.implementation.FetcherCompressed;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.scoring.ScoringType;
import it.unipi.utils.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Statistics {

    public static List<Integer> topPostingLists(List<Integer> dfs){
        dfs.sort(Collections.reverseOrder());
        System.out.println("TOP 10 SIZES : " + List.of(dfs.subList(0, 10)));
        return dfs;
    }

    public static void filterAndAvg(List<Integer> dfs, int threshold){
        List<Integer> filteredList = dfs.stream()
                .filter(element -> element >= threshold)
                .toList(); // Use toList() to collect the elements into a new list

        int sumF = filteredList.stream().mapToInt(Integer::intValue).sum();
        System.out.println("NUMBER OF LISTS WITHOUT CONSIDERING TERMS WITH LESS THAN " +  threshold + "  POSTINGS: " + filteredList.size());
        System.out.println("AVERAGE POSTING LISTS SIZE WITHOUT CONSIDERING TERMS WITH LESS THAN " + threshold + " POSTINGS " + (double) (sumF / filteredList.size()));
        System.out.println();
    }

    public static double calculateVariance(List<Integer> numbers) {
        // Step 1: Calculate the mean
        double mean = numbers.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        // Step 2: Calculate the squared differences
        double squaredDifferencesSum = numbers.stream()
                .mapToDouble(number -> Math.pow(number - mean, 2))
                .sum();

        // Step 3: Calculate the variance
        double variance = squaredDifferencesSum / numbers.size();

        return variance;
    }
    public static void main(String[] args) throws IOException {
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Path.of("./data"));
        Constants.setScoring(ScoringType.TFIDF);

        FetcherCompressed fetcher = new FetcherCompressed();
        fetcher.start(Constants.getPath());
        int[] docIndexInformations = fetcher.getDocumentIndexStats();
        System.out.println("AVERAGE LEN OF DOCUMENTS: "  + (docIndexInformations[1] / docIndexInformations[0]));
        ArrayList<Integer> collectionFrequencies = new ArrayList<>();
        while(true){
            Map.Entry<Integer, DocumentIndexEntry> entryDOC = fetcher.loadDocEntry();
            if(entryDOC == null)
                break;

            collectionFrequencies.add(entryDOC.getValue().getDocumentLength());
        }
        fetcher.close();
        /*
        filterAndAvg(collectionFrequencies, 0);
        filterAndAvg(collectionFrequencies, 5);
        filterAndAvg(collectionFrequencies, 100);
        filterAndAvg(collectionFrequencies, 10000);
        filterAndAvg(collectionFrequencies, 100000);

         */
        System.out.println("VARIANCE OF DOCUMENT LENGTHS : " + calculateVariance(collectionFrequencies));
        /*
        List<Integer> ordered = topPostingLists(collectionFrequencies);

        try(
                BufferedWriter writer = new BufferedWriter(new FileWriter("length.txt"));
                ) {
            for(Integer a: ordered){
                writer.write(String.valueOf(a) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

         */
    }
}
