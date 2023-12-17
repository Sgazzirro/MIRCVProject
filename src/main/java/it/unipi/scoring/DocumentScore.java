package it.unipi.scoring;

public class DocumentScore implements Comparable<DocumentScore>{
    public int docId;
    public double score;

    public DocumentScore(int docId, double score){
        this.docId = docId;
        this.score = score;
    }

    @Override
    public int compareTo(DocumentScore o) {
        // Reverse order so documents with higher score are first
        return Double.compare(score, o.score);
    }

    @Override
    public String toString() {
        return "{docId=" + docId +
                ", score=" + String.format("%.4f", score) +
                '}';
    }
}
