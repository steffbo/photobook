package cc.remer.photobook.adapter.persistence;

import cc.remer.photobook.domain.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlbumRepository extends JpaRepository<Album, UUID> {

    List<Album> findByOwnerId(UUID ownerId);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :userId " +
           "OR EXISTS (SELECT 1 FROM AlbumUser au WHERE au.albumId = a.id AND au.userId = :userId)")
    List<Album> findAlbumsAccessibleByUser(@Param("userId") UUID userId);
}
