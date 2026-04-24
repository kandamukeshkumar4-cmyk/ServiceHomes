package com.servicehomes.api.wishlists;

import com.servicehomes.api.wishlists.application.RecentlyViewedService;
import com.servicehomes.api.wishlists.application.WishlistCleanupScheduler;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class WishlistCleanupSchedulerTest {

    @Test
    void deletesRecentlyViewedRecordsOlderThanNinetyDays() {
        RecentlyViewedService service = mock(RecentlyViewedService.class);

        new WishlistCleanupScheduler(service).purgeRecentlyViewedHistory();

        verify(service).purgeOlderThan(argThat((Instant cutoff) -> cutoff.isBefore(Instant.now().minusSeconds(89L * 24 * 60 * 60))));
    }
}
