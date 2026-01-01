package cc.remer.photobook;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

class QuickLoginTest extends BaseIntegrationTest {

    @Test
    void testLogin() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@photobook.local");
        loginRequest.put("password", "admin");

        given()
            .spec(requestSpec)
            .body(loginRequest)
            .log().all()
        .when()
            .post("/api/auth/login")
        .then()
            .log().all()
            .statusCode(200);
    }
}
