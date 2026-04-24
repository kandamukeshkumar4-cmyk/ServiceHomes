package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record RecordResultCountRequest(@PositiveOrZero int resultCount) {}
