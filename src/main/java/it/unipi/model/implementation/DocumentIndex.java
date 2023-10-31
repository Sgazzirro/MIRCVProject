package it.unipi.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.unipi.model.DocumentIndexInterface;

import java.io.Serializable;
import java.util.HashMap;

public class DocumentIndex implements DocumentIndexInterface, Serializable {
    @JsonProperty("totalLength")
    private int totalLength;
    @JsonProperty("numDocuments")
    private int numDocuments;
    @JsonProperty("docIndexTable")
    private HashMap<Integer, DocumentIndexEntry> table;

    public DocumentIndex(){
        table = new HashMap<>();
        numDocuments = 0;
        totalLength = 0;
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
            DocumentIndexEntry die = new DocumentIndexEntry();
            die.setDocumentLength(docLength);
            table.put(docId, die);
            numDocuments +=1;
            totalLength += docLength;
            return true;
        }
    }
    @Override
    public String toString(){
        return "TotalLength: "+totalLength+" NumDocuments: "+numDocuments+" DocumentIndexTable: "+table.toString();
    }
}
