package it.unipi.model;

public class Document {

    private int docId;
    private String text;

    public Document() { }

    public Document(String text) {
        String[] params = text.split("\t");

        docId = Integer.parseInt(params[0]);
        this.text  = params[1];
    }

    public int getId() {
        return docId;
    }

    public void setId(int docId) {
        this.docId = docId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text=text;
    }
}
