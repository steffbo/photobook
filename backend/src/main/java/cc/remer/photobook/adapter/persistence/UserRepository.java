package cc.remer.photobook.adapter.persistence;

import cc.remer.photobook.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = "SELECT * FROM users WHERE " +
            "(('x' || substring(replace(id::text, '-', ''), 1, 16))::bit(64)::bigint) = :mostSigBits",
            nativeQuery = true)
    Optional<User> findByMostSignificantBits(@Param("mostSigBits") Long mostSigBits);
}
