package com.servicehomes.api.dashboards.application.dto;

import java.util.List;

public record GuestDashboardResponse(
    List<TripDto> upcomingTrips,
    List<TripDto> pastTrips,
    long savedListingsCount,
    long unreadMessageThreads
) {}
