package it.unipi.model.implementation;

import it.unipi.model.DocumentIndex;
import it.unipi.model.DocumentIndexEntry;

import java.io.Serializable;
import java.util.*;

public class DocumentIndexImpl implements DocumentIndex, Serializable {

    private int totalLength;
    private int numDocuments;
    private NavigableMap<Integer, DocumentIndexEntry> table;

    public DocumentIndexImpl(){
        table = new TreeMap<>();
        numDocuments = 0;
        totalLength = 0;
    }

    @Override
    public int getTotalLength() {
        return totalLength;
    }

    @Override
    public int getNumDocuments() {
        return numDocuments;
    }

    public void setTotalLength(int totalLength) {
        this.totalLength = totalLength;
    }

    public void setNumDocuments(int numDocuments) {
        this.numDocuments = numDocuments;
    }

    @Override
    public int getLength(int docId) {
        // returns -1 if the docId doesn't exist
        if (table.containsKey(docId)){
            return table.get(docId).getDocumentLength();
        }
        else return -1;
    }

    @Override
    public double getAverageLength() {
        return (double) totalLength/numDocuments;
    }

    @Override
    public boolean addDocument(int docId, int docLength) {
        if (table.containsKey(docId)){
            return false;
        }
        else{
            DocumentIndexEntry die = new DocumentIndexEntryImpl();
            die.setDocumentLength(docLength);
            table.put(docId, die);
            numDocuments +=1;
            totalLength += docLength;
            return true;
        }
    }




    public Iterable<Map.Entry<Integer, DocumentIndexEntry>> getEntries() {
        return table.entrySet();
    }

    @Override
    public String toString(){
        return "TotalLength: "+totalLength+" NumDocuments: "+numDocuments+" DocumentIndexTable: "+table.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentIndexImpl that = (DocumentIndexImpl) o;
        return totalLength == that.totalLength && numDocuments == that.numDocuments && Objects.equals(table.entrySet(), that.table.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalLength, numDocuments, table);
    }
}
