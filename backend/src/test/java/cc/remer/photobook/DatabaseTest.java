package cc.remer.photobook;

import cc.remer.photobook.adapter.persistence.UserRepository;
import cc.remer.photobook.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindAdminUser() {
        Optional<User> admin = userRepository.findByEmail("admin@photobook.local");
        assertThat(admin).isPresent();
        assertThat(admin.get().getEmail()).isEqualTo("admin@photobook.local");
        assertThat(admin.get().getRole()).isEqualTo("ADMIN");
        System.out.println("Admin user found: " + admin.get().getEmail());
        System.out.println("Admin password hash: " + admin.get().getPasswordHash());
    }
}
