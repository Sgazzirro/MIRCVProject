package it.unipi.model.implementation;

import it.unipi.model.TokenizerInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tokenizer implements TokenizerInterface {
    @Override
    public List<String> tokenizeBySpace(String content) {
        // TODO - Handling char[] instead of String should be a lot faster
        // Remove HTML tags
        String cleanedHTML = content.replaceAll("<[^>]+>", "");
        // Remove punctuation
        String removedPunctuation = cleanedHTML.replaceAll("[\\p{Punct}]", " ");

        return new ArrayList<>(Arrays.asList(removedPunctuation.toLowerCase().split("\\s+")));
    }
}
