package cc.remer.photobook.adapter.web;

import cc.remer.photobook.adapter.security.UserPrincipal;
import cc.remer.photobook.adapter.web.api.AlbumsApi;
import cc.remer.photobook.adapter.web.mapper.AlbumMapper;
import cc.remer.photobook.adapter.web.model.*;
import cc.remer.photobook.domain.Album;
import cc.remer.photobook.domain.AlbumUser;
import cc.remer.photobook.usecase.AlbumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AlbumController implements AlbumsApi {

    private final AlbumService albumService;
    private final AlbumMapper albumMapper;

    @Override
    public ResponseEntity<AlbumListResponse> listAlbums(Integer page, Integer size) {
        log.debug("List albums request: page={}, size={}", page, size);

        UserPrincipal principal = getCurrentUserPrincipal();
        List<Album> albums = albumService.listAlbumsForUser(principal.getId());

        List<AlbumResponse> albumResponses = albums.stream()
                .map(albumMapper::toResponse)
                .collect(Collectors.toList());

        AlbumListResponse response = new AlbumListResponse()
                .content(albumResponses)
                .totalElements((long) albums.size())
                .totalPages(1)
                .number(0)
                .size(albums.size());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AlbumResponse> getAlbum(UUID albumId) {
        log.debug("Get album request: {}", albumId);

        UserPrincipal principal = getCurrentUserPrincipal();
        Album album = albumService.getAlbum(albumId, principal.getId());

        return ResponseEntity.ok(albumMapper.toResponse(album));
    }

    @Override
    public ResponseEntity<AlbumResponse> createAlbum(CreateAlbumRequest createAlbumRequest) {
        log.debug("Create album request: {}", createAlbumRequest.getName());

        UserPrincipal principal = getCurrentUserPrincipal();
        Album album = albumService.createAlbum(
                createAlbumRequest.getName(),
                createAlbumRequest.getDescription(),
                principal.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(albumMapper.toResponse(album));
    }

    @Override
    public ResponseEntity<AlbumResponse> updateAlbum(UUID albumId, UpdateAlbumRequest updateAlbumRequest) {
        log.debug("Update album request: {}", albumId);

        UserPrincipal principal = getCurrentUserPrincipal();

        // Handle JsonNullable coverPhotoId
        UUID coverPhotoId = null;
        if (updateAlbumRequest.getCoverPhotoId() != null && updateAlbumRequest.getCoverPhotoId().isPresent()) {
            coverPhotoId = updateAlbumRequest.getCoverPhotoId().get();
        }

        Album album = albumService.updateAlbum(
                albumId,
                updateAlbumRequest.getName(),
                updateAlbumRequest.getDescription(),
                coverPhotoId,
                principal.getId()
        );

        return ResponseEntity.ok(albumMapper.toResponse(album));
    }

    @Override
    public ResponseEntity<Void> deleteAlbum(UUID albumId) {
        log.debug("Delete album request: {}", albumId);

        UserPrincipal principal = getCurrentUserPrincipal();
        albumService.deleteAlbum(albumId, principal.getId());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<AlbumUserResponse>> listAlbumUsers(UUID albumId) {
        log.debug("List album users request: {}", albumId);

        UserPrincipal principal = getCurrentUserPrincipal();
        List<AlbumUser> albumUsers = albumService.listAlbumUsers(albumId, principal.getId());

        List<AlbumUserResponse> responses = albumUsers.stream()
                .map(albumMapper::toAlbumUserResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<AlbumUserResponse> addAlbumUser(UUID albumId, AddAlbumUserRequest addAlbumUserRequest) {
        log.debug("Add album user request: album={}, user={}", albumId, addAlbumUserRequest.getUserId());

        UserPrincipal principal = getCurrentUserPrincipal();
        AlbumUser albumUser = albumService.addAlbumUser(
                albumId,
                addAlbumUserRequest.getUserId(),
                addAlbumUserRequest.getRole() != null ? addAlbumUserRequest.getRole().getValue() : null,
                principal.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(albumMapper.toAlbumUserResponse(albumUser));
    }

    @Override
    public ResponseEntity<Void> removeAlbumUser(UUID albumId, UUID userId) {
        log.debug("Remove album user request: album={}, user={}", albumId, userId);

        UserPrincipal principal = getCurrentUserPrincipal();
        albumService.removeAlbumUser(albumId, userId, principal.getId());

        return ResponseEntity.noContent().build();
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) authentication.getPrincipal();
    }
}
