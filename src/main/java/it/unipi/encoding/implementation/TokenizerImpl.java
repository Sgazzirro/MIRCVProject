package it.unipi.encoding.implementation;

import it.unipi.encoding.Tokenizer;
import it.unipi.utils.Constants;
import it.unipi.utils.IOUtils;
import opennlp.tools.stemmer.PorterStemmer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TokenizerImpl implements Tokenizer {

    // Use Porter stemmer
    PorterStemmer stemmer;

    private final boolean applyStemming;
    private final boolean removeStopwords;

    public TokenizerImpl(boolean applyStemming, boolean removeStopwords) {
        this.applyStemming = applyStemming;
        this.removeStopwords = removeStopwords;

        if (applyStemming)
            this.stemmer = new PorterStemmer();
    }

    @Override
    public List<String> tokenizeBySpace(String content) {
        // TODO - Handling char[] instead of String should be a lot faster
        // Remove HTML tags
        String cleanedHTML = content.replaceAll("<[^>]+>", "");
        // Remove punctuation
        String removedPunctuation = cleanedHTML.replaceAll("[\\p{Punct}]", " ");

        Stream<String> stream = Arrays.stream(removedPunctuation.toLowerCase().split("\\s+"));
        if (removeStopwords)
            stream = stream.filter(s -> !Constants.STOPWORDS.contains(s));
        if (applyStemming)
            stream = stream.map(s -> stemmer.stem(s));

        return stream.collect(Collectors.toList());
    }
}
