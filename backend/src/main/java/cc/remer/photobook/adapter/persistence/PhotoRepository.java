package cc.remer.photobook.adapter.persistence;

import cc.remer.photobook.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findByOwnerId(UUID ownerId);

    List<Photo> findByStatus(String status);

    @Query("SELECT p FROM Photo p " +
           "JOIN AlbumPhoto ap ON ap.photoId = p.id " +
           "WHERE ap.albumId = :albumId " +
           "ORDER BY ap.position ASC, ap.addedAt DESC")
    List<Photo> findByAlbumId(@Param("albumId") UUID albumId);
}
