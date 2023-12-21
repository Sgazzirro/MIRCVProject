package it.unipi.io;


import it.unipi.model.Document;
import it.unipi.utils.Constants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class DocumentStream {

    private BufferedReader br;
    private boolean firstLine = true;

    private final String filename;

    private final int startDocId;   // docId of the first document to be processed     (0 by default)
    private final int endDocId;     // docId of the first document not to be processed (-1 by default -> EOF)
    private int offset;

    public DocumentStream(String filename, int startDocId, int endDocId) {
        this.filename = filename;
        this.startDocId = startDocId;
        this.endDocId = (endDocId >= 0) ? endDocId : Integer.MAX_VALUE;
        reset();
    }

    public DocumentStream(String filename) {
        this(filename, 0, -1);
    }

    public DocumentStream() {
        this(Constants.COLLECTION_FILE, 0, -1);
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

            // Set document stream start to startDocId
            offset = 0;
            while (offset < startDocId) {
                br.readLine();
                offset++;
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public Document nextDoc() {
        Document doc = new Document();
        try {
            // Check if we are at the end of the stream
            if (offset == endDocId)
                return null;

            String line = br.readLine();
            offset++;
            if (line == null)
                return null;
            String[] data = line.split("\t"); // Split the line into fields using the tab character

            doc.setId(Integer.parseInt(data[0]));
            doc.setText(data[1]);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return doc;
    }

    public Document getDoc(int docId) {
        reset();

        try {
            while (docId > 0) {
                br.readLine();
                firstLine = false;
                docId--;
            }
            return nextDoc();
        } catch (IOException ignored) { }

        return null;
    }
}