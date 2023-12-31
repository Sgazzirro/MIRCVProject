package it.unipi.model;

public class DocumentIndexEntry {
    private int docNo;
    private int documentLength;
    public int getDocumentLength() {
        return documentLength;
    }
    public int getDocNo(){
        return docNo;
    }
    public void setDocNo(int docNo){
        this.docNo=docNo;
    }
    public void setDocumentLength(int documentLength) {
        this.documentLength = documentLength;
    }
    public static DocumentIndexEntry parseTXT(String line) {
        DocumentIndexEntry entry = new DocumentIndexEntry();
        String[] params = line.split(",");

        int docLength = Integer.parseInt(params[2]);
        int docNo = Integer.parseInt(params[1]);

        entry.setDocumentLength(docLength);
        entry.setDocNo(docNo);
        return entry;
    }

    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentIndexEntry that = (DocumentIndexEntry) o;
        return that.getDocumentLength() == this.getDocumentLength();
    }
}
