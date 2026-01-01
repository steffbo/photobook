package cc.remer.photobook.config;

import cc.remer.photobook.adapter.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.initialization.enabled", havingValue = "true", matchIfMissing = true)
public class StorageInitializer {

    private final S3StorageService storageService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeStorage() {
        log.info("Initializing storage: ensuring S3 buckets exist");
        try {
            storageService.ensureBucketsExist();
            log.info("Storage initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize storage", e);
        }
    }
}
