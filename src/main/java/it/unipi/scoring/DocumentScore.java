package it.unipi.scoring;

import java.util.Objects;

public record DocumentScore(int docId, double score) implements Comparable<DocumentScore> {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentScore that = (DocumentScore) o;
        return docId == that.docId ||
                Math.abs(score - that.score) < 1e-10;
    }

    @Override
    public int hashCode() {
        return Objects.hash(score);
    }
}
