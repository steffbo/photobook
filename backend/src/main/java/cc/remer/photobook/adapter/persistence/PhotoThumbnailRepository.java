package cc.remer.photobook.adapter.persistence;

import cc.remer.photobook.domain.PhotoThumbnail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoThumbnailRepository extends JpaRepository<PhotoThumbnail, UUID> {

    List<PhotoThumbnail> findByPhotoId(UUID photoId);

    Optional<PhotoThumbnail> findByPhotoIdAndSize(UUID photoId, String size);

    void deleteByPhotoId(UUID photoId);
}
