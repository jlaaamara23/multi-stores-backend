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
import java.util.ArrayList;
import java.util.List;
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
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        String filename = UUID.randomUUID().toString() + ext;
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target);
        return "/uploads/" + filename;
    }

    public boolean isCloudinaryEnabled() {
        return cloudinaryEnabled && cloudinary != null;
    }

    /**
     * Lists image secure URLs from a Cloudinary folder (paginated Admin API).
     * Each entry is [secureUrl, publicId].
     */
    @SuppressWarnings("unchecked")
    public List<CloudinaryAsset> listImagesInFolder(String folder, int maxImages) throws IOException {
        if (!isCloudinaryEnabled()) {
            throw new IllegalStateException("Cloudinary is not configured.");
        }
        String prefix = folder == null || folder.isBlank() ? "multi-stores/" : folder.trim();
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        int limit = Math.max(1, Math.min(maxImages, 5000));
        List<CloudinaryAsset> assets = new ArrayList<>();
        String nextCursor = null;
        try {
            do {
                int pageSize = Math.min(500, limit - assets.size());
                Map<String, Object> options = ObjectUtils.asMap(
                        "type", "upload",
                        "resource_type", "image",
                        "prefix", prefix,
                        "max_results", pageSize
                );
                if (nextCursor != null && !nextCursor.isBlank()) {
                    options.put("next_cursor", nextCursor);
                }
                Map<?, ?> result = cloudinary.api().resources(options);
                Object resourcesObj = result.get("resources");
                if (resourcesObj instanceof List<?> resources) {
                    for (Object item : resources) {
                        if (!(item instanceof Map<?, ?> resource)) continue;
                        Object url = resource.get("secure_url");
                        Object publicId = resource.get("public_id");
                        if (url == null || url.toString().isBlank()) continue;
                        assets.add(new CloudinaryAsset(
                                url.toString(),
                                publicId != null ? publicId.toString() : url.toString()
                        ));
                        if (assets.size() >= limit) break;
                    }
                }
                Object cursor = result.get("next_cursor");
                nextCursor = cursor != null ? cursor.toString() : null;
            } while (nextCursor != null && !nextCursor.isBlank() && assets.size() < limit);
        } catch (Exception e) {
            throw new IOException("Failed to list Cloudinary folder '" + prefix + "': " + e.getMessage(), e);
        }
        return assets;
    }

    public record CloudinaryAsset(String secureUrl, String publicId) {}
}
