package it.unipi.utils;

import it.unipi.model.PostingList;
import it.unipi.model.implementation.DocumentIndexEntry;
import it.unipi.model.implementation.VocabularyEntry;

import java.io.*;
import java.util.Map;

public class FetcherTXT implements Fetcher{
    BufferedReader globalReader;
    boolean opened = false;
    String prefix;

    public void setScope(String filename){
        prefix = filename;
    }
    @Override
    public boolean start(String filename) {
        try{
            if(opened)
                throw new IOException();
            System.out.println("TRYING TO OPEN" + filename);
            globalReader = new BufferedReader(new FileReader(filename + "vocabulary.csv"));
            opened = true;
            prefix = filename;
        }
        catch(IOException ie){
            System.out.println("Error in opening the file");
            return false;
        }
        return true;
    }

    public void loadPosting(PostingList list){
        loadPosting(list, "");
    }


    public void loadPosting(PostingList list, String prefix) {
        // The loading of a posting list uses two inner buffers


        long[] offsets = new long[]{list.getOffsetID(), list.getOffsetTF()};
        int len = list.getLength();

        try(
                BufferedReader readerIds = new BufferedReader(new FileReader(prefix + "doc_ids.txt"));
                BufferedReader readerTf = new BufferedReader(new FileReader(prefix + "term_frequencies.txt"));
                )
        {
            readerIds.skip(offsets[0]);
            readerTf.skip(offsets[1]);
            for(int i = 0; i < len; i++){
                list.addPosting(Integer.parseInt(readerIds.readLine()), Integer.parseInt(readerTf.readLine()));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map.Entry<String, VocabularyEntry> loadVocEntry(String term){
        // TODO
        return null;
    }
    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() {
        // The loading of an entry without arguments uses the global reader
        VocabularyEntry result;
        String term;
        if(!opened)
            if(!start(prefix + "vocabulary.csv"))
                return null;
        try {
            // Read next line
            String line = globalReader.readLine();
            if(line == null)
                return null;
            String[] params = line.split(",");
            term = params[0];
            result = new VocabularyEntry(params);
            loadPosting(result.getPostingList(), prefix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Map.entry(term, result);
    }

    @Override
    public DocumentIndexEntry loadDocEntry(long docId) {
        return null;
    }

    @Override
    public boolean end() {
        try{
            if(!opened)
                throw new IOException();
            globalReader.close();
            opened = false;
        }
        catch(IOException ie){
            System.out.println("Error in opening the file");
            return false;
        }
        return true;
    }
}
