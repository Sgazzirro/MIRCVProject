package it.unipi.io.implementation;
import it.unipi.io.DocumentStream;
import it.unipi.model.Document;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class DocumentStreamImpl implements DocumentStream {

    private BufferedReader br;
    private boolean firstLine = true;

    private String filename;

    public DocumentStreamImpl(String filename) {
        this.filename = filename;
        reset();
    }

    private void reset() {
        try {
            FileInputStream fileInput = new FileInputStream(filename);

            // Check if file is compressed
            InputStream inputStream = fileInput;
            if (filename.endsWith(".tar.gz"))
                inputStream = new GZIPInputStream(fileInput);

            // Forces Unicode decoding (it should be on by default)
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
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

            // Used to prevent incorrect reading of the first document id
            if (firstLine) {
                doc.setId(0);
                firstLine = false;
            } else {
                try {
                    doc.setId(Integer.parseInt(data[0]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            doc.setText(data[1]);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return doc;
    }

    @Override
    public Document getDoc(int docId) {
        reset();

        try {
            while (docId > 0) {
                br.readLine();
                docId--;
            }
            return nextDoc();
        } catch (IOException ignored) { }

        return null;
    }
}

