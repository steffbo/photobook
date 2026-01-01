package cc.remer.photobook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PhotobookApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotobookApplication.class, args);
    }
}
