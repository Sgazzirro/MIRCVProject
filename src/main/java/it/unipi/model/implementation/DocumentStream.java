package it.unipi.model.implementation;
import it.unipi.model.DocumentStreamInterface;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class DocumentStream implements DocumentStreamInterface {
    private BufferedReader br;
    boolean firstLine=true;

    public DocumentStream(String filename){
        try{
            FileInputStream fileInput = new FileInputStream(filename);
            GZIPInputStream gzipInput = new GZIPInputStream(fileInput);
            InputStreamReader reader = new InputStreamReader(gzipInput);
            br = new BufferedReader(reader);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public Document nextDoc() {
        Document doc = new Document();
        try{
            String line = br.readLine();
            if (line==null){
                return null;
            }
            String[] data = line.split("\t"); // Split the line into fields using the tab character
            if(firstLine){
                doc.setId(0);
                firstLine=false;
            }
            else doc.setId(Integer.parseInt(data[0]));
            doc.setText(data[1]);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }
}

