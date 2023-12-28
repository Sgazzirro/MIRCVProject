package it.unipi.utils;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class IOUtils {

    private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);

    public static List<String> loadStopwords() {
        try {
            return Files.readAllLines(Constants.STOPWORDS_FILE, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error loading stopwords");
            return List.of();
        }
    }

    /**
     * Clean directory before the creation of the index by deleting old files
     * @param path directory to clean before indexing
     */
    public static void cleanPath(Path path) {
        File[] filesToDelete = new File[] {
                path.resolve(Constants.VOCABULARY_FILENAME).toFile(),
                path.resolve(Constants.DOCUMENT_INDEX_FILENAME).toFile(),
                path.resolve(Constants.DOC_IDS_FILENAME).toFile(),
                path.resolve(Constants.TERM_FREQ_FILENAME).toFile()
        };

        for (File file : filesToDelete)
            if (file.exists() && !file.delete())
                logger.warn("Cannot delete " + file);
    }

    public static void createDirectory(Path dirPath) {
        // Create working directory if not exists
        try {
            Files.createDirectories(dirPath);
        } catch (IOException ignored) { }
    }

    public static void deleteDirectory(Path directoryToBeDeleted) {
        deleteDirectory(directoryToBeDeleted.toFile());
    }

    private static void deleteDirectory(File directoryToBeDeleted) {
        try {
            FileUtils.deleteDirectory(directoryToBeDeleted);
        } catch(IOException ie){
            ie.printStackTrace();
        }
    }

}
