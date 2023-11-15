package it.unipi.utils;

import it.unipi.model.DocumentIndex;
import it.unipi.model.PostingList;
import it.unipi.model.Vocabulary;
import it.unipi.model.implementation.EliasFano;
import it.unipi.model.implementation.VocabularyEntry;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DumpCompressed implements Dumper{
    // elias fano
    private FileOutputStream fosDocIds;
    private DataOutputStream dosDocIds;

    private boolean opened = false;


    @Override
    public boolean start(String filename) {
        try {
            fosDocIds = new FileOutputStream(filename, true);
            dosDocIds = new DataOutputStream(fosDocIds);
            // TO DO: aggiungere tutti gli altri writers

            opened = true;
        } catch (IOException ie){
            ie.printStackTrace();
            opened = false;
            return false;
        }
        return true;
    }

    @Override
    public void dumpEntry(Map.Entry<String, VocabularyEntry> entry) {
        // TO REVIEW: QUALCUNO DOVRà PRENDERSI START OFFSET E ENDOFFSET
        String term = entry.getKey();
        VocabularyEntry vocabularyEntry = entry.getValue();

        PostingList postingList = vocabularyEntry.getPostingList();
        // ELIAS FANO
        List<Integer> docIdList = postingList.getDocIdList();

        try {
            if (!opened){
                throw new IOException();
            }

            long startOffset = fosDocIds.getChannel().position();

            for (int i = 0; i < docIdList.size(); i += Constants.BLOCK_DIM_ELIASFANO) {
                List<Integer> blockDocIdList = docIdList.subList(i, Math.min(docIdList.size(), i + Constants.BLOCK_DIM_ELIASFANO));
                EliasFanoStruct efs = EliasFano.encode((ArrayList<Integer>) blockDocIdList);

                dosDocIds.writeInt(efs.getU());
                dosDocIds.writeInt(efs.getN());
                dosDocIds.write(efs.getHighBytes());
                dosDocIds.write(efs.getLowBytes());
            }

            long endOffset = fosDocIds.getChannel().position();
        } catch (IOException ie){
            ie.printStackTrace();
        }
    }

    @Override
    public void dumpDocumentIndex(DocumentIndex docIndex) {

    }

    @Override
    public void dumpVocabulary(Vocabulary vocabulary) {

    }

    @Override
    public boolean end() {
        try {
            if (opened) {
                fosDocIds.close();
                dosDocIds.close();
                opened = false;
                return true;
            }
            else throw new IOException();
        } catch (IOException ie){
            ie.printStackTrace();
            return false;
        }
    }
}
