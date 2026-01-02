package cc.remer.photobook.adapter.web;

import cc.remer.photobook.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Album API Integration Tests")
class AlbumControllerIntegrationTest extends BaseIntegrationTest {

    private String createAlbum(String token, String name, String description) {
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("name", name);
        if (description != null) {
            createRequest.put("description", description);
        }

        return given()
            .spec(withAuth(token))
            .body(createRequest)
        .when()
            .post("/api/albums")
        .then()
            .statusCode(201)
            .extract().path("id");
    }

    // ========== POST /api/albums Tests ==========

    @Test
    @DisplayName("POST /api/albums - Success creating album with name and description")
    void createAlbum_withValidData_shouldCreateAlbum() {
        String token = getAdminToken();

        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("name", "Vacation 2024");
        createRequest.put("description", "Summer vacation photos");

        given()
            .spec(withAuth(token))
            .body(createRequest)
        .when()
            .post("/api/albums")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Vacation 2024"))
            .body("description", equalTo("Summer vacation photos"))
            .body("ownerId", notNullValue())
            .body("photoCount", equalTo(0))
            .body("createdAt", notNullValue())
            .body("updatedAt", notNullValue());
    }

    @Test
    @DisplayName("POST /api/albums - Success creating album with only name")
    void createAlbum_withOnlyName_shouldCreateAlbum() {
        String token = getAdminToken();

        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("name", "Quick Album");

        given()
            .spec(withAuth(token))
            .body(createRequest)
        .when()
            .post("/api/albums")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Quick Album"))
            .body("description", nullValue())
            .body("photoCount", equalTo(0));
    }

    @Test
    @DisplayName("POST /api/albums - Failure without authentication")
    void createAlbum_withoutAuth_shouldReturn403() {
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("name", "Test Album");

        given()
            .spec(requestSpec)
            .body(createRequest)
        .when()
            .post("/api/albums")
        .then()
            .statusCode(403);
    }

    // ========== GET /api/albums Tests ==========

