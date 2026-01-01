package cc.remer.photobook.adapter.persistence;

import cc.remer.photobook.domain.AlbumUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlbumUserRepository extends JpaRepository<AlbumUser, UUID> {

    List<AlbumUser> findByAlbumId(UUID albumId);

    List<AlbumUser> findByUserId(UUID userId);

    Optional<AlbumUser> findByAlbumIdAndUserId(UUID albumId, UUID userId);

    void deleteByAlbumIdAndUserId(UUID albumId, UUID userId);

    boolean existsByAlbumIdAndUserId(UUID albumId, UUID userId);
}
