package com.servicehomes.api.wishlists.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.wishlists.application.SavedSearchService;
import com.servicehomes.api.wishlists.application.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/saved-searches")
@RequiredArgsConstructor
public class SavedSearchController {

    private final CurrentUserService currentUserService;
    private final SavedSearchService savedSearchService;

    @PostMapping
    public ResponseEntity<SavedSearchDto> saveSearch(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SaveSearchRequest request) {
        return ResponseEntity.ok(savedSearchService.saveSearch(currentUserService.requireUserId(jwt), request));
    }

    @GetMapping
    public ResponseEntity<List<SavedSearchDto>> getSavedSearches(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(savedSearchService.getSavedSearches(currentUserService.requireUserId(jwt)));
    }

    @DeleteMapping("/{savedSearchId}")
    public ResponseEntity<Void> deleteSavedSearch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID savedSearchId) {
        savedSearchService.deleteSavedSearch(currentUserService.requireUserId(jwt), savedSearchId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{savedSearchId}/result-count")
    public ResponseEntity<SavedSearchDto> recordResultCount(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID savedSearchId, @Valid @RequestBody RecordResultCountRequest request) {
        return ResponseEntity.ok(savedSearchService.recordResultCount(currentUserService.requireUserId(jwt), savedSearchId, request.resultCount()));
    }
}
