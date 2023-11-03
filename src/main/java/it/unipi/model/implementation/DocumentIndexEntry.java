package it.unipi.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class DocumentIndexEntry implements Serializable {

    // maybe there will be something to add inside here
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentIndexEntry that = (DocumentIndexEntry) o;
        return documentLength == that.documentLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentLength);
    }
}