    @Test
    @DisplayName("GET /api/albums - Success listing user's albums")
    void listAlbums_withValidToken_shouldReturnAlbums() {
        String token = getAdminToken();

        // Create a couple of albums
        createAlbum(token, "Album 1", "First album");
        createAlbum(token, "Album 2", "Second album");

        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums")
        .then()
            .statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(2)))
            .body("content[0].id", notNullValue())
            .body("content[0].name", notNullValue())
            .body("content[0].ownerId", notNullValue())
            .body("totalElements", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("GET /api/albums - Failure without authentication")
    void listAlbums_withoutAuth_shouldReturn403() {
        given()
            .spec(requestSpec)
        .when()
            .get("/api/albums")
        .then()
            .statusCode(403);
    }

    // ========== GET /api/albums/{albumId} Tests ==========

    @Test
    @DisplayName("GET /api/albums/{albumId} - Success retrieving album details")
    void getAlbum_withValidId_shouldReturnAlbum() {
        String token = getAdminToken();
        String albumId = createAlbum(token, "Test Album", "Test Description");

        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + albumId)
        .then()
            .statusCode(200)
            .body("id", equalTo(albumId))
            .body("name", equalTo("Test Album"))
            .body("description", equalTo("Test Description"))
            .body("photoCount", equalTo(0));
    }

    @Test
    @DisplayName("GET /api/albums/{albumId} - Failure with non-existent album")
    void getAlbum_withNonExistentId_shouldReturn404() {
        String token = getAdminToken();
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + nonExistentId)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("GET /api/albums/{albumId} - Failure without authentication")
    void getAlbum_withoutAuth_shouldReturn403() {
        String token = getAdminToken();
        String albumId = createAlbum(token, "Test Album", null);

        given()
            .spec(requestSpec)
        .when()
            .get("/api/albums/" + albumId)
        .then()
            .statusCode(403);
    }

    // ========== PUT /api/albums/{albumId} Tests ==========

    @Test
    @DisplayName("PUT /api/albums/{albumId} - Success updating album name")
    void updateAlbum_withNewName_shouldUpdateAlbum() {
        String token = getAdminToken();
        String albumId = createAlbum(token, "Original Name", "Original Description");

        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated Name");

        given()
            .spec(withAuth(token))
            .body(updateRequest)
        .when()
            .put("/api/albums/" + albumId)
        .then()
            .statusCode(200)
            .body("id", equalTo(albumId))
            .body("name", equalTo("Updated Name"))
            .body("description", equalTo("Original Description"));
    }

    @Test
    @DisplayName("PUT /api/albums/{albumId} - Success updating description")
    void updateAlbum_withNewDescription_shouldUpdateAlbum() {
        String token = getAdminToken();
        String albumId = createAlbum(token, "Test Album", "Original Description");

        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("description", "Updated Description");

        given()
            .spec(withAuth(token))
            .body(updateRequest)
        .when()
            .put("/api/albums/" + albumId)
        .then()
            .statusCode(200)
            .body("id", equalTo(albumId))
            .body("name", equalTo("Test Album"))
            .body("description", equalTo("Updated Description"));
    }

    @Test
    @DisplayName("PUT /api/albums/{albumId} - Failure with non-existent album")
    void updateAlbum_withNonExistentId_shouldReturn404() {
        String token = getAdminToken();
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated Name");

        given()
            .spec(withAuth(token))
            .body(updateRequest)
        .when()
            .put("/api/albums/" + nonExistentId)
        .then()
            .statusCode(404);
    }

    // ========== DELETE /api/albums/{albumId} Tests ==========

    @Test
    @DisplayName("DELETE /api/albums/{albumId} - Success deleting album")
    void deleteAlbum_asOwner_shouldDeleteAlbum() {
        String token = getAdminToken();
        String albumId = createAlbum(token, "Album to Delete", null);

        // Delete the album
        given()
            .spec(withAuth(token))
        .when()
            .delete("/api/albums/" + albumId)
        .then()
            .statusCode(204);

        // Verify it's deleted
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + albumId)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /api/albums/{albumId} - Failure with non-existent album")
    void deleteAlbum_withNonExistentId_shouldReturn404() {
        String token = getAdminToken();
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        given()
            .spec(withAuth(token))
        .when()
            .delete("/api/albums/" + nonExistentId)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /api/albums/{albumId} - Failure without authentication")
    void deleteAlbum_withoutAuth_shouldReturn403() {
        String token = getAdminToken();
        String albumId = createAlbum(token, "Test Album", null);

        given()
            .spec(requestSpec)
        .when()
            .delete("/api/albums/" + albumId)
        .then()
            .statusCode(403);
    }

    // ========== GET /api/albums/{albumId}/users Tests ==========

    @Test
    @DisplayName("GET /api/albums/{albumId}/users - Success listing album users")
    void listAlbumUsers_asOwner_shouldReturnUsers() {
        String token = getAdminToken();
        String albumId = createAlbum(token, "Shared Album", null);

        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + albumId + "/users")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("GET /api/albums/{albumId}/users - Failure with non-existent album")
    void listAlbumUsers_withNonExistentId_shouldReturn404() {
        String token = getAdminToken();
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + nonExistentId + "/users")
        .then()
            .statusCode(404);
    }

    // ========== POST /api/albums/{albumId}/users Tests ==========

    @Test
    @DisplayName("POST /api/albums/{albumId}/users - Success adding user with VIEWER role")
    void addAlbumUser_withValidUserId_shouldGrantAccess() {
        String adminToken = getAdminToken();
        String albumId = createAlbum(adminToken, "Shared Album", null);

        // First, get the admin user's ID to use as test user
        String adminUserId = given()
            .spec(withAuth(adminToken))
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(200)
            .extract().path("id");

        Map<String, String> addUserRequest = new HashMap<>();
        addUserRequest.put("userId", adminUserId);
        addUserRequest.put("role", "VIEWER");

        given()
            .spec(withAuth(adminToken))
            .body(addUserRequest)
        .when()
            .post("/api/albums/" + albumId + "/users")
        .then()
            .statusCode(201)
            .body("userId", equalTo(adminUserId))
            .body("role", equalTo("VIEWER"))
            .body("addedAt", notNullValue());
    }

    // ========== DELETE /api/albums/{albumId}/users/{userId} Tests ==========

    @Test
    @DisplayName("DELETE /api/albums/{albumId}/users/{userId} - Success removing user access")
    void removeAlbumUser_asOwner_shouldRevokeAccess() {
        String adminToken = getAdminToken();
        String albumId = createAlbum(adminToken, "Shared Album", null);

        // Get admin user ID
        String adminUserId = given()
            .spec(withAuth(adminToken))
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(200)
            .extract().path("id");

        // Add user
        Map<String, String> addUserRequest = new HashMap<>();
        addUserRequest.put("userId", adminUserId);
        addUserRequest.put("role", "VIEWER");

        given()
            .spec(withAuth(adminToken))
            .body(addUserRequest)
        .when()
            .post("/api/albums/" + albumId + "/users")
        .then()
            .statusCode(201);

        // Remove user
        given()
            .spec(withAuth(adminToken))
        .when()
            .delete("/api/albums/" + albumId + "/users/" + adminUserId)
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("DELETE /api/albums/{albumId}/users/{userId} - Failure with non-existent album")
    void removeAlbumUser_withNonExistentAlbum_shouldReturn404() {
        String token = getAdminToken();
        String nonExistentAlbumId = "00000000-0000-0000-0000-000000000000";
        String nonExistentUserId = "11111111-1111-1111-1111-111111111111";

        given()
            .spec(withAuth(token))
        .when()
            .delete("/api/albums/" + nonExistentAlbumId + "/users/" + nonExistentUserId)
        .then()
            .statusCode(404);
    }
}
