package it.unipi.utils;

import it.unipi.model.implementation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class LoadStructures {
/*
    public static String loadLine(ReadingInterface reader) throws IOException {
        // Get the term of the next line
        String lineK = reader.readLine();

        // If block is finished, just close its buffer
        if(lineK == null){
            reader.close();
            return null;
        }
        return lineK;

    }

    public static VocabularyImpl loadVocabulary(String filename) {
        try {
            ReadingInterface reader = new ASCIIReader();
            reader.open(filename);
            VocabularyImpl vocabularyImpl = new VocabularyImpl();

            String line;
            while ((line = loadLine(reader)) != null) {
                String[] split = line.split(",");

                // Vocabulary has csv structure
                //  term | frequency | upper bound | offset in postings | length of postings
                String term = split[0];

                VocabularyEntry entry = new VocabularyEntry(split);
                vocabularyImpl.setEntry(term, entry);
            }

            return vocabularyImpl;

        } catch (IOException e) {
            System.err.println("There has been an error loading the vocabulary");
            e.printStackTrace();
            return null;
        }
    }

    public static DocumentIndexImpl loadDocumentIndex(String filename) {
        try (
                FileInputStream fileInput = new FileInputStream(filename);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileInput, StandardCharsets.UTF_8))
        ) {
            DocumentIndexImpl documentIndexImpl = new DocumentIndexImpl();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(",");

                // Document index has csv structure
                //  doc id | document length
                int docId = Integer.parseInt(split[0]);
                int docLength = Integer.parseInt(split[1]);

                documentIndexImpl.addDocument(docId, docLength);
            }

            return documentIndexImpl;

        } catch (IOException e) {
            System.err.println("There has been an error loading the document index");
            e.printStackTrace();
            return null;
        }
    }
*/
    public static List<String> loadStopwords(String filename) {
        try {
            return Files.readAllLines(new File(filename).toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error loading stopwords");
            return List.of();
        }
    }


}
