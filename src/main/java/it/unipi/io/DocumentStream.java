package it.unipi.io;


import it.unipi.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class DocumentStream {

    protected static final Logger logger = LoggerFactory.getLogger(DocumentStream.class);

    private BufferedReader reader;

    private final File file;

    private final int startDocId;   // docId of the first document to be processed     (0 by default)
    private final int endDocId;     // docId of the first document not to be processed (-1 by default -> EOF)
    private int offset;

    public DocumentStream(File file, int startDocId, int endDocId) throws IOException {
        this.file = file;
        this.startDocId = startDocId;
        this.endDocId = (endDocId >= 0) ? endDocId : Integer.MAX_VALUE;
        reset();
    }

    public DocumentStream(File file) throws IOException {
        this(file, 0, -1);
    }

    private void reset() throws IOException {
        FileInputStream fileInput = new FileInputStream(file);

        // Check if file is compressed
        InputStream inputStream = fileInput;
        if (file.getName().endsWith(".tar.gz")) {
            logger.info("Compressed file " + file + " correctly decompressed");
            inputStream = new GZIPInputStream(fileInput);
        }

        // Forces Unicode decoding (it should be on by default)
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        this.reader = new BufferedReader(reader);

        // Set document stream start to startDocId
        offset = 0;
        while (offset < startDocId) {
            this.reader.readLine();
            offset++;
        }

        logger.info("Stream correctly initialized");
    }

    public Document nextDoc() throws IOException {
        // Check if we are at the end of the stream
        if (offset == endDocId)
            return null;

        Document doc = new Document();

        String line = reader.readLine();
        if (line == null)
            return null;
        String[] data = line.split("\t"); // Split the line into fields using the tab character

        if (offset == 0)
            doc.setId(0);
        else {
            try {
                doc.setId(Integer.parseInt(data[0]));
            } catch(NumberFormatException nfe){ // end of file reached
                return null;
            }
        }
        doc.setText(data[1]);

        offset++;
        return doc;
    }

    public Document getDoc(int docId) throws IOException {
        reset();

        while (docId > 0) {
            reader.readLine();
            offset++;
            docId--;
        }
        return nextDoc();
    }
}