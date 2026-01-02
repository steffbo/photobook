package cc.remer.photobook.adapter.web.mapper;

import cc.remer.photobook.adapter.web.model.PhotoResponse;
import cc.remer.photobook.domain.AlbumPhoto;
import cc.remer.photobook.domain.Photo;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class PhotoMapper {

    public PhotoResponse toResponse(Photo photo, UUID albumId) {
        if (photo == null) {
            return null;
        }

        PhotoResponse response = new PhotoResponse();
        response.setId(photo.getId());
        response.setAlbumId(albumId);
        response.setOriginalFilename(photo.getOriginalFilename());
        response.setMimeType(photo.getMimeType());
        response.setFileSize(photo.getFileSize());
        response.setWidth(photo.getWidth());
        response.setHeight(photo.getHeight());
        response.setStatus(PhotoResponse.StatusEnum.fromValue(photo.getStatus()));
        response.setMetadata(photo.getExifData());

        if (photo.getCreatedAt() != null) {
            response.setUploadedAt(photo.getCreatedAt().atOffset(ZoneOffset.UTC));
        }

        // Extract takenAt from EXIF data if available
        if (photo.getExifData() != null && photo.getExifData().containsKey("DateTimeOriginal")) {
            // TODO: Parse EXIF date properly - for now just leave it null
        }

        return response;
    }

    public PhotoResponse toResponse(AlbumPhoto albumPhoto) {
        if (albumPhoto == null || albumPhoto.getPhoto() == null) {
            return null;
        }

        return toResponse(albumPhoto.getPhoto(), albumPhoto.getAlbumId());
    }
}
