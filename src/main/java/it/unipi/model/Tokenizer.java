package it.unipi.model;

import java.util.List;

public interface Tokenizer {
    /*
        A collection of methods for tokenizing some content
     */

    public List<String> tokenizeBySpace(String content);
}
