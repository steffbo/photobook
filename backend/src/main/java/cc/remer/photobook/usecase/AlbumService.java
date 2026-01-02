package cc.remer.photobook.usecase;

import cc.remer.photobook.adapter.persistence.AlbumPhotoRepository;
import cc.remer.photobook.adapter.persistence.AlbumRepository;
import cc.remer.photobook.adapter.persistence.AlbumUserRepository;
import cc.remer.photobook.adapter.persistence.UserRepository;
import cc.remer.photobook.domain.Album;
import cc.remer.photobook.domain.AlbumUser;
import cc.remer.photobook.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumUserRepository albumUserRepository;
    private final AlbumPhotoRepository albumPhotoRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Album> listAlbumsForUser(UUID userId) {
        log.debug("Listing albums accessible by user: {}", userId);
        return albumRepository.findAlbumsAccessibleByUser(userId);
    }

    @Transactional(readOnly = true)
    public Album getAlbum(UUID albumId, UUID currentUserId) {
        log.debug("Getting album: {} for user: {}", albumId, currentUserId);

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found"));

        if (!hasAccess(album, currentUserId)) {
            throw new ForbiddenException("You don't have access to this album");
        }

        return album;
    }

    @Transactional
    public Album createAlbum(String name, String description, UUID ownerId) {
        log.debug("Creating album: {} for owner: {}", name, ownerId);

        Album album = Album.builder()
                .name(name)
                .description(description)
                .ownerId(ownerId)
                .build();

        return albumRepository.save(album);
    }

    @Transactional
    public Album updateAlbum(UUID albumId, String name, String description, UUID coverPhotoId, UUID currentUserId) {
        log.debug("Updating album: {} by user: {}", albumId, currentUserId);

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found"));

        if (!isOwner(album, currentUserId)) {
            throw new ForbiddenException("Only the album owner can update it");
        }

        if (name != null) {
            album.setName(name);
        }
        if (description != null) {
            album.setDescription(description);
        }
        if (coverPhotoId != null) {
            album.setCoverPhotoId(coverPhotoId);
        }

        return albumRepository.save(album);
    }

    @Transactional
    public void deleteAlbum(UUID albumId, UUID currentUserId) {
        log.debug("Deleting album: {} by user: {}", albumId, currentUserId);

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found"));

        if (!isOwner(album, currentUserId)) {
            throw new ForbiddenException("Only the album owner can delete it");
        }

        // Delete all album users
        albumUserRepository.findByAlbumId(albumId).forEach(albumUserRepository::delete);

        // Delete all album photos
        albumPhotoRepository.findByAlbumId(albumId).forEach(albumPhotoRepository::delete);

        albumRepository.delete(album);
    }

    @Transactional(readOnly = true)
    public List<AlbumUser> listAlbumUsers(UUID albumId, UUID currentUserId) {
        log.debug("Listing users for album: {} by user: {}", albumId, currentUserId);

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found"));

        if (!isOwner(album, currentUserId)) {
            throw new ForbiddenException("Only the album owner can view album users");
        }

        return albumUserRepository.findByAlbumId(albumId);
    }

    @Transactional
    public AlbumUser addAlbumUser(UUID albumId, UUID userId, String role, UUID currentUserId) {
        log.debug("Adding user: {} to album: {} with role: {} by user: {}", userId, albumId, role, currentUserId);

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found"));

        if (!isOwner(album, currentUserId)) {
            throw new ForbiddenException("Only the album owner can add users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user already has access
        if (albumUserRepository.existsByAlbumIdAndUserId(albumId, userId)) {
            throw new IllegalArgumentException("User already has access to this album");
        }

        AlbumUser albumUser = AlbumUser.builder()
                .albumId(albumId)
                .userId(userId)
                .role(role != null ? role : "VIEWER")
                .build();

        return albumUserRepository.save(albumUser);
    }

    @Transactional
    public void removeAlbumUser(UUID albumId, UUID userId, UUID currentUserId) {
        log.debug("Removing user: {} from album: {} by user: {}", userId, albumId, currentUserId);

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found"));

        if (!isOwner(album, currentUserId)) {
            throw new ForbiddenException("Only the album owner can remove users");
        }

        AlbumUser albumUser = albumUserRepository.findByAlbumIdAndUserId(albumId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User does not have access to this album"));

        albumUserRepository.delete(albumUser);
    }

    @Transactional(readOnly = true)
    public long getPhotoCount(UUID albumId) {
        return albumPhotoRepository.countByAlbumId(albumId);
    }

    private boolean hasAccess(Album album, UUID userId) {
        // Owner always has access
        if (album.getOwnerId().equals(userId)) {
            return true;
        }
        // Check if user has been granted access
        return albumUserRepository.existsByAlbumIdAndUserId(album.getId(), userId);
    }

    public boolean isOwner(Album album, UUID userId) {
        return album.getOwnerId().equals(userId);
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
