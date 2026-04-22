package com.servicehomes.api.saved.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.application.dto.ListingSearchResult;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingPhoto;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reviews.domain.ListingReviewSummary;
import com.servicehomes.api.reviews.domain.ReviewRepository;
import com.servicehomes.api.saved.domain.SavedListing;
import com.servicehomes.api.saved.domain.SavedListingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedListingService {

    private final SavedListingRepository savedListingRepository;
    private final ListingRepository listingRepository;
    private final ReviewRepository reviewRepository;
    private final EventPublisher eventPublisher;

    public List<ListingSearchResult> listSavedListings(UUID guestId) {
        List<SavedListing> savedListings = savedListingRepository.findSavedListingsByGuestId(guestId);
        List<Listing> listings = savedListings.stream()
            .map(SavedListing::getListing)
            .toList();
        Map<UUID, ListingReviewSummary> reviewSummaries = loadReviewSummaries(extractListingIds(listings));

        return listings.stream()
            .map(listing -> toResult(listing, reviewSummaries.get(listing.getId())))
            .toList();
    }

    @Transactional
    public void save(UUID guestId, UUID listingId) {
        if (savedListingRepository.existsByGuestIdAndListing_Id(guestId, listingId)) {
            return;
        }

        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        savedListingRepository.save(SavedListing.builder()
            .guestId(guestId)
            .listing(listing)
            .build());

        eventPublisher.publish(
            "listing_saved",
            "listing",
            listingId.toString(),
            Map.of(
                "listingId", listingId.toString(),
                "guestId", guestId.toString()
            )
        );
    }

    @Transactional
    public void unsave(UUID guestId, UUID listingId) {
        if (!savedListingRepository.existsByGuestIdAndListing_Id(guestId, listingId)) {
            return;
        }

        savedListingRepository.deleteByGuestIdAndListing_Id(guestId, listingId);
        eventPublisher.publish(
            "listing_unsaved",
            "listing",
            listingId.toString(),
            Map.of(
                "listingId", listingId.toString(),
                "guestId", guestId.toString()
            )
        );
    }

    public Set<UUID> getSavedListingIds(UUID guestId, Collection<UUID> listingIds) {
        if (guestId == null || listingIds == null || listingIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(savedListingRepository.findListingIdsByGuestIdAndListingIdIn(guestId, listingIds));
    }

    private Map<UUID, ListingReviewSummary> loadReviewSummaries(List<UUID> listingIds) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }

        return reviewRepository.findSummariesByListingIds(listingIds).stream()
            .collect(Collectors.toMap(ListingReviewSummary::listingId, Function.identity()));
    }

    private List<UUID> extractListingIds(List<Listing> listings) {
        return listings.stream()
            .map(Listing::getId)
            .toList();
    }

    private ListingSearchResult toResult(Listing listing, ListingReviewSummary reviewSummary) {
        return new ListingSearchResult(
            listing.getId(),
            listing.getTitle(),
            coverUrl(listing),
            listing.getLocation() != null ? listing.getLocation().getCity() : null,
            listing.getLocation() != null ? listing.getLocation().getCountry() : null,
            listing.getNightlyPrice(),
            listing.getCategory() != null ? listing.getCategory().getName() : null,
            listing.getLocation() != null ? listing.getLocation().getLatitude() : null,
            listing.getLocation() != null ? listing.getLocation().getLongitude() : null,
            listing.getMaxGuests(),
            listing.getBedrooms(),
            listing.getBeds(),
            listing.getBathrooms(),
            null,
            reviewSummary != null && reviewSummary.averageRating() != null
                ? BigDecimal.valueOf(reviewSummary.averageRating())
                : null,
            reviewSummary != null ? reviewSummary.reviewCount() : 0L,
            true
        );
    }

    private String coverUrl(Listing listing) {
        return listing.getPhotos().stream()
            .filter(ListingPhoto::isCover)
            .findFirst()
            .map(ListingPhoto::getUrl)
            .orElse(listing.getPhotos().isEmpty() ? null : listing.getPhotos().get(0).getUrl());
    }
}
