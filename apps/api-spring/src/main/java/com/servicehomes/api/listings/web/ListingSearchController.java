package com.servicehomes.api.listings.web;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.application.ListingSearchService;
import com.servicehomes.api.listings.application.dto.ListingCardDto;
import com.servicehomes.api.listings.application.dto.SearchListingsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/listings/search")
@RequiredArgsConstructor
public class ListingSearchController {

    private final ListingSearchService searchService;
    private final EventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<List<ListingCardDto>> search(
        @RequestParam(required = false) String locationQuery,
        @RequestParam(required = false) java.util.UUID categoryId,
        @RequestParam(required = false) Integer guests,
        @RequestParam(required = false) java.time.LocalDate checkIn,
        @RequestParam(required = false) java.time.LocalDate checkOut,
        @RequestParam(required = false) java.math.BigDecimal minPrice,
        @RequestParam(required = false) java.math.BigDecimal maxPrice,
        @RequestParam(required = false) List<java.util.UUID> amenityIds,
        @RequestParam(required = false) Double swLat,
        @RequestParam(required = false) Double swLng,
        @RequestParam(required = false) Double neLat,
        @RequestParam(required = false) Double neLng
    ) {
        var request = new SearchListingsRequest(
            locationQuery, categoryId, guests, checkIn, checkOut,
            minPrice, maxPrice, amenityIds, swLat, swLng, neLat, neLng
        );
        var results = searchService.search(request);
        eventPublisher.publish("search_executed", "search", UUID.randomUUID().toString(), Map.of(
            "locationQuery", locationQuery,
            "categoryId", categoryId != null ? categoryId.toString() : null,
            "guests", guests,
            "checkIn", checkIn,
            "checkOut", checkOut,
            "minPrice", minPrice,
            "maxPrice", maxPrice,
            "resultCount", results.size()
        ));
        return ResponseEntity.ok(results);
    }
}
