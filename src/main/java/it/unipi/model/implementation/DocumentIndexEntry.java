package it.unipi.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class DocumentIndexEntry implements Serializable {
    // maybe there will be something to add inside here
    @JsonProperty("docLength")
    private int documentLength;

    public int getDocumentLength() {
        return documentLength;
    }

    public void setDocumentLength(int documentLength) {
        this.documentLength = documentLength;
    }

    @Override
    public String toString(){
        return String.valueOf(documentLength);
    }
}
