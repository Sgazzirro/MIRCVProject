package it.unipi.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class IOUtils {

    public static List<String> loadStopwords() {
        try {
            return Files.readAllLines(new File(Constants.STOPWORDS_FILE).toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error loading stopwords");
            return List.of();
        }
    }

    public static void createDirectory(String dirPath) {
        // Create working directory if not exists
        try {
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException ignored) { }
    }


}
