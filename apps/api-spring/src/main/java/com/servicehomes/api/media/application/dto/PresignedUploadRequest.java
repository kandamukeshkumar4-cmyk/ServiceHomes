package com.servicehomes.api.media.application.dto;

public record PresignedUploadRequest(
    String contentType,
    String fileName
) {}
