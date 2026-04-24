package com.servicehomes.api.wishlists.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.wishlists.application.dto.*;
import com.servicehomes.api.wishlists.domain.SavedSearch;
import com.servicehomes.api.wishlists.domain.SavedSearchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
public class SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public SavedSearchDto saveSearch(UUID userId, SaveSearchRequest request) {
        SavedSearch savedSearch = savedSearchRepository.save(SavedSearch.builder()
            .userId(userId)
            .name(request.name())
            .filtersJson(request.filters())
            .locationQuery(request.locationQuery())
            .geoCenterLat(request.geoCenterLat())
            .geoCenterLng(request.geoCenterLng())
            .radiusKm(request.radiusKm())
            .notifyNewResults(request.notifyNewResults())
            .build());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("timestamp", Instant.now().toString());
        payload.put("userId", userId.toString());
        payload.put("searchHash", Integer.toHexString(request.filters().hashCode()));
        payload.put("hasLocation", request.locationQuery() != null || request.geoCenterLat() != null);
        payload.put("hasDates", request.filters().containsKey("checkIn") || request.filters().containsKey("checkOut"));
        payload.put("filterCount", request.filters().size());
        eventPublisher.publish("saved_search_created", "saved_search", savedSearch.getId().toString(), payload);
        return toDto(savedSearch);
    }

    public List<SavedSearchDto> getSavedSearches(UUID userId) {
        return savedSearchRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public void deleteSavedSearch(UUID userId, UUID savedSearchId) {
        savedSearchRepository.delete(requireOwner(userId, savedSearchId));
    }

    @Transactional
    public SavedSearchDto recordResultCount(UUID userId, UUID savedSearchId, int resultCount) {
        SavedSearch savedSearch = requireOwner(userId, savedSearchId);
        savedSearch.setResultCountSnapshot(resultCount);
        return toDto(savedSearch);
    }

    private SavedSearch requireOwner(UUID userId, UUID savedSearchId) {
        SavedSearch savedSearch = savedSearchRepository.findById(savedSearchId)
            .orElseThrow(() -> new EntityNotFoundException("Saved search not found"));
        if (!savedSearch.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        return savedSearch;
    }

    private SavedSearchDto toDto(SavedSearch savedSearch) {
        return new SavedSearchDto(
            savedSearch.getId(),
            savedSearch.getName(),
            savedSearch.getFiltersJson(),
            savedSearch.getLocationQuery(),
            savedSearch.getGeoCenterLat(),
            savedSearch.getGeoCenterLng(),
            savedSearch.getRadiusKm(),
            savedSearch.isNotifyNewResults(),
            savedSearch.getResultCountSnapshot(),
            savedSearch.getCreatedAt()
        );
    }
}
