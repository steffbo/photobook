package cc.remer.photobook;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton PostgreSQL container for integration tests.
 * Implements the shared container pattern with Flyway integration.
 */
public class PhotobookPostgresContainer extends PostgreSQLContainer<PhotobookPostgresContainer> {

    private static final String IMAGE_VERSION = "postgres:17-alpine";
    private static PhotobookPostgresContainer container;
    private static boolean migrated = false;

    private PhotobookPostgresContainer() {
        super(DockerImageName.parse(IMAGE_VERSION));
    }

    public static PhotobookPostgresContainer getInstance() {
        if (container == null) {
            container = new PhotobookPostgresContainer()
                    .withDatabaseName("photobook_test")
                    .withUsername("test")
                    .withPassword("test");
        }
        return container;
    }

    @Override
    public void start() {
        super.start();

        // Set system properties for Spring Boot
        System.setProperty("DB_URL", container.getJdbcUrl());
        System.setProperty("DB_USERNAME", container.getUsername());
        System.setProperty("DB_PASSWORD", container.getPassword());

        // Run Flyway migrations once
        if (!migrated) {
            migrateDatabaseSchema();
            migrated = true;
        }
    }

    private void migrateDatabaseSchema() {
        System.out.println("Starting Flyway database migrations...");
        long startTime = System.currentTimeMillis();

        // Retry mechanism for Flyway migration (container may not be fully ready)
        int maxRetries = 10;
        int retryDelayMs = 500;

        for (int i = 0; i < maxRetries; i++) {
            try {
                if (i > 0) {
                    System.out.println("Retry attempt " + (i + 1) + " for Flyway migration...");
                }

                Flyway flyway = Flyway.configure()
                        .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .load();
                flyway.migrate();

                long duration = System.currentTimeMillis() - startTime;
                System.out.println("Flyway migrations completed successfully in " + duration + "ms");
                return; // Success
            } catch (Exception e) {
                if (i == maxRetries - 1) {
                    long duration = System.currentTimeMillis() - startTime;
                    System.err.println("Failed to run Flyway migrations after " + maxRetries + " attempts and " + duration + "ms");
                    throw new RuntimeException("Failed to run Flyway migrations after " + maxRetries + " attempts", e);
                }
                System.out.println("Migration attempt " + (i + 1) + " failed, waiting " + retryDelayMs + "ms before retry...");
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for database", ie);
                }
            }
        }
    }

    @Override
    public void stop() {
        // Do nothing, JVM handles shutdown
    }
}
