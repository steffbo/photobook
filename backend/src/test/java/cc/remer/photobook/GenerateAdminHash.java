package cc.remer.photobook;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class GenerateAdminHash {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin";
        String hash = encoder.encode(password);
        System.out.println("BCrypt hash for '" + password + "': " + hash);
        System.out.println("Verification: " + encoder.matches(password, hash));
    }
}
