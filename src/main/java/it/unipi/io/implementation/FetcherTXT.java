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

            // System.out.println("TRYING TO OPEN" + path);
            globalReaderVOC = new BufferedReader(new FileReader(path.resolve(Constants.VOCABULARY_FILENAME).toFile()));
            globalReaderDOC = new BufferedReader(new FileReader(path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile()));

            opened = true;
            this.path = path;
        }
        catch(IOException ie){
            System.out.println("Error in opening the file");
            return false;
        }
        return true;
    }

    @Override
    public void loadPosting(VocabularyEntry entry) {
        // The loading of a posting list uses two inner buffers

        long[] offsets = new long[]{entry.getDocIdsOffset(), entry.getTermFreqOffset()};
        int len = entry.getDocIdsLength();

        try (
                BufferedReader readerIds = new BufferedReader(new FileReader(path.resolve(Constants.DOC_IDS_POSTING_FILENAME).toFile()));
                BufferedReader readerTf = new BufferedReader(new FileReader(path.resolve(Constants.TF_POSTING_FILENAME).toFile()))
        ) {
            readerIds.skip(offsets[0]);
            readerTf.skip(offsets[1]);
            for (int i = 0; i < len; i++)
                entry.getPostingList().addPosting(Integer.parseInt(readerIds.readLine()), Integer.parseInt(readerTf.readLine()));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // TODO - Should we use this method or the inner method in PostingList???
        //list.loadPosting(path + Constants.DOC_IDS_POSTING_FILENAME, path + Constants.TF_POSTING_FILENAME);
    }

    public VocabularyEntry loadVocEntry(String term) {
        // Use the global reader and restart it if EOF reached
        VocabularyEntry result;
        String firstTerm = null, line;
        if (!opened) {
            if (!start(path))
                return null;
        }

        try {
            while (true) {
                line = globalReaderVOC.readLine();
                if (line == null) {
                    // EOF reached, restart buffer
                    globalReaderVOC = new BufferedReader(new FileReader(path + Constants.VOCABULARY_FILENAME));
                    line = globalReaderVOC.readLine();
                }

                String[] params = line.split(",");
                if (firstTerm == null)
                    // Remember first term read so to stop reading again the whole file
                    firstTerm = params[0];
                else if (params[0].equals(firstTerm))
                    // We are at the starting point after reading the whole file, stop
                    return null;

                if (params[0].equals(term)) {
                    result = VocabularyEntry.parseTXT(line);
                    loadPosting(result);
                    return result;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map.Entry<String, VocabularyEntry> loadVocEntry() {
        // The loading of an entry without arguments uses the global reader
        VocabularyEntry result;
        String term;
        if (!opened)
            if (!start(path))
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
            throw new RuntimeException(e);
        }
        return Map.entry(term, result);
    }

    @Override
    public DocumentIndexEntry loadDocEntry(long docId) {
        int firstDocId = -1;
        String line;

        // Use the global reader and restart it if EOF reached
        if (!opened)
            if (!start(path))
                return null;


        try {
            while (true) {
                line = globalReaderDOC.readLine();
                if (line == null) {
                    // EOF reached, restart buffer
                    globalReaderDOC = new BufferedReader(new FileReader(path + Constants.DOCUMENT_INDEX_FILENAME));
                    line = globalReaderDOC.readLine();
                }

                String[] params = line.split(",");
                int currentDocId = Integer.parseInt(params[0]);
                if (firstDocId == -1)
                    // Remember first term read so to stop reading again the whole file
                    firstDocId = currentDocId;
                else if (currentDocId == firstDocId)
                    // We are at the starting point after reading the whole file, stop
                    return null;

                if (currentDocId == docId)
                    return DocumentIndexEntry.parseTXT(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map.Entry<Integer, DocumentIndexEntry> loadDocEntry() {
        // The loading of an entry without arguments uses the global reader
        DocumentIndexEntry result;

        int docId;
        if (!opened)
            if (!start(path))
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
        return true;
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
