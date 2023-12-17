package it.unipi.encoding;

import java.util.List;

public interface Tokenizer {
    /*
        A collection of methods for tokenizing some content
     */

    public List<String> tokenizeBySpace(String content);
}
