package cc.remer.photobook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncodingTest extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testPasswordEncoding() {
        String rawPassword = "admin";
        String hashedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
        System.out.println("Password matches: " + matches);
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Hashed password: " + hashedPassword);
        System.out.println("New hash of 'admin': " + passwordEncoder.encode(rawPassword));
    }
}
