package it.unipi.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class IOUtils {

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

    public static boolean deleteDirectory(Path directoryToBeDeleted) {
        return deleteDirectory(directoryToBeDeleted.toFile());
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
