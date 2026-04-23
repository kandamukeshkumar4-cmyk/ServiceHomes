package com.servicehomes.api.dashboards.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record HostDashboardResponse(
    List<ReservationPipelineDto> upcomingReservations,
    List<ReservationPipelineDto> pendingRequests,
    double occupancyRate,
    BigDecimal mockEarnings,
    List<ListingPerformanceDto> listingPerformance,
    long unreadMessageThreads
) {}
