package cc.remer.photobook.adapter.web;

import cc.remer.photobook.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Photo API Integration Tests")
class PhotoControllerIntegrationTest extends BaseIntegrationTest {

    private String albumId;
    private File testImage;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test album
        String token = getAdminToken();
        albumId = createAlbum(token, "Test Album for Photos", "Photos test album");

        // Create a test image file
        testImage = File.createTempFile("test-photo", ".jpg");
        try (FileOutputStream fos = new FileOutputStream(testImage)) {
            // Write a minimal JPEG header (for testing purposes only)
            byte[] jpegHeader = {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00
            };
            fos.write(jpegHeader);
            // Add some content
            fos.write(new byte[100]);
            // Write JPEG end marker
            fos.write(new byte[]{(byte) 0xFF, (byte) 0xD9});
        }
    }

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

    // ========== POST /api/albums/{albumId}/photos Tests ==========

    @Test
    @DisplayName("POST /api/albums/{albumId}/photos - Success uploading single photo")
    void uploadPhotos_withSinglePhoto_shouldUploadSuccessfully() {
        String token = getAdminToken();

        given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201)
            .body("size()", equalTo(1))
            .body("[0].id", notNullValue())
            .body("[0].albumId", equalTo(albumId))
            .body("[0].originalFilename", containsString("test-photo"))
            .body("[0].mimeType", equalTo("image/jpeg"))
            .body("[0].fileSize", greaterThan(0))
            .body("[0].status", equalTo("PROCESSING"))
            .body("[0].uploadedAt", notNullValue());
    }

    @Test
    @DisplayName("POST /api/albums/{albumId}/photos - Success uploading multiple photos")
    void uploadPhotos_withMultiplePhotos_shouldUploadAllSuccessfully() throws IOException {
        String token = getAdminToken();

        // Create another test image
        File testImage2 = File.createTempFile("test-photo2", ".jpg");
        try (FileOutputStream fos = new FileOutputStream(testImage2)) {
            byte[] jpegHeader = {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00
            };
            fos.write(jpegHeader);
            fos.write(new byte[100]);
            fos.write(new byte[]{(byte) 0xFF, (byte) 0xD9});
        }

        try {
            given()
                .spec(withAuth(token))
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("files", testImage, "image/jpeg")
                .multiPart("files", testImage2, "image/jpeg")
            .when()
                .post("/api/albums/" + albumId + "/photos")
            .then()
                .statusCode(201)
                .body("size()", equalTo(2))
                .body("[0].albumId", equalTo(albumId))
                .body("[1].albumId", equalTo(albumId));
        } finally {
            testImage2.delete();
        }
    }

    @Test
    @DisplayName("POST /api/albums/{albumId}/photos - Failure without authentication")
    void uploadPhotos_withoutAuth_shouldReturn403() {
        given()
            .spec(requestSpec)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("POST /api/albums/{albumId}/photos - Failure with invalid album ID")
    void uploadPhotos_withInvalidAlbumId_shouldReturn404() {
        String token = getAdminToken();

        given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/00000000-0000-0000-0000-000000000000/photos")
        .then()
            .statusCode(404);
    }

    // ========== GET /api/albums/{albumId}/photos Tests ==========

    @Test
    @DisplayName("GET /api/albums/{albumId}/photos - Success listing photos in album")
    void listPhotos_withValidAlbumId_shouldReturnPhotos() {
        String token = getAdminToken();

        // Upload a photo first
        given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201);

        // List photos
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(200)
            .body("content.size()", greaterThan(0))
            .body("content[0].id", notNullValue())
            .body("content[0].albumId", equalTo(albumId))
            .body("totalElements", greaterThan(0L))
            .body("totalPages", greaterThan(0))
            .body("number", equalTo(0))
            .body("size", equalTo(50));
    }

    @Test
    @DisplayName("GET /api/albums/{albumId}/photos - Success with pagination")
    void listPhotos_withPagination_shouldReturnPaginatedPhotos() {
        String token = getAdminToken();

        // Upload a photo first
        given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201);

        // List photos with pagination
        given()
            .spec(withAuth(token))
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(200)
            .body("number", equalTo(0))
            .body("size", equalTo(10));
    }

    @Test
    @DisplayName("GET /api/albums/{albumId}/photos - Failure without authentication")
    void listPhotos_withoutAuth_shouldReturn403() {
        given()
            .spec(requestSpec)
        .when()
            .get("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(403);
    }

    // ========== GET /api/photos/{photoId} Tests ==========

    @Test
    @DisplayName("GET /api/photos/{photoId} - Success getting photo details")
    void getPhoto_withValidPhotoId_shouldReturnPhoto() {
        String token = getAdminToken();

        // Upload a photo first
        String photoId = given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201)
            .extract().path("[0].id");

        // Get photo details
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/photos/" + photoId)
        .then()
            .statusCode(200)
            .body("id", equalTo(photoId))
            .body("albumId", equalTo(albumId))
            .body("originalFilename", notNullValue())
            .body("mimeType", equalTo("image/jpeg"))
            .body("fileSize", greaterThan(0))
            .body("status", notNullValue())
            .body("uploadedAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/photos/{photoId} - Failure with invalid photo ID")
    void getPhoto_withInvalidPhotoId_shouldReturn404() {
        String token = getAdminToken();

        given()
            .spec(withAuth(token))
        .when()
            .get("/api/photos/00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(404);
    }

    // ========== DELETE /api/photos/{photoId} Tests ==========

    @Test
    @DisplayName("DELETE /api/photos/{photoId} - Success deleting photo")
    void deletePhoto_withValidPhotoId_shouldDeleteSuccessfully() {
        String token = getAdminToken();

        // Upload a photo first
        String photoId = given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201)
            .extract().path("[0].id");

        // Delete photo
        given()
            .spec(withAuth(token))
        .when()
            .delete("/api/photos/" + photoId)
        .then()
            .statusCode(204);

        // Verify photo is deleted
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/photos/" + photoId)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /api/photos/{photoId} - Failure without authentication")
    void deletePhoto_withoutAuth_shouldReturn403() {
        String token = getAdminToken();

        // Upload a photo first
        String photoId = given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201)
            .extract().path("[0].id");

        // Try to delete without auth
        given()
            .spec(requestSpec)
        .when()
            .delete("/api/photos/" + photoId)
        .then()
            .statusCode(403);
    }

    // ========== POST /api/photos/{photoId}/move Tests ==========

    @Test
    @DisplayName("POST /api/photos/{photoId}/move - Success moving photo to another album")
    void movePhoto_withValidAlbums_shouldMoveSuccessfully() {
        String token = getAdminToken();

        // Upload a photo to first album
        String photoId = given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201)
            .extract().path("[0].id");

        // Create target album
        String targetAlbumId = createAlbum(token, "Target Album", "Target album for move test");

        // Move photo
        Map<String, String> moveRequest = new HashMap<>();
        moveRequest.put("targetAlbumId", targetAlbumId);

        given()
            .spec(withAuth(token))
            .body(moveRequest)
        .when()
            .post("/api/photos/" + photoId + "/move")
        .then()
            .statusCode(200)
            .body("id", equalTo(photoId))
            .body("albumId", equalTo(targetAlbumId));

        // Verify photo is no longer in source album
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(200)
            .body("content.size()", equalTo(0));

        // Verify photo is in target album
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + targetAlbumId + "/photos")
        .then()
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].id", equalTo(photoId));
    }

    // ========== POST /api/photos/{photoId}/copy Tests ==========

    @Test
    @DisplayName("POST /api/photos/{photoId}/copy - Success copying photo to another album")
    void copyPhoto_withValidAlbums_shouldCopySuccessfully() {
        String token = getAdminToken();

        // Upload a photo to first album
        String photoId = given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201)
            .extract().path("[0].id");

        // Create target album
        String targetAlbumId = createAlbum(token, "Target Album Copy", "Target album for copy test");

        // Copy photo
        Map<String, String> copyRequest = new HashMap<>();
        copyRequest.put("targetAlbumId", targetAlbumId);

        given()
            .spec(withAuth(token))
            .body(copyRequest)
        .when()
            .post("/api/photos/" + photoId + "/copy")
        .then()
            .statusCode(201)
            .body("id", equalTo(photoId))
            .body("albumId", equalTo(targetAlbumId));

        // Verify photo is still in source album
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(200)
            .body("content.size()", equalTo(1));

        // Verify photo is also in target album
        given()
            .spec(withAuth(token))
        .when()
            .get("/api/albums/" + targetAlbumId + "/photos")
        .then()
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].id", equalTo(photoId));
    }

    // ========== GET /api/photos/{photoId}/url Tests ==========

    @Test
    @DisplayName("GET /api/photos/{photoId}/url - Success getting original photo URL")
    void getPhotoUrl_forOriginal_shouldReturnPresignedUrl() {
        String token = getAdminToken();

        // Upload a photo first
        String photoId = given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201)
            .extract().path("[0].id");

        // Get photo URL
        given()
            .spec(withAuth(token))
            .queryParam("size", "original")
        .when()
            .get("/api/photos/" + photoId + "/url")
        .then()
            .statusCode(200)
            .body("url", notNullValue())
            .body("url", startsWith("http"))
            .body("expiresAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/photos/{photoId}/url - Failure getting thumbnail URL (not ready)")
    void getPhotoUrl_forThumbnailNotReady_shouldReturn404() {
        String token = getAdminToken();

        // Upload a photo first (thumbnails won't be generated immediately)
        String photoId = given()
            .spec(withAuth(token))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", testImage, "image/jpeg")
        .when()
            .post("/api/albums/" + albumId + "/photos")
        .then()
            .statusCode(201)
            .extract().path("[0].id");

        // Try to get thumbnail URL (should fail as thumbnails are generated async)
        given()
            .spec(withAuth(token))
            .queryParam("size", "small")
        .when()
            .get("/api/photos/" + photoId + "/url")
        .then()
            .statusCode(404);
    }
}
