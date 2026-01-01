package cc.remer.photobook.adapter.web;

import cc.remer.photobook.BaseIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Authentication API Integration Tests")
class AuthenticationControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/auth/login - Success with valid credentials")
    void loginWithValidCredentials_shouldReturnTokens() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@photobook.local");
        loginRequest.put("password", "admin");

        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("expiresIn", greaterThan(0))
            .body("user.email", equalTo("admin@photobook.local"))
            .body("user.role", equalTo("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Failure with invalid email")
    void loginWithInvalidEmail_shouldReturn401() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "nonexistent@photobook.local");
        loginRequest.put("password", "wrongpassword");

        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_CREDENTIALS"))
            .body("message", containsString("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Failure with wrong password")
    void loginWithWrongPassword_shouldReturn401() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@photobook.local");
        loginRequest.put("password", "wrongpassword");

        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_CREDENTIALS"))
            .body("message", containsString("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Failure with missing email")
    void loginWithMissingEmail_shouldReturn400() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("password", "admin");

        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/auth/login - Failure with missing password")
    void loginWithMissingPassword_shouldReturn400() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@photobook.local");

        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Success with valid refresh token")
    void refreshTokenWithValidToken_shouldReturnNewTokens() {
        // First, login to get a refresh token
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@photobook.local");
        loginRequest.put("password", "admin");

        String refreshToken = given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("refreshToken");

        // Now use the refresh token
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", refreshToken);

        given()
            .spec(requestSpec)
            .body(refreshRequest)
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("refreshToken", not(equalTo(refreshToken))) // Should be a new token
            .body("expiresIn", greaterThan(0))
            .body("user.email", equalTo("admin@photobook.local"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Failure with invalid refresh token")
    void refreshTokenWithInvalidToken_shouldReturn401() {
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", "invalid.token.here");

        given()
            .spec(requestSpec)
            .body(refreshRequest)
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Failure with missing refresh token")
    void refreshTokenWithMissingToken_shouldReturn400() {
        Map<String, String> refreshRequest = new HashMap<>();

        given()
            .spec(requestSpec)
            .body(refreshRequest)
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Failure when reusing revoked token")
    void refreshTokenWithRevokedToken_shouldReturn401() {
        // Login to get tokens
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@photobook.local");
        loginRequest.put("password", "admin");

        String refreshToken = given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("refreshToken");

        // Use the refresh token once (this revokes the old one)
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", refreshToken);

        given()
            .spec(requestSpec)
            .body(refreshRequest)
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(200);

        // Try to use the same refresh token again (should fail)
        given()
            .spec(requestSpec)
            .body(refreshRequest)
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_TOKEN"))
            .body("message", containsString("revoked"));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Success with valid refresh token")
    void logoutWithValidToken_shouldReturn204() {
        // Login to get tokens
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@photobook.local");
        loginRequest.put("password", "admin");

        Map<String, String> tokens = given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().as(Map.class);

        // Logout
        Map<String, String> logoutRequest = new HashMap<>();
        logoutRequest.put("refreshToken", tokens.get("refreshToken"));

        given()
            .spec(withAuth(tokens.get("accessToken")))
            .body(logoutRequest)
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(204);

        // Verify the refresh token is now invalid
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", tokens.get("refreshToken"));

        given()
            .spec(requestSpec)
            .body(refreshRequest)
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Failure without authentication even with token in body")
    void logoutWithInvalidToken_shouldReturn403() {
        Map<String, String> logoutRequest = new HashMap<>();
        logoutRequest.put("refreshToken", "invalid.token");

        given()
            .spec(requestSpec)
            .body(logoutRequest)
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(403); // Spring Security returns 403 for anonymous access to protected endpoints
    }

    @Test
    @DisplayName("POST /api/auth/logout - Failure without authentication")
    void logoutWithoutAuth_shouldReturn403() {
        Map<String, String> logoutRequest = new HashMap<>();
        logoutRequest.put("refreshToken", "some.token");

        given()
            .spec(requestSpec)
            .body(logoutRequest)
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(403); // Spring Security returns 403 for anonymous access to protected endpoints
    }
}
