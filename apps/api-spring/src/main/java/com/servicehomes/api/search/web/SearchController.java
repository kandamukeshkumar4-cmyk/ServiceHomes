package com.servicehomes.api.search.web;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.search.application.SearchService;
import com.servicehomes.api.search.application.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final CurrentUserService currentUserService;
    private final EventPublisher eventPublisher;

    @PostMapping
    public ResponseEntity<SearchResponse> search(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody SearchRequest request
    ) {
        validateGeoInputs(request);
        var pageable = PageRequest.of(request.page(), request.size());
        var currentUserId = jwt != null ? currentUserService.requireUserId(jwt) : null;
        var response = searchService.search(request, pageable, currentUserId);
        publishSearchExecuted(request, response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<SearchSuggestionResponse>> getSuggestions(
        @RequestParam String q
    ) {
        var suggestions = searchService.getSuggestions(q);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/click")
    public ResponseEntity<Void> recordClick(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody RecordSearchClickRequest request
    ) {
        var currentUserId = jwt != null ? currentUserService.requireUserId(jwt) : null;
        searchService.recordSearchClick(currentUserId, request);
        return ResponseEntity.noContent().build();
    }

    private void validateGeoInputs(SearchRequest request) {
        if ((request.lat() == null) != (request.lng() == null)) {
            throw new IllegalArgumentException("Latitude and longitude must be provided together");
        }
        if (request.radiusKm() != null && (request.lat() == null || request.lng() == null)) {
            throw new IllegalArgumentException("Radius requires latitude and longitude");
        }
        if (request.radiusKm() != null && request.radiusKm() > 100.0) {
            throw new IllegalArgumentException("radiusKm must be less than or equal to 100");
        }
    }

    private void publishSearchExecuted(SearchRequest request, SearchResponse response) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("timestamp", Instant.now().toString());
        payload.put("locationQuery", request.query());
        payload.put("categoryId", request.categoryId() != null ? request.categoryId().toString() : null);
        payload.put("guests", request.guests());
        payload.put("checkIn", request.checkIn() != null ? request.checkIn().toString() : null);
        payload.put("checkOut", request.checkOut() != null ? request.checkOut().toString() : null);
        payload.put("minPrice", request.minPrice() != null ? request.minPrice().toPlainString() : null);
        payload.put("maxPrice", request.maxPrice() != null ? request.maxPrice().toPlainString() : null);
        payload.put("bedrooms", request.bedrooms());
        payload.put("propertyTypes", request.propertyTypes());
        payload.put("instantBook", request.instantBook());
        payload.put("radiusKm", request.radiusKm());
        payload.put("sort", request.sort() != null ? request.sort().name() : null);
        payload.put("page", request.page());
        payload.put("pageSize", request.size());
        payload.put("resultCount", Math.toIntExact(Math.min(response.totalElements(), Integer.MAX_VALUE)));
        eventPublisher.publish("search_executed", "search", response.searchQueryId() != null ? response.searchQueryId().toString() : eventId, payload);
    }
}
