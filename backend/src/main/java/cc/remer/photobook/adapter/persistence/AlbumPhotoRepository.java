package cc.remer.photobook.adapter.persistence;

import cc.remer.photobook.domain.AlbumPhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlbumPhotoRepository extends JpaRepository<AlbumPhoto, UUID> {

    List<AlbumPhoto> findByAlbumId(UUID albumId);

    List<AlbumPhoto> findByPhotoId(UUID photoId);

    Optional<AlbumPhoto> findByAlbumIdAndPhotoId(UUID albumId, UUID photoId);

    void deleteByAlbumIdAndPhotoId(UUID albumId, UUID photoId);

    void deleteByPhotoId(UUID photoId);

    boolean existsByAlbumIdAndPhotoId(UUID albumId, UUID photoId);

    long countByAlbumId(UUID albumId);

    @Query("SELECT MAX(ap.position) FROM AlbumPhoto ap WHERE ap.albumId = :albumId")
    Optional<Integer> findMaxPositionByAlbumId(@Param("albumId") UUID albumId);

    @Query("SELECT ap FROM AlbumPhoto ap LEFT JOIN FETCH ap.photo WHERE ap.albumId = :albumId")
    Page<AlbumPhoto> findByAlbumIdWithPhoto(@Param("albumId") UUID albumId, Pageable pageable);
}
