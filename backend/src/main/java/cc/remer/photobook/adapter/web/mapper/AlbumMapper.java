package cc.remer.photobook.adapter.web.mapper;

import cc.remer.photobook.adapter.web.model.AlbumResponse;
import cc.remer.photobook.adapter.web.model.AlbumUserResponse;
import cc.remer.photobook.domain.Album;
import cc.remer.photobook.domain.AlbumUser;
import cc.remer.photobook.usecase.AlbumService;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class AlbumMapper {

    private final AlbumService albumService;

    public AlbumResponse toResponse(Album album) {
        if (album == null) {
            return null;
        }

        AlbumResponse response = new AlbumResponse();
        response.setId(album.getId());
        response.setName(album.getName());
        response.setDescription(album.getDescription());
        response.setCoverPhotoId(album.getCoverPhotoId() != null ?
                JsonNullable.of(album.getCoverPhotoId()) : JsonNullable.undefined());
        response.setOwnerId(album.getOwnerId());
        response.setPhotoCount((int) albumService.getPhotoCount(album.getId()));

        if (album.getCreatedAt() != null) {
            response.setCreatedAt(album.getCreatedAt().atOffset(ZoneOffset.UTC));
        }

        if (album.getUpdatedAt() != null) {
            response.setUpdatedAt(album.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }

        return response;
    }

    public AlbumUserResponse toAlbumUserResponse(AlbumUser albumUser) {
        if (albumUser == null) {
            return null;
        }

        AlbumUserResponse response = new AlbumUserResponse();
        response.setUserId(albumUser.getUserId());
        response.setRole(AlbumUserResponse.RoleEnum.fromValue(albumUser.getRole()));

        if (albumUser.getGrantedAt() != null) {
            response.setAddedAt(albumUser.getGrantedAt().atOffset(ZoneOffset.UTC));
        }

        return response;
    }
}
