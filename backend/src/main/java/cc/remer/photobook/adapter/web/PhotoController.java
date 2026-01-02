package cc.remer.photobook.adapter.web;

import cc.remer.photobook.adapter.security.UserPrincipal;
import cc.remer.photobook.adapter.web.api.PhotosApi;
import cc.remer.photobook.adapter.web.mapper.PhotoMapper;
import cc.remer.photobook.adapter.web.model.*;
import cc.remer.photobook.domain.AlbumPhoto;
import cc.remer.photobook.domain.Photo;
import cc.remer.photobook.usecase.PhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PhotoController implements PhotosApi {

    private final PhotoService photoService;
    private final PhotoMapper photoMapper;

    @Override
    public ResponseEntity<List<PhotoResponse>> uploadPhotos(UUID albumId, List<MultipartFile> files) {
        log.debug("Upload photos request: albumId={}, files={}", albumId, files != null ? files.size() : 0);

        UserPrincipal principal = getCurrentUserPrincipal();

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Photo> photos = photoService.uploadPhotos(albumId, files, principal.getId());

        List<PhotoResponse> responses = photos.stream()
                .map(photo -> photoMapper.toResponse(photo, albumId))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @Override
    public ResponseEntity<PhotoListResponse> listPhotos(UUID albumId, Integer page, Integer size) {
        log.debug("List photos request: albumId={}, page={}, size={}", albumId, page, size);

        UserPrincipal principal = getCurrentUserPrincipal();
        Page<AlbumPhoto> photoPage = photoService.listPhotosInAlbum(albumId, page, size, principal.getId());

        List<PhotoResponse> photoResponses = photoPage.getContent().stream()
                .map(photoMapper::toResponse)
                .collect(Collectors.toList());

        PhotoListResponse response = new PhotoListResponse()
                .content(photoResponses)
                .totalElements(photoPage.getTotalElements())
                .totalPages(photoPage.getTotalPages())
                .number(photoPage.getNumber())
                .size(photoPage.getSize());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PhotoResponse> getPhoto(UUID photoId) {
        log.debug("Get photo request: {}", photoId);

        UserPrincipal principal = getCurrentUserPrincipal();
        Photo photo = photoService.getPhoto(photoId, principal.getId());

        // Get the first album this photo belongs to for the response
        UUID albumId = photoService.getFirstAlbumIdForPhoto(photoId);

        return ResponseEntity.ok(photoMapper.toResponse(photo, albumId));
    }

    @Override
    public ResponseEntity<Void> deletePhoto(UUID photoId) {
        log.debug("Delete photo request: {}", photoId);

        UserPrincipal principal = getCurrentUserPrincipal();
        photoService.deletePhoto(photoId, principal.getId());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PhotoResponse> movePhoto(UUID photoId, MovePhotoRequest movePhotoRequest) {
        log.debug("Move photo request: photoId={}, targetAlbumId={}",
                photoId, movePhotoRequest.getTargetAlbumId());

        UserPrincipal principal = getCurrentUserPrincipal();
        Photo photo = photoService.movePhoto(
                photoId,
                movePhotoRequest.getTargetAlbumId(),
                principal.getId()
        );

        return ResponseEntity.ok(photoMapper.toResponse(photo, movePhotoRequest.getTargetAlbumId()));
    }

    @Override
    public ResponseEntity<PhotoResponse> copyPhoto(UUID photoId, CopyPhotoRequest copyPhotoRequest) {
        log.debug("Copy photo request: photoId={}, targetAlbumId={}", photoId, copyPhotoRequest.getTargetAlbumId());

        UserPrincipal principal = getCurrentUserPrincipal();
        Photo photo = photoService.copyPhoto(photoId, copyPhotoRequest.getTargetAlbumId(), principal.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(photoMapper.toResponse(photo, copyPhotoRequest.getTargetAlbumId()));
    }

    @Override
    public ResponseEntity<PhotoUrlResponse> getPhotoUrl(UUID photoId, String size) {
        log.debug("Get photo URL request: photoId={}, size={}", photoId, size);

        UserPrincipal principal = getCurrentUserPrincipal();
        String url = photoService.getPhotoUrl(photoId, size, principal.getId());

        PhotoUrlResponse response = new PhotoUrlResponse()
                .url(url)
                .expiresAt(java.time.OffsetDateTime.now().plusSeconds(3600));

        return ResponseEntity.ok(response);
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }
}
