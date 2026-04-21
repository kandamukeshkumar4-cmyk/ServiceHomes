package com.servicehomes.api.media.application;

import com.servicehomes.api.listings.domain.*;
import com.servicehomes.api.media.application.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ListingRepository listingRepository;
    private final ListingPhotoRepository photoRepository;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public PresignedUploadResponse generatePresignedUrl(PresignedUploadRequest request) {
        String s3Key = "listings/" + UUID.randomUUID() + "-" + request.fileName();

        var putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .contentType(request.contentType())
            .build();

        var presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .putObjectRequest(putRequest)
            .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        String publicUrl = buildPublicUrl(s3Key);

        return new PresignedUploadResponse(uploadUrl, s3Key, publicUrl);
    }

    @Transactional
    public ListingPhoto uploadByUrl(UUID listingId, UploadByUrlRequest request) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        validateUrl(request.url());

        try {
            URL url = new URL(request.url());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(false);

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("URL must point to an image");
            }

            String extension = contentType.replace("image/", "").replace("jpeg", "jpg");
            String s3Key = "listings/" + UUID.randomUUID() + "." + extension;

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = inputStream.readAllBytes();
                s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .build(), RequestBody.fromBytes(bytes));
            }

            int nextOrder = listing.getPhotos().size();
            boolean isFirst = listing.getPhotos().isEmpty();

            ListingPhoto photo = ListingPhoto.builder()
                .listing(listing)
                .s3Key(s3Key)
                .url(buildPublicUrl(s3Key))
                .orderIndex(nextOrder)
                .isCover(isFirst)
                .build();

            return photoRepository.save(photo);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    private void validateUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            if (!"https".equals(uri.getScheme())) {
                throw new IllegalArgumentException("Only HTTPS URLs are allowed");
            }

            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Invalid URL: no host");
            }

            InetAddress address = InetAddress.getByName(host);
            if (address.isSiteLocalAddress() || address.isLoopbackAddress() || address.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Internal addresses are not allowed");
            }

            Set<String> blockedHosts = Set.of(
                "localhost", "127.0.0.1", "0.0.0.0", "169.254.169.254",
                "metadata.google.internal", "metadata.azure.com"
            );
            if (blockedHosts.contains(host.toLowerCase())) {
                throw new IllegalArgumentException("Access to this host is not allowed");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + e.getMessage());
        }
    }

    @Transactional
    public void reorderPhotos(UUID listingId, ReorderPhotosRequest request) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        for (int i = 0; i < request.photoIds().size(); i++) {
            UUID photoId = UUID.fromString(request.photoIds().get(i));
            final int orderIndex = i;
            listing.getPhotos().stream()
                .filter(p -> p.getId().equals(photoId))
                .findFirst()
                .ifPresent(p -> p.setOrderIndex(orderIndex));
        }
    }

    @Transactional
    public void setCoverPhoto(UUID listingId, UUID photoId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        listing.getPhotos().forEach(p -> p.setCover(false));
        listing.getPhotos().stream()
            .filter(p -> p.getId().equals(photoId))
            .findFirst()
            .ifPresent(p -> p.setCover(true));
    }

    @Transactional
    public void deletePhoto(UUID listingId, UUID photoId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        ListingPhoto photo = listing.getPhotos().stream()
            .filter(p -> p.getId().equals(photoId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(photo.getS3Key())
                .build());
        } catch (Exception e) {
            // Log but continue to remove from DB
        }

        listing.getPhotos().remove(photo);
        photoRepository.delete(photo);
    }

    private String buildPublicUrl(String s3Key) {
        if (!endpoint.isBlank()) {
            return endpoint + "/" + bucket + "/" + s3Key;
        }
        return "https://" + bucket + ".s3." + System.getProperty("aws.region", "us-east-1") + ".amazonaws.com/" + s3Key;
    }
}
