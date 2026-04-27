package com.servicehomes.api.listings.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HostCalendarResponse(
    UUID listingId,
    LocalDate startDate,
    LocalDate endDate,
    List<HostCalendarDayDto> days
) {}
