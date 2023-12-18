package it.unipi.encoding;

import it.unipi.encoding.implementation.TokenizerImpl;

import java.util.List;

public interface Tokenizer {
    /*
        A collection of methods for tokenizing some content
     */

    List<String> tokenizeBySpace(String content);

    static Tokenizer getInstance() {
        return getInstance(true, true);
    }

    static Tokenizer getInstance(boolean applyStemming, boolean removeStopwords) {
        return new TokenizerImpl(applyStemming, removeStopwords);
    }
}
