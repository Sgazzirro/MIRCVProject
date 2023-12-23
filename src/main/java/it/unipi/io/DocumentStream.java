package it.unipi.io;


import it.unipi.model.Document;
import it.unipi.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class DocumentStream {

    protected static final Logger logger = LoggerFactory.getLogger(DocumentStream.class);

    private BufferedReader br;

    private final String filename;

    private final int startDocId;   // docId of the first document to be processed     (0 by default)
    private final int endDocId;     // docId of the first document not to be processed (-1 by default -> EOF)
    private int offset;

    public DocumentStream(String filename, int startDocId, int endDocId) throws IOException {
        this.filename = filename;
        this.startDocId = startDocId;
        this.endDocId = (endDocId >= 0) ? endDocId : Integer.MAX_VALUE;
        reset();
    }

    public DocumentStream(String filename) throws IOException {
        this(filename, 0, -1);
    }

    public DocumentStream() throws IOException {
        this(Constants.COLLECTION_FILE, 0, -1);
    }

    private void reset() throws IOException {
        FileInputStream fileInput = new FileInputStream(filename);

        // Check if file is compressed
        InputStream inputStream = fileInput;
        if (filename.endsWith(".tar.gz")) {
            logger.info("Compressed file " + filename + " correctly decompressed");
            inputStream = new GZIPInputStream(fileInput);
        }

        // Forces Unicode decoding (it should be on by default)
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        br = new BufferedReader(reader);

        // Set document stream start to startDocId
        offset = 0;
        while (offset < startDocId) {
            br.readLine();
            offset++;
        }

        logger.info("Stream correctly initialized");
    }

    public Document nextDoc() throws IOException {
        // Check if we are at the end of the stream
        if (offset == endDocId)
            return null;

        Document doc = new Document();

        String line = br.readLine();
        if (line == null)
            return null;
        String[] data = line.split("\t"); // Split the line into fields using the tab character

        if (offset == 0)
            doc.setId(0);
        else
            doc.setId(Integer.parseInt(data[0]));
        doc.setText(data[1]);

        offset++;
        return doc;
    }

    public Document getDoc(int docId) throws IOException {
        reset();

        while (docId > 0) {
            br.readLine();
            offset++;
            docId--;
        }
        return nextDoc();
    }
}