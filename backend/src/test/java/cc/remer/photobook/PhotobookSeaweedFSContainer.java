package cc.remer.photobook;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;
import java.time.Duration;

/**
 * Singleton MinIO container for integration tests (S3-compatible storage).
 * Implements the shared container pattern with bucket initialization.
 * Using MinIO instead of SeaweedFS for simpler test setup.
 */
public class PhotobookSeaweedFSContainer extends GenericContainer<PhotobookSeaweedFSContainer> {

    private static final String IMAGE_VERSION = "minio/minio:latest";
    private static final int S3_PORT = 9000;
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";
    private static PhotobookSeaweedFSContainer container;
    private static boolean bucketsCreated = false;

    private PhotobookSeaweedFSContainer() {
        super(DockerImageName.parse(IMAGE_VERSION));

        // Start MinIO with S3 API
        withCommand("server", "/data");
        withExposedPorts(S3_PORT);
        withEnv("MINIO_ROOT_USER", ACCESS_KEY);
        withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY);

        // Wait for MinIO API to be ready
        waitingFor(Wait.forHttp("/minio/health/live")
                .forPort(S3_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(60)));
    }

    public static PhotobookSeaweedFSContainer getInstance() {
        if (container == null) {
            container = new PhotobookSeaweedFSContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();

        // Get the S3 endpoint
        String s3Endpoint = "http://" + getHost() + ":" + getMappedPort(S3_PORT);

        // Set system properties for Spring Boot
        System.setProperty("SEAWEEDFS_S3_ENDPOINT", s3Endpoint);
        System.setProperty("SEAWEEDFS_ACCESS_KEY", ACCESS_KEY);
        System.setProperty("SEAWEEDFS_SECRET_KEY", SECRET_KEY);

        // Create buckets once
        if (!bucketsCreated) {
            createBuckets(s3Endpoint);
            bucketsCreated = true;
        }
    }

    private void createBuckets(String s3Endpoint) {
        System.out.println("Creating MinIO buckets...");
        long startTime = System.currentTimeMillis();

        try {
            // Create S3 client for bucket creation
            S3Client s3Client = S3Client.builder()
                    .endpointOverride(URI.create(s3Endpoint))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                    .forcePathStyle(true)
                    .build();

            // Create test buckets
            try {
                s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket("test-originals")
                        .build());
                System.out.println("Created bucket: test-originals");
            } catch (Exception e) {
                System.out.println("Bucket test-originals may already exist: " + e.getMessage());
            }

            try {
                s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket("test-thumbnails")
                        .build());
                System.out.println("Created bucket: test-thumbnails");
            } catch (Exception e) {
                System.out.println("Bucket test-thumbnails may already exist: " + e.getMessage());
            }

            s3Client.close();

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("MinIO buckets created successfully in " + duration + "ms");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("Failed to create MinIO buckets after " + duration + "ms");
            throw new RuntimeException("Failed to create MinIO buckets", e);
        }
    }

    @Override
    public void stop() {
        // Do nothing, JVM handles shutdown
    }

    /**
     * Get the S3 endpoint URL for the container
     */
    public String getS3Endpoint() {
        return "http://" + getHost() + ":" + getMappedPort(S3_PORT);
    }
}
