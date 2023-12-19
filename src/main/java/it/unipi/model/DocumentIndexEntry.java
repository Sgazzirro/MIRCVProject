package it.unipi.model;

import it.unipi.encoding.CompressionType;

public class DocumentIndexEntry {

    private int documentLength;

    public int getDocumentLength() {
        return documentLength;
    }

    public void setDocumentLength(int documentLength) {
        this.documentLength = documentLength;
    }

    public static DocumentIndexEntry parseTXT(String line) {
        DocumentIndexEntry entry = new DocumentIndexEntry();
        String[] params = line.split(",");

        int docLength = Integer.parseInt(params[1]);

        entry.setDocumentLength(docLength);
        return entry;
    }
}
