package cc.remer.photobook;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PhotobookPostgresContainer postgres = PhotobookPostgresContainer.getInstance();

    @Container
    static PhotobookSeaweedFSContainer seaweedfs = PhotobookSeaweedFSContainer.getInstance();

    @LocalServerPort
    protected int port;

    protected RequestSpecification requestSpec;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;
        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(port)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();

        // Clean up database tables before each test (except system data)
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        // Delete data in reverse order of dependencies
        // Don't delete users table as we need the admin user for authentication
        jdbcTemplate.execute("DELETE FROM photo_thumbnails");
        jdbcTemplate.execute("DELETE FROM album_photos");
        jdbcTemplate.execute("DELETE FROM photos");
        jdbcTemplate.execute("DELETE FROM album_users");
        jdbcTemplate.execute("DELETE FROM albums");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
    }

    protected RequestSpecification withAuth(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    protected String getAdminToken() {
        java.util.Map<String, String> loginRequest = new java.util.HashMap<>();
        loginRequest.put("email", "admin@photobook.local");
        loginRequest.put("password", "admin");

        return io.restassured.RestAssured.given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("accessToken");
    }
}
