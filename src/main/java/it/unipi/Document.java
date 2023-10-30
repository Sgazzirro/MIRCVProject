package it.unipi;

import it.unipi.model.DocumentInterface;

public class Document implements DocumentInterface {
    private int docid;
    private String text;

    @Override
    public int getId() {
        return docid;
    }

    @Override
    public void setId(int docid) {
        this.docid=docid;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text=text;
    }
}
