package it.unipi.model.implementation;

import it.unipi.model.TokenizerInterface;

import java.util.Arrays;
import java.util.List;

public class Tokenizer implements TokenizerInterface {
    @Override
    public List<String> tokenizeBySpace(String content) {
        String cleanedHTML = content.replaceAll("<[^>]+>", "");
        String removedPunctuation = cleanedHTML.replaceAll("[\\p{Punct}]", " ");
        return Arrays.asList(removedPunctuation.toLowerCase().split("\\s+"));
    }
}
