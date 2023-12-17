package it.unipi.model.implementation;

import it.unipi.model.DocumentIndexEntry;

import java.util.Objects;

public class DocumentIndexEntryImpl implements DocumentIndexEntry {

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
        return documentLength == that.getDocumentLength();
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentLength);
    }
}
