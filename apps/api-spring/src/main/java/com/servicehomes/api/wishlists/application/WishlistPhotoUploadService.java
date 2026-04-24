package com.servicehomes.api.wishlists.application;

import com.servicehomes.api.wishlists.application.dto.WishlistCoverUploadRequest;
import com.servicehomes.api.wishlists.application.dto.WishlistCoverUploadResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistPhotoUploadService {

    private static final Logger log = LoggerFactory.getLogger(WishlistPhotoUploadService.class);

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Value("${aws.s3.region}")
    private String region;

    public WishlistCoverUploadResponse generateCoverUpload(UUID wishlistId, WishlistCoverUploadRequest request) {
        if (!request.contentType().equals("image/jpeg") && !request.contentType().equals("image/png")) {
            throw new IllegalArgumentException("Wishlist covers must be JPEG or PNG images");
        }
        if (request.contentLength() > 2_097_152L) {
            throw new IllegalArgumentException("Wishlist covers must be 2MB or smaller");
        }
        String extension = request.contentType().equals("image/png") ? "png" : "jpg";
        String s3Key = "wishlists/%s/cover-%s.%s".formatted(wishlistId, Instant.now().toEpochMilli(), extension);
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .contentType(request.contentType())
            .contentLength(request.contentLength())
            .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .putObjectRequest(putRequest)
            .build();
        return new WishlistCoverUploadResponse(s3Presigner.presignPutObject(presignRequest).url().toString(), s3Key, publicUrlForKey(s3Key));
    }

    public String publicUrlForKey(String s3Key) {
        if (!endpoint.isBlank()) {
            return endpoint + "/" + bucket + "/" + s3Key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + s3Key;
    }

    public void deleteCoverByKey(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build());
        } catch (Exception e) {
            log.warn("Failed to delete wishlist cover from S3: {}", s3Key, e);
        }
    }
}
