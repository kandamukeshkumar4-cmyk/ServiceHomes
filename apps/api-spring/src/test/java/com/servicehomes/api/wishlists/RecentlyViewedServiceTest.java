package com.servicehomes.api.wishlists;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.wishlists.application.RecentlyViewedService;
import com.servicehomes.api.wishlists.domain.RecentlyViewed;
import com.servicehomes.api.wishlists.domain.RecentlyViewedRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecentlyViewedServiceTest {

    @Mock RecentlyViewedRepository recentlyViewedRepository;
    @Mock ListingRepository listingRepository;
    @Mock EventPublisher eventPublisher;
    @InjectMocks RecentlyViewedService recentlyViewedService;

    @Test
    void upsertsRecentViewsAndReturnsLatestListings() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        Listing listing = Listing.builder().id(listingId).title("Loft").nightlyPrice(BigDecimal.TEN).photos(List.of()).build();
        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(recentlyViewedRepository.findTop20ByUserIdOrderByViewedAtDesc(userId))
            .thenReturn(List.of(RecentlyViewed.builder().userId(userId).listing(listing).viewedAt(Instant.now()).sourcePage("home").build()));

        recentlyViewedService.recordView(userId, listingId, "home");
        var listings = recentlyViewedService.getRecentlyViewed(userId);

        assertThat(listings).extracting("id").containsExactly(listingId);
        verify(recentlyViewedRepository).upsertViewedListing(any(UUID.class), eq(userId), eq(listingId), any(Instant.class), eq("home"));
    }

    @Test
    void purgesOlderThanRetentionCutoff() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        when(recentlyViewedRepository.deleteByViewedAtBefore(cutoff)).thenReturn(3L);

        assertThat(recentlyViewedService.purgeOlderThan(cutoff)).isEqualTo(3L);
    }
}
