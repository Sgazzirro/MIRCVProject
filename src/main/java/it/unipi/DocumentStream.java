package it.unipi;

import it.unipi.model.DocumentStreamInterface;

public class DocumentStream implements DocumentStreamInterface {

    @Override
    public String[] nextDoc() {
        return new String[0];
    }

    @Override
    public boolean hasNext() {
        return false;
    }
}
