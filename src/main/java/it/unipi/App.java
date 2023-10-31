package it.unipi;

import it.unipi.model.implementation.Document;
import it.unipi.model.implementation.DocumentStream;
import it.unipi.model.implementation.Tokenizer;

import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DocumentStream ds = new DocumentStream("data/reduced_collection.tar.gz");

        Document doc = ds.nextDoc();
        System.out.println(doc.getText());
        Tokenizer tok = new Tokenizer();
        List<String> tokenized = tok.tokenizeBySpace(doc.getText());
        System.out.println(tokenized);

    }
}
