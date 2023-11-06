package it.unipi.model.implementation;

import it.unipi.model.TokenizerInterface;
import it.unipi.utils.Constants;
import it.unipi.utils.LoadStructures;
import opennlp.tools.stemmer.PorterStemmer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Tokenizer implements TokenizerInterface {

    List<String> stopwords = LoadStructures.loadStopwords(Constants.STOPWORDS_FILE);
    // Use Porter stemmer
    PorterStemmer stemmer = new PorterStemmer();

    private final boolean applyStemming;

    public Tokenizer() {
        this(true);
    }

    public Tokenizer(boolean applyStemming) {
        this.applyStemming = applyStemming;
    }

    @Override
    public List<String> tokenizeBySpace(String content) {
        // TODO - Handling char[] instead of String should be a lot faster
        // Remove HTML tags
        String cleanedHTML = content.replaceAll("<[^>]+>", "");
        // Remove punctuation
        String removedPunctuation = cleanedHTML.replaceAll("[\\p{Punct}]", " ");

        Stream<String> stream = Arrays.stream(removedPunctuation.toLowerCase().split("\\s+"))
                .filter(s -> !stopwords.contains(s));
        if (applyStemming)
            stream = stream.map(s -> stemmer.stem(s));

        return stream.collect(Collectors.toList());
    }
}
