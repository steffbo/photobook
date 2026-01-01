package cc.remer.photobook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "seaweedfs")
public class S3Properties {
    private S3Config s3 = new S3Config();
    private BucketConfig buckets = new BucketConfig();

    @Getter
    @Setter
    public static class S3Config {
        private String endpoint;
        private String accessKey;
        private String secretKey;
    }

    @Getter
    @Setter
    public static class BucketConfig {
        private String originals;
        private String thumbnails;
    }
}
