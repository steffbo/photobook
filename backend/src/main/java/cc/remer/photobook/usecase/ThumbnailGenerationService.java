package cc.remer.photobook.usecase;

import cc.remer.photobook.adapter.persistence.PhotoRepository;
import cc.remer.photobook.adapter.persistence.PhotoThumbnailRepository;
import cc.remer.photobook.adapter.storage.S3StorageService;
import cc.remer.photobook.domain.Photo;
import cc.remer.photobook.domain.PhotoThumbnail;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailGenerationService {

    private final S3StorageService storageService;
    private final PhotoRepository photoRepository;
    private final PhotoThumbnailRepository thumbnailRepository;

    @Value("${thumbnail.sizes.small}")
    private int smallSize;

    @Value("${thumbnail.sizes.medium}")
    private int mediumSize;

    @Value("${thumbnail.sizes.large}")
    private int largeSize;

    @Value("${thumbnail.quality}")
    private double quality;

    @Async
    @Transactional
    public void generateThumbnailsAsync(UUID photoId) {
        try {
            log.info("Starting async thumbnail generation for photo: {}", photoId);
            generateThumbnails(photoId);
            log.info("Completed async thumbnail generation for photo: {}", photoId);
        } catch (Exception e) {
            log.error("Failed to generate thumbnails for photo: {}", photoId, e);

            Photo photo = photoRepository.findById(photoId).orElse(null);
            if (photo != null) {
                photo.setStatus("FAILED");
                photoRepository.save(photo);
            }
        }
    }

    @Transactional
    public void generateThumbnails(UUID photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));

        try {
            log.debug("Generating thumbnails for photo: {}", photoId);

            InputStream originalStream = storageService.downloadOriginal(photo.getStorageKey());
            byte[] originalBytes = originalStream.readAllBytes();

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (originalImage == null) {
                throw new IllegalArgumentException("Unable to read image file");
            }

            Map<String, Object> exifData = extractExifData(new ByteArrayInputStream(originalBytes));
            photo.setExifData(exifData);
            photo.setWidth(originalImage.getWidth());
            photo.setHeight(originalImage.getHeight());

            generateAndStoreThumbnail(photo, originalImage, "SMALL", smallSize);
            generateAndStoreThumbnail(photo, originalImage, "MEDIUM", mediumSize);
            generateAndStoreThumbnail(photo, originalImage, "LARGE", largeSize);

            photo.setStatus("READY");
            photoRepository.save(photo);

            log.debug("Successfully generated thumbnails for photo: {}", photoId);
        } catch (Exception e) {
            log.error("Failed to generate thumbnails for photo: {}", photoId, e);
            photo.setStatus("FAILED");
            photoRepository.save(photo);
            throw new RuntimeException("Failed to generate thumbnails", e);
        }
    }

    private void generateAndStoreThumbnail(Photo photo, BufferedImage originalImage,
                                          String sizeName, int maxSize) throws Exception {
        log.debug("Generating {} thumbnail for photo: {}", sizeName, photo.getId());

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        int targetWidth;
        int targetHeight;

        if (width > height) {
            targetWidth = maxSize;
            targetHeight = (int) ((double) height / width * maxSize);
        } else {
            targetHeight = maxSize;
            targetWidth = (int) ((double) width / height * maxSize);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(originalImage)
                .size(targetWidth, targetHeight)
                .outputFormat("jpg")
                .outputQuality(quality)
                .toOutputStream(outputStream);

        byte[] thumbnailBytes = outputStream.toByteArray();

        String fileExtension = getFileExtension(photo.getOriginalFilename());
        String baseFilename = photo.getStorageKey().replace("." + fileExtension, "");
        String thumbnailKey = baseFilename + "_" + sizeName.toLowerCase() + ".jpg";

        storageService.uploadThumbnail(
                thumbnailKey,
                new ByteArrayInputStream(thumbnailBytes),
                thumbnailBytes.length
        );

        PhotoThumbnail thumbnail = PhotoThumbnail.builder()
                .photoId(photo.getId())
                .size(sizeName)
                .storageKey(thumbnailKey)
                .width(targetWidth)
                .height(targetHeight)
                .fileSize((long) thumbnailBytes.length)
                .build();

        thumbnailRepository.save(thumbnail);

        log.debug("Successfully generated {} thumbnail for photo: {}", sizeName, photo.getId());
    }

    private Map<String, Object> extractExifData(InputStream inputStream) {
        Map<String, Object> exifData = new HashMap<>();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifIFD0 != null) {
                if (exifIFD0.containsTag(ExifIFD0Directory.TAG_MAKE)) {
                    exifData.put("make", exifIFD0.getString(ExifIFD0Directory.TAG_MAKE));
                }
                if (exifIFD0.containsTag(ExifIFD0Directory.TAG_MODEL)) {
                    exifData.put("model", exifIFD0.getString(ExifIFD0Directory.TAG_MODEL));
                }
                if (exifIFD0.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                    exifData.put("orientation", exifIFD0.getInteger(ExifIFD0Directory.TAG_ORIENTATION));
                }
            }

            ExifSubIFDDirectory exifSubIFD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifSubIFD != null) {
                if (exifSubIFD.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                    Date date = exifSubIFD.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                    if (date != null) {
                        exifData.put("dateTimeOriginal", date.toInstant().toString());
                    }
                }
                if (exifSubIFD.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
                    exifData.put("exposureTime", exifSubIFD.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                }
                if (exifSubIFD.containsTag(ExifSubIFDDirectory.TAG_FNUMBER)) {
                    exifData.put("fNumber", exifSubIFD.getString(ExifSubIFDDirectory.TAG_FNUMBER));
                }
                if (exifSubIFD.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
                    exifData.put("iso", exifSubIFD.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                }
                if (exifSubIFD.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                    exifData.put("focalLength", exifSubIFD.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                }
            }

            JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpegDirectory != null) {
                if (jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_WIDTH)) {
                    exifData.put("imageWidth", jpegDirectory.getInteger(JpegDirectory.TAG_IMAGE_WIDTH));
                }
                if (jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)) {
                    exifData.put("imageHeight", jpegDirectory.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT));
                }
            }

            log.debug("Extracted EXIF data: {}", exifData);
        } catch (Exception e) {
            log.warn("Failed to extract EXIF data: {}", e.getMessage());
        }

        return exifData;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}
