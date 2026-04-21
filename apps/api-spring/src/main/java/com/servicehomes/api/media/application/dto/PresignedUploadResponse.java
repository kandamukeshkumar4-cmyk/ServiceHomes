package com.servicehomes.api.media.application.dto;

public record PresignedUploadResponse(
    String uploadUrl,
    String s3Key,
    String publicUrl
) {}
