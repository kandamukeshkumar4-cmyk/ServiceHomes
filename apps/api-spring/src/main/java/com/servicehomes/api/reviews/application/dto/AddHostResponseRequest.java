package com.servicehomes.api.reviews.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddHostResponseRequest(
    @NotBlank @Size(max = 4000) String response
) {}
