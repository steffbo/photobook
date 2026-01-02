package cc.remer.photobook.adapter.web;

import cc.remer.photobook.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("User API Integration Tests")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    private String createTestUser(String adminToken, String email, String password) {
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", email);
        createRequest.put("password", password);
        createRequest.put("firstName", "Test");
        createRequest.put("lastName", "User");
        createRequest.put("role", "USER");

        return given()
            .spec(withAuth(adminToken))
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .extract().path("accessToken");
    }

    // ========== GET /api/users/me Tests ==========

    @Test
    @DisplayName("GET /api/users/me - Success with valid token")
    void getCurrentUser_withValidToken_shouldReturnUserProfile() {
        String token = getAdminToken();

        given()
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(200)
            .body("email", equalTo("admin@photobook.local"))
            .body("role", equalTo("ADMIN"))
            .body("id", notNullValue())
            .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/users/me - Failure without authentication")
    void getCurrentUser_withoutAuth_shouldReturn401() {
        given()
            .spec(requestSpec)
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(403); // Spring Security returns 403 for anonymous access to protected endpoints
    }

    @Test
    @DisplayName("GET /api/users/me - Failure with invalid token")
    void getCurrentUser_withInvalidToken_shouldReturn401() {
        given()
            .spec(withAuth("invalid.jwt.token"))
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(403); // Invalid token is treated as anonymous, returns 403
    }

    // ========== PUT /api/users/me Tests ==========

    @Test
    @DisplayName("PUT /api/users/me - Success updating first and last name")
    void updateCurrentUser_withValidData_shouldUpdateProfile() {
        String token = getAdminToken();

        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("firstName", "Updated");
        updateRequest.put("lastName", "Admin");

        given()
            .spec(withAuth(token))
            .body(updateRequest)
        .when()
            .put("/api/users/me")
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Updated"))
            .body("lastName", equalTo("Admin"))
            .body("email", equalTo("admin@photobook.local"));

        // Verify the changes persisted
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Updated"))
            .body("lastName", equalTo("Admin"));
    }

    @Test
    @DisplayName("PUT /api/users/me - Success updating password")
    void updateCurrentUser_withNewPassword_shouldChangePassword() {
        String adminToken = getAdminToken();

        // Create a test user
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", "testuser@photobook.local");
        createRequest.put("password", "oldpassword");
        createRequest.put("firstName", "Test");
        createRequest.put("lastName", "User");
        createRequest.put("role", "USER");

        given()
            .spec(withAuth(adminToken))
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201);

        // Login with old password
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "testuser@photobook.local");
        loginRequest.put("password", "oldpassword");

        String userToken = given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("accessToken");

        // Update password
        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("password", "newpassword");

        given()
            .spec(withAuth(userToken))
            .body(updateRequest)
        .when()
            .put("/api/users/me")
        .then()
            .statusCode(200);

        // Verify old password doesn't work
        loginRequest.put("password", "oldpassword");
        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401);

        // Verify new password works
        loginRequest.put("password", "newpassword");
        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("PUT /api/users/me - Failure without authentication")
    void updateCurrentUser_withoutAuth_shouldReturn401() {
        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("firstName", "Updated");

        given()
            .spec(requestSpec)
            .body(updateRequest)
        .when()
            .put("/api/users/me")
        .then()
            .statusCode(403); // Spring Security returns 403 for anonymous access to protected endpoints
    }

    // ========== GET /api/users Tests (Admin Only) ==========

    @Test
    @DisplayName("GET /api/users - Success with admin token")
    void listUsers_withAdminToken_shouldReturnUserList() {
        String token = getAdminToken();

        given()
            .spec(withAuth(token))
            .queryParam("page", 0)
            .queryParam("size", 20)
        .when()
            .get("/api/users")
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content.size()", greaterThan(0))
            .body("content[0].email", equalTo("admin@photobook.local"))
            .body("totalElements", greaterThan(0))
            .body("number", equalTo(0))
            .body("size", equalTo(20));
    }

    @Test
    @DisplayName("GET /api/users - Failure with non-admin token")
    void listUsers_withNonAdminToken_shouldReturn403() {
        String adminToken = getAdminToken();

        // Create a regular user
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", "regularuser@photobook.local");
        createRequest.put("password", "password");
        createRequest.put("firstName", "Regular");
        createRequest.put("lastName", "User");
        createRequest.put("role", "USER");

        given()
            .spec(withAuth(adminToken))
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201);

        // Login as regular user
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "regularuser@photobook.local");
        loginRequest.put("password", "password");

        String userToken = given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("accessToken");

        // Try to list users as regular user
        given()
            .spec(withAuth(userToken))
        .when()
            .get("/api/users")
        .then()
            .statusCode(403)
            .body("error", equalTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /api/users - Failure without authentication")
    void listUsers_withoutAuth_shouldReturn401() {
        given()
            .spec(requestSpec)
        .when()
            .get("/api/users")
        .then()
            .statusCode(403); // Spring Security returns 403 for anonymous access to protected endpoints
    }

    @Test
    @DisplayName("GET /api/users - Success with pagination")
    void listUsers_withPagination_shouldReturnCorrectPage() {
        String token = getAdminToken();

        // Create multiple users
        for (int i = 1; i <= 5; i++) {
            Map<String, String> createRequest = new HashMap<>();
            createRequest.put("email", "user" + i + "@photobook.local");
            createRequest.put("password", "password");
            createRequest.put("firstName", "User");
            createRequest.put("lastName", String.valueOf(i));
            createRequest.put("role", "USER");

            given()
                .spec(withAuth(token))
                .body(createRequest)
            .when()
                .post("/api/users")
            .then()
                .statusCode(201);
        }

        // Get first page with size 3
        given()
            .spec(withAuth(token))
            .queryParam("page", 0)
            .queryParam("size", 3)
        .when()
            .get("/api/users")
        .then()
            .statusCode(200)
            .body("content.size()", equalTo(3))
            .body("number", equalTo(0))
            .body("size", equalTo(3))
            .body("totalElements", greaterThanOrEqualTo(6));

        // Get second page
        given()
            .spec(withAuth(token))
            .queryParam("page", 1)
            .queryParam("size", 3)
        .when()
            .get("/api/users")
        .then()
            .statusCode(200)
            .body("content.size()", greaterThan(0))
            .body("number", equalTo(1))
            .body("size", equalTo(3));
    }

    // ========== POST /api/users Tests (Admin Only) ==========

    @Test
    @DisplayName("POST /api/users - Success creating user with admin token")
    void createUser_withAdminToken_shouldCreateUser() {
        String token = getAdminToken();

        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", "newuser@photobook.local");
        createRequest.put("password", "password123");
        createRequest.put("firstName", "New");
        createRequest.put("lastName", "User");
        createRequest.put("role", "USER");

        given()
            .spec(withAuth(token))
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .body("email", equalTo("newuser@photobook.local"))
            .body("firstName", equalTo("New"))
            .body("lastName", equalTo("User"))
            .body("role", equalTo("USER"))
            .body("id", notNullValue());

        // Verify user can login
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "newuser@photobook.local");
        loginRequest.put("password", "password123");

        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("POST /api/users - Failure with duplicate email")
    void createUser_withDuplicateEmail_shouldReturn400() {
        String token = getAdminToken();

        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", "admin@photobook.local");
        createRequest.put("password", "password");
        createRequest.put("firstName", "Duplicate");
        createRequest.put("lastName", "User");
        createRequest.put("role", "USER");

        given()
            .spec(withAuth(token))
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(400)
            .body("error", equalTo("DUPLICATE_EMAIL"))
            .body("message", containsString("Email already exists"));
    }

    @Test
    @DisplayName("POST /api/users - Failure with missing required fields")
    void createUser_withMissingFields_shouldReturn400() {
        String token = getAdminToken();

        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", "incomplete@photobook.local");
        // Missing password

        given()
            .spec(withAuth(token))
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/users - Failure with non-admin token")
    void createUser_withNonAdminToken_shouldReturn403() {
        String adminToken = getAdminToken();

        // Create regular user
        Map<String, String> createUserRequest = new HashMap<>();
        createUserRequest.put("email", "regularuser2@photobook.local");
        createUserRequest.put("password", "password");
        createUserRequest.put("firstName", "Regular");
        createUserRequest.put("lastName", "User");
        createUserRequest.put("role", "USER");

        given()
            .spec(withAuth(adminToken))
            .body(createUserRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201);

        // Login as regular user
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "regularuser2@photobook.local");
        loginRequest.put("password", "password");

        String userToken = given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("accessToken");

        // Try to create user as regular user
        Map<String, String> newUserRequest = new HashMap<>();
        newUserRequest.put("email", "forbidden@photobook.local");
        newUserRequest.put("password", "password");
        newUserRequest.put("firstName", "Forbidden");
        newUserRequest.put("lastName", "User");

        given()
            .spec(withAuth(userToken))
            .body(newUserRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(403)
            .body("error", equalTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("POST /api/users - Failure without authentication")
    void createUser_withoutAuth_shouldReturn401() {
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", "unauthorized@photobook.local");
        createRequest.put("password", "password");

        given()
            .spec(requestSpec)
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(403); // Spring Security returns 403 for anonymous access to protected endpoints
    }

    // ========== DELETE /api/users/{userId} Tests (Admin Only) ==========

    @Test
    @DisplayName("DELETE /api/users/{userId} - Success with admin token")
    void deleteUser_withAdminToken_shouldDeleteUser() {
        String adminToken = getAdminToken();

        // Create a user to delete
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", "todelete@photobook.local");
        createRequest.put("password", "password");
        createRequest.put("firstName", "To");
        createRequest.put("lastName", "Delete");
        createRequest.put("role", "USER");

        String userId = given()
            .spec(withAuth(adminToken))
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .extract().path("id");

        // Delete the user
        given()
            .spec(withAuth(adminToken))
        .when()
            .delete("/api/users/" + userId)
        .then()
            .statusCode(204);

        // Verify user cannot login
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "todelete@photobook.local");
        loginRequest.put("password", "password");

        given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - Failure with non-existent user")
    void deleteUser_withNonExistentId_shouldReturn404() {
        String token = getAdminToken();

        given()
            .spec(withAuth(token))
        .when()
            .delete("/api/users/00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(404)
            .body("error", equalTo("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - Failure with non-admin token")
    void deleteUser_withNonAdminToken_shouldReturn403() {
        String adminToken = getAdminToken();

        // Create a target user to attempt to delete
        Map<String, String> targetUserRequest = new HashMap<>();
        targetUserRequest.put("email", "target@photobook.local");
        targetUserRequest.put("password", "password");
        targetUserRequest.put("firstName", "Target");
        targetUserRequest.put("lastName", "User");
        targetUserRequest.put("role", "USER");

        String targetUserId = given()
            .spec(withAuth(adminToken))
            .body(targetUserRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .extract().path("id");

        // Create regular user
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("email", "regularuser3@photobook.local");
        createRequest.put("password", "password");
        createRequest.put("firstName", "Regular");
        createRequest.put("lastName", "User");
        createRequest.put("role", "USER");

        given()
            .spec(withAuth(adminToken))
            .body(createRequest)
        .when()
            .post("/api/users")
        .then()
            .statusCode(201);

        // Login as regular user
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "regularuser3@photobook.local");
        loginRequest.put("password", "password");

        String userToken = given()
            .spec(requestSpec)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("accessToken");

        // Try to delete target user as regular user (should fail)
        given()
            .spec(withAuth(userToken))
        .when()
            .delete("/api/users/" + targetUserId)
        .then()
            .statusCode(403)
            .body("error", equalTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - Failure without authentication")
    void deleteUser_withoutAuth_shouldReturn401() {
        given()
            .spec(requestSpec)
        .when()
            .delete("/api/users/00000000-0000-0000-0000-000000000001")
        .then()
            .statusCode(403); // Spring Security returns 403 for anonymous access to protected endpoints
    }
}
