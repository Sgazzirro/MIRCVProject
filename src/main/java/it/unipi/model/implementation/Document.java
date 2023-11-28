package it.unipi.model.implementation;

public class Document {
    private int docid;
    private String text;

    public Document(){

    }
    public Document(String text){
        String[] params = text.split("\t");
        setId(Integer.parseInt(params[0]));
        setText(params[1]);
    }
    public int getId() {
        return docid;
    }


    public void setId(int docid) {
        this.docid=docid;
    }


    public String getText() {
        return text;
    }


    public void setText(String text) {
        this.text=text;
    }
}
