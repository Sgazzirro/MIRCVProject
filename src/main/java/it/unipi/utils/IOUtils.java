package it.unipi.utils;

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
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }

        if (directoryToBeDeleted.exists() && !directoryToBeDeleted.delete())
            logger.warn("Cannot delete directory " + directoryToBeDeleted);
    }

}
