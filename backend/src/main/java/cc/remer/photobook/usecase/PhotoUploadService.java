package cc.remer.photobook.usecase;

import cc.remer.photobook.adapter.persistence.PhotoRepository;
import cc.remer.photobook.adapter.storage.S3StorageService;
import cc.remer.photobook.domain.Photo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoUploadService {

    private final S3StorageService storageService;
    private final PhotoRepository photoRepository;
    private final ThumbnailGenerationService thumbnailService;

    @Value("${upload.allowed-extensions}")
    private String allowedExtensionsConfig;

    private Set<String> getAllowedExtensions() {
        return new HashSet<>(Arrays.asList(allowedExtensionsConfig.toLowerCase().split(",")));
    }

    @Transactional
    public List<UUID> uploadPhotos(UUID userId, List<MultipartFile> files) {
        log.info("Starting photo upload for user: {}, files: {}", userId, files.size());

        List<UUID> uploadedPhotoIds = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String filename = file.getOriginalFilename();
                if (filename == null || filename.isEmpty()) {
                    log.warn("Skipping file with empty filename");
                    continue;
                }

                if (isZipFile(filename)) {
                    log.debug("Processing ZIP file: {}", filename);
                    List<UUID> zipPhotoIds = processZipFile(userId, file);
                    uploadedPhotoIds.addAll(zipPhotoIds);
                } else if (isImageFile(filename)) {
                    log.debug("Processing image file: {}", filename);
                    UUID photoId = processImageFile(userId, file);
                    if (photoId != null) {
                        uploadedPhotoIds.add(photoId);
                    }
                } else {
                    log.warn("Skipping unsupported file: {}", filename);
                }
            } catch (Exception e) {
                log.error("Failed to process file: {}", file.getOriginalFilename(), e);
            }
        }

        log.info("Completed photo upload for user: {}, uploaded: {} photos", userId, uploadedPhotoIds.size());
        return uploadedPhotoIds;
    }

    @Transactional
    public UUID uploadSinglePhoto(UUID userId, MultipartFile file) {
        List<UUID> result = uploadPhotos(userId, Collections.singletonList(file));
        return result.isEmpty() ? null : result.get(0);
    }

    private List<UUID> processZipFile(UUID userId, MultipartFile zipFile) throws IOException {
        List<UUID> photoIds = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();
                String filename = getFilenameFromPath(entryName);

                if (!isImageFile(filename)) {
                    log.debug("Skipping non-image entry in ZIP: {}", entryName);
                    continue;
                }

                log.debug("Processing ZIP entry: {}", entryName);

                byte[] fileBytes = zis.readAllBytes();
                UUID photoId = storePhoto(userId, filename, fileBytes, getContentType(filename));

                if (photoId != null) {
                    photoIds.add(photoId);
                }

                zis.closeEntry();
            }
        }

        log.info("Extracted {} photos from ZIP file", photoIds.size());
        return photoIds;
    }

    private UUID processImageFile(UUID userId, MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        byte[] fileBytes = file.getBytes();
        String contentType = file.getContentType();

        if (contentType == null || contentType.isEmpty()) {
            contentType = getContentType(filename);
        }

        return storePhoto(userId, filename, fileBytes, contentType);
    }

    private UUID storePhoto(UUID userId, String originalFilename, byte[] fileBytes, String contentType) {
        try {
            UUID photoId = UUID.randomUUID();
            String fileExtension = getFileExtension(originalFilename);
            String storageKey = buildStorageKey(userId, photoId, fileExtension);

            log.debug("Storing photo: {}, size: {} bytes", storageKey, fileBytes.length);

            storageService.uploadOriginal(
                    storageKey,
                    new ByteArrayInputStream(fileBytes),
                    fileBytes.length,
                    contentType
            );

            Photo photo = Photo.builder()
                    .id(photoId)
                    .ownerId(userId)
                    .storageKey(storageKey)
                    .originalFilename(originalFilename)
                    .mimeType(contentType)
                    .fileSize((long) fileBytes.length)
                    .status("PROCESSING")
                    .build();

            photoRepository.save(photo);

            thumbnailService.generateThumbnailsAsync(photoId);

            log.debug("Successfully stored photo: {}", photoId);
            return photoId;
        } catch (Exception e) {
            log.error("Failed to store photo: {}", originalFilename, e);
            return null;
        }
    }

    private String buildStorageKey(UUID userId, UUID photoId, String extension) {
        return String.format("%s/%s.%s", userId, photoId, extension);
    }

    private boolean isZipFile(String filename) {
        String extension = getFileExtension(filename);
        return "zip".equalsIgnoreCase(extension);
    }

    private boolean isImageFile(String filename) {
        String extension = getFileExtension(filename);
        return getAllowedExtensions().contains(extension.toLowerCase());
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private String getFilenameFromPath(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }

    private String getContentType(String filename) {
        String extension = getFileExtension(filename);
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            case "heif" -> "image/heif";
            default -> "application/octet-stream";
        };
    }
}
