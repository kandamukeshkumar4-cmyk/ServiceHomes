package com.servicehomes.api.wishlists.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class WishlistCleanupScheduler {

    private final RecentlyViewedService recentlyViewedService;

    @Scheduled(cron = "0 30 3 * * *")
    public void purgeRecentlyViewedHistory() {
        recentlyViewedService.purgeOlderThan(Instant.now().minus(90, ChronoUnit.DAYS));
    }
}
