package it.unipi.encoding;

import it.unipi.utils.Constants;
import opennlp.tools.stemmer.PorterStemmer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Tokenizer {

    private static Tokenizer instance;

    // Use Porter stemmer
    private PorterStemmer stemmer;

    private final boolean applyStemming;
    private final boolean removeStopwords;

    private Tokenizer(boolean applyStemming, boolean removeStopwords) {
        this.applyStemming = applyStemming;
        this.removeStopwords = removeStopwords;

        if (applyStemming)
            this.stemmer = new PorterStemmer();
    }

    public List<String> tokenize(String content) {
        // Remove HTML tags
        String cleanedHTML = content.replaceAll("<[^>]+>", "");
        // Remove punctuation
        String removedPunctuation = cleanedHTML.replaceAll("\\p{Punct}", " ");

        Stream<String> stream = Arrays.stream(removedPunctuation.toLowerCase().split("\\s+"));
        if (removeStopwords)
            stream = stream.filter(s -> !Constants.STOPWORDS.contains(s));
        if (applyStemming)
            stream = stream.map(s -> stemmer.stem(s));

        return stream.collect(Collectors.toList());
    }

    public static Tokenizer getInstance() {
        return getInstance(true, true);
    }

    public static Tokenizer getInstance(boolean applyStemming, boolean removeStopwords) {
        if (instance == null || instance.applyStemming != applyStemming || instance.removeStopwords != removeStopwords)
            instance = new Tokenizer(applyStemming, removeStopwords);

        return instance;
    }
}
