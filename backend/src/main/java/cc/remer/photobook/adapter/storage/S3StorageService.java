package cc.remer.photobook.adapter.storage;

import cc.remer.photobook.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public void uploadOriginal(String key, InputStream inputStream, long contentLength, String contentType) {
        uploadFile(properties.getBuckets().getOriginals(), key, inputStream, contentLength, contentType);
    }

    public void uploadThumbnail(String key, InputStream inputStream, long contentLength) {
        uploadFile(properties.getBuckets().getThumbnails(), key, inputStream, contentLength, "image/jpeg");
    }

    public void deleteOriginal(String key) {
        deleteFile(properties.getBuckets().getOriginals(), key);
    }

    public void deleteThumbnail(String key) {
        deleteFile(properties.getBuckets().getThumbnails(), key);
    }

    public InputStream downloadOriginal(String key) {
        return downloadFile(properties.getBuckets().getOriginals(), key);
    }

    public InputStream downloadThumbnail(String key) {
        return downloadFile(properties.getBuckets().getThumbnails(), key);
    }

    public String getPresignedOriginalUrl(String key, Duration expiration) {
        return getPresignedUrl(properties.getBuckets().getOriginals(), key, expiration);
    }

    public String getPresignedThumbnailUrl(String key, Duration expiration) {
        return getPresignedUrl(properties.getBuckets().getThumbnails(), key, expiration);
    }

    public String generatePresignedUrl(String key, int expirationSeconds) {
        // Determine if this is a thumbnail or original based on the key pattern
        // Thumbnails have format: userId/photoId_size.jpg
        // Originals have format: userId/photoId.ext
        String bucket = key.contains("_") ?
                properties.getBuckets().getThumbnails() :
                properties.getBuckets().getOriginals();

        return getPresignedUrl(bucket, key, Duration.ofSeconds(expirationSeconds));
    }

    public void ensureBucketsExist() {
        ensureBucketExists(properties.getBuckets().getOriginals());
        ensureBucketExists(properties.getBuckets().getThumbnails());
    }

    private void uploadFile(String bucket, String key, InputStream inputStream, long contentLength, String contentType) {
        try {
            log.debug("Uploading file to bucket: {}, key: {}, size: {} bytes", bucket, key, contentLength);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

            log.debug("Successfully uploaded file to bucket: {}, key: {}", bucket, key);
        } catch (Exception e) {
            log.error("Failed to upload file to bucket: {}, key: {}", bucket, key, e);
            throw new StorageException("Failed to upload file: " + key, e);
        }
    }

    private void deleteFile(String bucket, String key) {
        try {
            log.debug("Deleting file from bucket: {}, key: {}", bucket, key);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);

            log.debug("Successfully deleted file from bucket: {}, key: {}", bucket, key);
        } catch (Exception e) {
            log.error("Failed to delete file from bucket: {}, key: {}", bucket, key, e);
            throw new StorageException("Failed to delete file: " + key, e);
        }
    }

    private InputStream downloadFile(String bucket, String key) {
        try {
            log.debug("Downloading file from bucket: {}, key: {}", bucket, key);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            log.error("File not found in bucket: {}, key: {}", bucket, key);
            throw new StorageException("File not found: " + key, e);
        } catch (Exception e) {
            log.error("Failed to download file from bucket: {}, key: {}", bucket, key, e);
            throw new StorageException("Failed to download file: " + key, e);
        }
    }

    private String getPresignedUrl(String bucket, String key, Duration expiration) {
        try {
            log.debug("Generating presigned URL for bucket: {}, key: {}, expiration: {}", bucket, key, expiration);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for bucket: {}, key: {}", bucket, key, e);
            throw new StorageException("Failed to generate presigned URL: " + key, e);
        }
    }

    private void ensureBucketExists(String bucket) {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3Client.headBucket(headBucketRequest);
            log.debug("Bucket exists: {}", bucket);
        } catch (NoSuchBucketException e) {
            log.info("Bucket does not exist, creating: {}", bucket);
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3Client.createBucket(createBucketRequest);
            log.info("Successfully created bucket: {}", bucket);
        } catch (Exception e) {
            log.error("Failed to check/create bucket: {}", bucket, e);
            throw new StorageException("Failed to ensure bucket exists: " + bucket, e);
        }
    }
}
