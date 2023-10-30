package it.unipi;

import it.unipi.model.implementation.Document;
import it.unipi.model.implementation.DocumentStream;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DocumentStream ds = new DocumentStream("data/reduced_collection.tar.gz");
        for(int i=0; i<5; i++){
            Document doc = ds.nextDoc();
            System.out.println(doc.getId());
            System.out.println(doc.getText());
        }

    }
}
