package cc.remer.photobook.usecase;

import cc.remer.photobook.adapter.persistence.UserRepository;
import cc.remer.photobook.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        log.debug("Getting current user: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public User updateCurrentUser(UUID userId, String firstName, String lastName, String password) {
        log.debug("Updating user profile: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (firstName != null) {
            user.setFirstName(firstName);
        }

        if (lastName != null) {
            user.setLastName(lastName);
        }

        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(password));
            log.info("Password changed for user: {}", userId);
        }

        return userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<User> listUsers(int page, int size) {
        log.debug("Listing users: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public User createUser(String email, String password, String firstName, String lastName, String role) {
        log.debug("Creating user: {}", email);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException("Email already exists: " + email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(role != null ? role : "USER")
                .build();

        user = userRepository.save(user);
        log.info("User created: {}", email);

        return user;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(UUID userId) {
        log.debug("Deleting user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUserByMostSigBits(Long mostSigBits) {
        log.debug("Deleting user by most significant bits: {}", mostSigBits);

        User user = userRepository.findByMostSignificantBits(mostSigBits)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    public UUID findByMostSignificantBits(Long mostSigBits) {
        log.debug("Finding user by most significant bits: {}", mostSigBits);
        User user = userRepository.findByMostSignificantBits(mostSigBits)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getId();
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String message) {
            super(message);
        }
    }
}
