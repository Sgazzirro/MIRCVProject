package it.unipi.io.implementation;

import it.unipi.io.Fetcher;
import it.unipi.model.DocumentIndexEntry;
import it.unipi.model.VocabularyEntry;
import it.unipi.utils.Constants;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

public class FetcherTXT implements Fetcher {

    BufferedReader globalReaderVOC;
    BufferedReader globalReaderDOC;
    boolean opened = false;
    Path path;

    @Override
    public boolean start(Path path) {
        try{
            if (opened)
                throw new IOException();


            globalReaderVOC = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
            globalReaderDOC = new BufferedReader(new FileReader(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));

            opened = true;
            this.path = path;
        }
        catch(IOException ie){
            System.out.println("Error in opening the file");
            opened =  false;
        }
        return true;
    }

    @Override
    public void loadPosting(VocabularyEntry entry) {
        // The loading of a posting list uses two inner buffers

        long[] offsets = new long[]{entry.getDocIdsOffset(), entry.getTermFreqOffset()};
        // In the TXT case, the length is the number of postings
        int len = entry.getDocIdsLength();

        try (
                BufferedReader readerIds = new BufferedReader(new FileReader(path.resolve(Constants.DOC_IDS_POSTING_FILENAME).toFile()));
                BufferedReader readerTf = new BufferedReader(new FileReader(path.resolve(Constants.TF_POSTING_FILENAME).toFile()))
        ) {
            // Set the readers to the correct offsets (computed before)
            readerIds.skip(offsets[0]);
            readerTf.skip(offsets[1]);
            for (int i = 0; i < len; i++)
                entry.getPostingList().addPosting(Integer.parseInt(readerIds.readLine()), Integer.parseInt(readerTf.readLine()));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public VocabularyEntry loadVocEntry(String term) {
        // Use the global reader and restart it if EOF reached
        VocabularyEntry result;
        String  line;
        if (!opened)
            return null;

        try {
            // In the TXT version, no binary search has been implemented
            // Anyway, below we propose a slight optimization of the search based on the previous one

            // CASES AT FIRST READ
            // - line = null EOF reached, restart and parse the whole file
            // - line != null and comparison < 0, you have to restart and search until you come back to that term
            // - line != null and comparison > 0, you have to search until EOF (do nothing)
            line = globalReaderVOC.readLine();
            String toReach = null;
            if(line == null) {
                // CASE 1
                globalReaderVOC.close();
                globalReaderVOC = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
                line = globalReaderVOC.readLine();
            }
            else {
                int comparison = term.compareTo(line.split(",")[0]);
                if(comparison < 0) {
                    // CASE 2
                    toReach = line.split(",")[0];
                    globalReaderVOC.close();
                    globalReaderVOC = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
                    line = globalReaderVOC.readLine();
                }
            }
            do{
                String[] params = line.split(",");

                if(toReach != null && params[0].equals(toReach))
                    return null;

                if (params[0].equals(term)) {
                    result = VocabularyEntry.parseTXT(line);
                    loadPosting(result);
                    return result;
                }
                line = globalReaderVOC.readLine();
            }while(line != null);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() {
        // The loading of an entry without arguments uses the global reader
        VocabularyEntry result;
        String term;
        if (!opened)
            return null;


        try {
            // Read next line
            String line = globalReaderVOC.readLine();
            if (line == null)
                return null;
            String[] params = line.split(",");
            term = params[0];
            result = VocabularyEntry.parseTXT(line);
            loadPosting(result);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return Map.entry(term, result);
    }

    @Override
    public DocumentIndexEntry loadDocEntry(long docId) {
        int firstDocId = -1;
        String line;

        // Use the global reader and restart it if EOF reached
        if (!opened)
                return null;


        try {
            // CASES AT FIRST READ
            // - line = null EOF reached, restart and parse the whole file
            // - line != null and comparison < 0, you have to restart and search until you come back to that term
            // - line != null and comparison > 0, you have to search until EOF (do nothing)
            line = globalReaderDOC.readLine();
            Long toReach = null;
            if(line == null) {
                // CASE 1
                globalReaderDOC.close();
                globalReaderDOC = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
                line = globalReaderDOC.readLine();
            }
            else {
                boolean comparison = docId < Long.parseLong(line.split(",")[0]);
                if(comparison) {
                    // CASE 2
                    toReach = Long.parseLong(line.split(",")[0]);
                    globalReaderDOC.close();
                    globalReaderDOC = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
                    line = globalReaderDOC.readLine();
                }
            }

            do{
                String[] params = line.split(",");

                if(toReach != null && Long.parseLong(params[0]) == toReach)
                    return null;

                if (Long.parseLong(params[0]) == docId) {
                    return DocumentIndexEntry.parseTXT(line);
                }
                line = globalReaderDOC.readLine();
            }while(line != null);

        } catch(IOException e){
            e.printStackTrace();
        }
            return null;
        }


        @Override
        public Map.Entry<Integer, DocumentIndexEntry> loadDocEntry () {
            // The loading of an entry without arguments uses the global reader
            DocumentIndexEntry result;

            int docId;
            if (!opened)
                return null;

            try {
                // Read next line
                String line = globalReaderDOC.readLine();
                if (line == null)
                    return null;
                String[] params = line.split(",");
                docId = Integer.parseInt(params[0]);
                result = DocumentIndexEntry.parseTXT(line);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return Map.entry(docId, result);
        }


    @Override
    public boolean end() {
        try {
            if (!opened)
                throw new IOException();
            globalReaderVOC.close();
            globalReaderDOC.close();
            opened = false;
        } catch (IOException ie) {
            System.out.println("Error in opening the file");
            return false;
        }
        return opened;
    }

    @Override
    public int[] getDocumentIndexStats() {
        int N, l;
        try {
            l = Integer.parseInt(globalReaderDOC.readLine());
            N = Integer.parseInt(globalReaderDOC.readLine());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new int[] {N, l};
    }
}
