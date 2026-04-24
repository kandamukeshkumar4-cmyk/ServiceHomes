package com.servicehomes.api.search.web;

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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<SearchResponse> search(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody SearchRequest request
    ) {
        validateGeoInputs(request);
        var pageable = PageRequest.of(request.page(), request.size());
        var currentUserId = jwt != null ? currentUserService.requireUserId(jwt) : null;
        var response = searchService.search(request, pageable, currentUserId);
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
}
