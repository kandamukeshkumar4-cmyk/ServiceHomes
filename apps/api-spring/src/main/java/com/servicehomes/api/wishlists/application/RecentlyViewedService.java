package com.servicehomes.api.wishlists.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingPhoto;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.wishlists.application.dto.ListingSummaryDto;
import com.servicehomes.api.wishlists.domain.RecentlyViewed;
import com.servicehomes.api.wishlists.domain.RecentlyViewedRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentlyViewedService {

    private final RecentlyViewedRepository recentlyViewedRepository;
    private final ListingRepository listingRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void recordView(UUID userId, UUID listingId, String sourcePage) {
        if (!listingRepository.existsById(listingId)) {
            throw new EntityNotFoundException("Listing not found");
        }
        String normalizedSource = normalizeSource(sourcePage);
        recentlyViewedRepository.upsertViewedListing(UUID.randomUUID(), userId, listingId, Instant.now(), normalizedSource);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("timestamp", Instant.now().toString());
        payload.put("userId", userId.toString());
        payload.put("listingId", listingId.toString());
        payload.put("sourcePage", normalizedSource);
        eventPublisher.publish("recently_viewed_recorded", "listing", listingId.toString(), payload);
    }

    public List<ListingSummaryDto> getRecentlyViewed(UUID userId) {
        return recentlyViewedRepository.findTop20ByUserIdOrderByViewedAtDesc(userId).stream()
            .map(RecentlyViewed::getListing)
            .map(this::toListingSummary)
            .toList();
    }

    @Transactional
    public void clearHistory(UUID userId) {
        recentlyViewedRepository.deleteByUserId(userId);
    }

    @Transactional
    public long purgeOlderThan(Instant cutoff) {
        return recentlyViewedRepository.deleteByViewedAtBefore(cutoff);
    }

    private String normalizeSource(String sourcePage) {
        if (sourcePage == null || sourcePage.isBlank()) {
            return "home";
        }
        return switch (sourcePage) {
            case "search", "wishlist", "home" -> sourcePage;
            default -> "home";
        };
    }

    private ListingSummaryDto toListingSummary(Listing listing) {
        return new ListingSummaryDto(listing.getId(), listing.getTitle(), thumbnail(listing), listing.getNightlyPrice(), listing.getAverageRating());
    }

    private String thumbnail(Listing listing) {
        return listing.getPhotos().stream()
            .filter(ListingPhoto::isCover)
            .findFirst()
            .map(ListingPhoto::getUrl)
            .orElse(listing.getPhotos().isEmpty() ? null : listing.getPhotos().get(0).getUrl());
    }
}
