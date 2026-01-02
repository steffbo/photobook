package cc.remer.photobook.usecase;

import cc.remer.photobook.adapter.persistence.AlbumPhotoRepository;
import cc.remer.photobook.adapter.persistence.AlbumRepository;
import cc.remer.photobook.adapter.persistence.PhotoRepository;
import cc.remer.photobook.adapter.persistence.PhotoThumbnailRepository;
import cc.remer.photobook.adapter.storage.S3StorageService;
import cc.remer.photobook.domain.Album;
import cc.remer.photobook.domain.AlbumPhoto;
import cc.remer.photobook.domain.Photo;
import cc.remer.photobook.domain.PhotoThumbnail;
import cc.remer.photobook.usecase.AlbumService.ForbiddenException;
import cc.remer.photobook.usecase.AlbumService.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final AlbumRepository albumRepository;
    private final AlbumPhotoRepository albumPhotoRepository;
    private final PhotoThumbnailRepository photoThumbnailRepository;
    private final PhotoUploadService photoUploadService;
    private final AlbumService albumService;
    private final S3StorageService storageService;

    @Transactional
    public List<Photo> uploadPhotos(UUID albumId, List<MultipartFile> files, UUID currentUserId) {
        log.debug("Uploading {} photos to album: {} by user: {}", files.size(), albumId, currentUserId);

        // Check album access (must be owner to upload)
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found"));

        if (!albumService.isOwner(album, currentUserId)) {
            throw new ForbiddenException("Only the album owner can upload photos");
        }

        // Upload photos
        List<UUID> photoIds = photoUploadService.uploadPhotos(currentUserId, files);

        // Link photos to album
        int maxPosition = albumPhotoRepository.findMaxPositionByAlbumId(albumId).orElse(-1);
        List<Photo> uploadedPhotos = new ArrayList<>();

        for (UUID photoId : photoIds) {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Photo not found after upload"));

            AlbumPhoto albumPhoto = AlbumPhoto.builder()
                    .albumId(albumId)
                    .photoId(photoId)
                    .position(++maxPosition)
                    .build();

            albumPhotoRepository.save(albumPhoto);
            uploadedPhotos.add(photo);
        }

        log.info("Successfully uploaded {} photos to album {}", uploadedPhotos.size(), albumId);
        return uploadedPhotos;
    }

    @Transactional(readOnly = true)
    public Page<AlbumPhoto> listPhotosInAlbum(UUID albumId, int page, int size, UUID currentUserId) {
        log.debug("Listing photos in album: {} for user: {}", albumId, currentUserId);

        // Check album access
        Album album = albumService.getAlbum(albumId, currentUserId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "position"));
        return albumPhotoRepository.findByAlbumIdWithPhoto(albumId, pageable);
    }

    @Transactional(readOnly = true)
    public Photo getPhoto(UUID photoId, UUID currentUserId) {
        log.debug("Getting photo: {} for user: {}", photoId, currentUserId);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));

        // Check if user has access to at least one album containing this photo
        List<AlbumPhoto> albumPhotos = albumPhotoRepository.findByPhotoId(photoId);
        if (albumPhotos.isEmpty()) {
            throw new ResourceNotFoundException("Photo not found in any album");
        }

        boolean hasAccess = albumPhotos.stream()
                .anyMatch(ap -> {
                    try {
                        albumService.getAlbum(ap.getAlbumId(), currentUserId);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

        if (!hasAccess) {
            throw new ForbiddenException("You don't have access to this photo");
        }

        return photo;
    }

    @Transactional
    public void deletePhoto(UUID photoId, UUID currentUserId) {
        log.debug("Deleting photo: {} by user: {}", photoId, currentUserId);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));

        // Check if user is the photo owner
        if (!photo.getOwnerId().equals(currentUserId)) {
            throw new ForbiddenException("Only the photo owner can delete it");
        }

        // Delete from all albums
        albumPhotoRepository.deleteByPhotoId(photoId);

        // Delete thumbnails from storage
        List<PhotoThumbnail> thumbnails = photoThumbnailRepository.findByPhotoId(photoId);
        for (PhotoThumbnail thumbnail : thumbnails) {
            try {
                storageService.deleteThumbnail(thumbnail.getStorageKey());
            } catch (Exception e) {
                log.error("Failed to delete thumbnail from storage: {}", thumbnail.getStorageKey(), e);
            }
        }
        photoThumbnailRepository.deleteByPhotoId(photoId);

        // Delete original from storage
        try {
            storageService.deleteOriginal(photo.getStorageKey());
        } catch (Exception e) {
            log.error("Failed to delete original photo from storage: {}", photo.getStorageKey(), e);
        }

        // Delete photo record
        photoRepository.delete(photo);

        log.info("Successfully deleted photo: {}", photoId);
    }

    @Transactional
    public Photo movePhoto(UUID photoId, UUID targetAlbumId, UUID currentUserId) {
        log.debug("Moving photo: {} to album: {}", photoId, targetAlbumId);

        // Validate photo exists and user has access
        Photo photo = getPhoto(photoId, currentUserId);

        // Check if user is the photo owner (only owner can move photos)
        if (!photo.getOwnerId().equals(currentUserId)) {
            throw new ForbiddenException("Only the photo owner can move photos");
        }

        // Check target album access (must be owner)
        Album targetAlbum = albumRepository.findById(targetAlbumId)
                .orElseThrow(() -> new ResourceNotFoundException("Target album not found"));
        if (!albumService.isOwner(targetAlbum, currentUserId)) {
            throw new ForbiddenException("Only the album owner can add photos");
        }

        // Remove from all current albums
        List<AlbumPhoto> currentAlbumPhotos = albumPhotoRepository.findByPhotoId(photoId);
        currentAlbumPhotos.forEach(albumPhotoRepository::delete);

        // Add to target album
        int maxPosition = albumPhotoRepository.findMaxPositionByAlbumId(targetAlbumId).orElse(-1);
        AlbumPhoto albumPhoto = AlbumPhoto.builder()
                .albumId(targetAlbumId)
                .photoId(photoId)
                .position(maxPosition + 1)
                .build();
        albumPhotoRepository.save(albumPhoto);

        log.info("Successfully moved photo: {} to album: {}", photoId, targetAlbumId);
        return photo;
    }

    @Transactional
    public Photo copyPhoto(UUID photoId, UUID targetAlbumId, UUID currentUserId) {
        log.debug("Copying photo: {} to album: {}", photoId, targetAlbumId);

        // Validate photo exists and user has access
        Photo photo = getPhoto(photoId, currentUserId);

        // Check target album access (must be owner)
        Album targetAlbum = albumRepository.findById(targetAlbumId)
                .orElseThrow(() -> new ResourceNotFoundException("Target album not found"));
        if (!albumService.isOwner(targetAlbum, currentUserId)) {
            throw new ForbiddenException("Only the album owner can add photos");
        }

        // Check if photo is already in target album
        if (albumPhotoRepository.existsByAlbumIdAndPhotoId(targetAlbumId, photoId)) {
            log.debug("Photo already exists in target album");
            return photo;
        }

        // Add to target album
        int maxPosition = albumPhotoRepository.findMaxPositionByAlbumId(targetAlbumId).orElse(-1);
        AlbumPhoto albumPhoto = AlbumPhoto.builder()
                .albumId(targetAlbumId)
                .photoId(photoId)
                .position(maxPosition + 1)
                .build();
        albumPhotoRepository.save(albumPhoto);

        log.info("Successfully copied photo: {} to album: {}", photoId, targetAlbumId);
        return photo;
    }

    @Transactional(readOnly = true)
    public String getPhotoUrl(UUID photoId, String size, UUID currentUserId) {
        log.debug("Generating photo URL for photo: {}, size: {}", photoId, size);

        // Check access
        Photo photo = getPhoto(photoId, currentUserId);

        if ("original".equals(size)) {
            return storageService.generatePresignedUrl(photo.getStorageKey(), 3600);
        }

        // Get thumbnail
        PhotoThumbnail thumbnail = photoThumbnailRepository.findByPhotoIdAndSize(photoId, size)
                .orElseThrow(() -> new ResourceNotFoundException("Thumbnail not found for size: " + size));

        return storageService.generatePresignedUrl(thumbnail.getStorageKey(), 3600);
    }

    @Transactional(readOnly = true)
    public UUID getFirstAlbumIdForPhoto(UUID photoId) {
        List<AlbumPhoto> albumPhotos = albumPhotoRepository.findByPhotoId(photoId);
        if (albumPhotos.isEmpty()) {
            throw new ResourceNotFoundException("Photo not found in any album");
        }
        return albumPhotos.get(0).getAlbumId();
    }
}
