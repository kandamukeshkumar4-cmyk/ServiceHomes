package com.servicehomes.api.listings.web;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.listings.application.ListingSearchService;
import com.servicehomes.api.listings.application.dto.ListingSearchResult;
import com.servicehomes.api.listings.application.dto.SearchListingsRequest;
import com.servicehomes.api.listings.application.dto.SearchSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/listings/search")
@RequiredArgsConstructor
public class ListingSearchController {

    private final ListingSearchService searchService;
    private final EventPublisher eventPublisher;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<Page<ListingSearchResult>> search(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String locationQuery,
        @RequestParam(required = false) java.util.UUID categoryId,
        @RequestParam(required = false) Integer guests,
        @RequestParam(required = false) java.time.LocalDate checkIn,
        @RequestParam(required = false) java.time.LocalDate checkOut,
        @RequestParam(required = false) java.math.BigDecimal minPrice,
        @RequestParam(required = false) java.math.BigDecimal maxPrice,
        @RequestParam(required = false) List<java.util.UUID> amenityIds,
        @RequestParam(required = false) java.util.UUID listingId,
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lng,
        @RequestParam(required = false) Double radiusKm,
        @RequestParam(required = false) Integer bedrooms,
        @RequestParam(required = false) List<String> propertyTypes,
        @RequestParam(required = false) Boolean instantBook,
        @RequestParam(required = false) SearchSort sort,
        @RequestParam(required = false) Double swLat,
        @RequestParam(required = false) Double swLng,
        @RequestParam(required = false) Double neLat,
        @RequestParam(required = false) Double neLng,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        validateGeoInputs(lat, lng, radiusKm);
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        var pageable = PageRequest.of(page, size);
        Double resolvedRadiusKm = (lat != null && lng != null)
            ? (radiusKm != null ? radiusKm : 10.0d)
            : null;
        var request = new SearchListingsRequest(
            listingId,
            locationQuery,
            categoryId,
            guests,
            checkIn,
            checkOut,
            minPrice,
            maxPrice,
            amenityIds,
            lat,
            lng,
            resolvedRadiusKm,
            bedrooms,
            propertyTypes,
            instantBook,
            sort,
            page,
            size,
            swLat,
            swLng,
            neLat,
            neLng
        );
        var currentUserId = jwt != null ? currentUserService.requireUserId(jwt) : null;
        var results = searchService.search(request, pageable, currentUserId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("locationQuery", locationQuery);
        payload.put("categoryId", categoryId != null ? categoryId.toString() : null);
        payload.put("guests", guests);
        payload.put("checkIn", checkIn);
        payload.put("checkOut", checkOut);
        payload.put("minPrice", minPrice);
        payload.put("maxPrice", maxPrice);
        payload.put("bedrooms", bedrooms);
        payload.put("propertyTypes", propertyTypes);
        payload.put("instantBook", instantBook);
        payload.put("radiusKm", resolvedRadiusKm);
        payload.put("sort", sort != null ? sort.name() : null);
        payload.put("page", page);
        payload.put("pageSize", size);
        payload.put("resultCount", results.getTotalElements());
        eventPublisher.publish("search_executed", "search", UUID.randomUUID().toString(), payload);
        return ResponseEntity.ok(results);
    }

    private void validateGeoInputs(Double lat, Double lng, Double radiusKm) {
        if ((lat == null) != (lng == null)) {
            throw new IllegalArgumentException("Latitude and longitude must be provided together");
        }
        if (radiusKm != null && (lat == null || lng == null)) {
            throw new IllegalArgumentException("Radius requires latitude and longitude");
        }
        if (radiusKm != null && radiusKm > 100.0d) {
            throw new IllegalArgumentException("radiusKm must be less than or equal to 100");
        }
    }
}
