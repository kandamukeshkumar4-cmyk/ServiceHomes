package com.servicehomes.api.media.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UploadByUrlRequest(
    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https://.*", message = "Only HTTPS URLs are allowed")
    String url
) {}
