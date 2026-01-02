package cc.remer.photobook;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PhotobookPostgresContainer postgres = PhotobookPostgresContainer.getInstance();

    @LocalServerPort
    protected int port;

    protected RequestSpecification requestSpec;

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;
        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(port)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    protected RequestSpecification withAuth(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }
}
