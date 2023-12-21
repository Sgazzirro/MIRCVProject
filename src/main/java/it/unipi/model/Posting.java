package it.unipi.model;

public class Posting implements Comparable<Posting> {

    private int tf;
    private int docId;

    public Posting(int tf, int docId) {
        this.tf = tf;
        this.docId = docId;
    }

    public int getTf() {
        return tf;
    }

    public int getDocId() {
        return docId;
    }

    @Override
    public int compareTo(Posting other) {
        return Integer.compare(this.tf, other.tf);
    }
}
