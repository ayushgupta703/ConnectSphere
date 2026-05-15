package com.connectsphere.mediaservice.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUtil {

    private final Path root = Paths.get("uploads");

    public FileUtil() {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    public String save(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();

// 🔥 sanitize filename
            String safeName = originalName
                    .replaceAll("[^a-zA-Z0-9\\.\\-]", "_"); // removes #, emojis, spaces

            String filename = UUID.randomUUID() + "_" + safeName;
            Files.copy(file.getInputStream(), this.root.resolve(filename));
            return filename;
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    public void delete(String filename) {
        try {
            Files.deleteIfExists(this.root.resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}
