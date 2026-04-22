package com.servicehomes.api.media.web;

import com.servicehomes.api.media.application.MediaService;
import com.servicehomes.api.media.application.dto.*;
import com.servicehomes.api.listings.domain.ListingPhoto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/listings/{listingId}/photos")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping
    public ResponseEntity<List<ListingPhoto>> listPhotos(@PathVariable UUID listingId) {
        return ResponseEntity.ok(mediaService.listPhotos(listingId));
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUploadResponse> presignedUrl(
        @PathVariable UUID listingId,
        @Valid @RequestBody PresignedUploadRequest request
    ) {
        return ResponseEntity.ok(mediaService.generatePresignedUrl(request));
    }

    @PostMapping("/upload-by-url")
    public ResponseEntity<ListingPhoto> uploadByUrl(
        @PathVariable UUID listingId,
        @Valid @RequestBody UploadByUrlRequest request
    ) {
        return ResponseEntity.ok(mediaService.uploadByUrl(listingId, request));
    }

    @PostMapping("/reorder")
    public ResponseEntity<Void> reorder(
        @PathVariable UUID listingId,
        @Valid @RequestBody ReorderPhotosRequest request
    ) {
        mediaService.reorderPhotos(listingId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{photoId}/cover")
    public ResponseEntity<Void> setCover(
        @PathVariable UUID listingId,
        @PathVariable UUID photoId
    ) {
        mediaService.setCoverPhoto(listingId, photoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID listingId,
        @PathVariable UUID photoId
    ) {
        mediaService.deletePhoto(listingId, photoId);
        return ResponseEntity.noContent().build();
    }
}
