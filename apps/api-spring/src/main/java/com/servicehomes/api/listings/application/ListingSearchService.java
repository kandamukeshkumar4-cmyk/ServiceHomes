package com.servicehomes.api.listings.application;

import com.servicehomes.api.listings.application.dto.ListingSearchResult;
import com.servicehomes.api.listings.application.dto.SearchListingsRequest;
import com.servicehomes.api.listings.domain.ListingSearchRepository;
import com.servicehomes.api.reviews.domain.ListingReviewSummary;
import com.servicehomes.api.reviews.domain.ReviewRepository;
import com.servicehomes.api.saved.domain.SavedListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingSearchService {

    private final ListingSearchRepository searchRepository;
    private final ReviewRepository reviewRepository;
    private final SavedListingRepository savedListingRepository;

    public Page<ListingSearchResult> search(SearchListingsRequest request, Pageable pageable, UUID currentUserId) {
        var page = searchRepository.search(request, pageable);
        var listingIds = page.getContent().stream()
            .map(row -> row.id())
            .toList();
        Map<UUID, ListingReviewSummary> reviewSummaries = listingIds.isEmpty()
            ? Map.of()
            : reviewRepository.findSummariesByListingIds(listingIds).stream()
                .collect(Collectors.toMap(ListingReviewSummary::listingId, Function.identity()));
        Set<UUID> savedListingIds = currentUserId == null || listingIds.isEmpty()
            ? Set.of()
            : new HashSet<>(savedListingRepository.findListingIdsByGuestIdAndListingIdIn(currentUserId, listingIds));

        return page.map(row -> {
            ListingReviewSummary reviewSummary = reviewSummaries.get(row.id());
            return new ListingSearchResult(
                row.id(),
                row.title(),
                row.coverUrl(),
                row.city(),
                row.country(),
                row.nightlyPrice(),
                row.categoryName(),
                row.latitude(),
                row.longitude(),
                row.maxGuests(),
                row.bedrooms(),
                row.beds(),
                row.bathrooms(),
                row.distanceKm(),
                reviewSummary != null && reviewSummary.averageRating() != null
                    ? BigDecimal.valueOf(reviewSummary.averageRating())
                    : null,
                reviewSummary != null ? reviewSummary.reviewCount() : 0L,
                savedListingIds.contains(row.id())
            );
        });
    }
}
