package com.example.multi_stores.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path uploadDir;
    private final Cloudinary cloudinary;
    private final boolean cloudinaryEnabled;

    public FileStorageService(
            @Value("${app.upload-dir:uploads}") String uploadDirName,
            @Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
            @Value("${CLOUDINARY_API_KEY:}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET:}") String apiSecret
    ) {
        this.uploadDir = Paths.get(uploadDirName).toAbsolutePath().normalize();
        this.cloudinaryEnabled = cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();

        if (cloudinaryEnabled) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName.trim(),
                    "api_key", apiKey.trim(),
                    "api_secret", apiSecret.trim(),
                    "secure", true
            ));
            log.info("Cloudinary image storage enabled.");
        } else {
            this.cloudinary = null;
            log.warn("Cloudinary is not configured. Falling back to local uploads directory.");
        }

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * Saves the file to Cloudinary when configured, otherwise local uploads directory.
     */
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        detectImageExtension(file); // rejects anything that is not a JPEG, PNG, GIF or WebP image
        if (cloudinaryEnabled && cloudinary != null) {
            return storeInCloudinary(file);
        }
        return storeLocally(file);
    }

    private String storeInCloudinary(MultipartFile file) throws IOException {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "multi-stores",
                            "resource_type", "image"
                    )
            );
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                throw new IOException("Cloudinary upload succeeded but secure_url was missing.");
            }
            return secureUrl.toString();
        } catch (Exception e) {
            throw new IOException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    private String storeLocally(MultipartFile file) throws IOException {
        // Extension comes from the detected file type, never from the client's file name.
        String ext = detectImageExtension(file);
        String filename = UUID.randomUUID().toString() + ext;
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target);
        return "/uploads/" + filename;
    }

    /**
     * Checks the file's first bytes (not its name or declared type) and returns the matching extension.
     * Only JPEG, PNG, GIF and WebP are accepted.
     */
    static String detectImageExtension(MultipartFile file) throws IOException {
        byte[] h = new byte[12];
        int n;
        try (java.io.InputStream in = file.getInputStream()) {
            n = in.readNBytes(h, 0, h.length);
        }
        if (n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return ".jpg";
        if (n >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return ".png";
        if (n >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') return ".gif";
        if (n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return ".webp";
        throw new IllegalArgumentException("Only JPEG, PNG, GIF or WebP images can be uploaded");
    }
}
