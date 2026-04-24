package com.servicehomes.api.search.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.saved.domain.SavedListingRepository;
import com.servicehomes.api.search.application.dto.*;
import com.servicehomes.api.search.domain.SearchClick;
import com.servicehomes.api.search.domain.SearchClickRepository;
import com.servicehomes.api.search.domain.SearchQuery;
import com.servicehomes.api.search.domain.SearchQueryRepository;
import com.servicehomes.api.search.domain.SearchSuggestionProjection;
import com.servicehomes.api.search.domain.SearchableListing;
import com.servicehomes.api.search.domain.SearchableListingRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SearchableListingRepositoryCustom searchRepository;
    private final SavedListingRepository savedListingRepository;
    private final SearchQueryRepository searchQueryRepository;
    private final SearchClickRepository searchClickRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request, Pageable pageable, UUID currentUserId) {
        long startTime = System.currentTimeMillis();

        String cacheKey = buildCacheKey(request, pageable, currentUserId);
        Cache cache = cacheManager.getCache("searchResults");
        CachedSearchResult cached = cache != null ? cache.get(cacheKey, CachedSearchResult.class) : null;

        Page<SearchableListing> page;

        if (cached != null) {
            page = cached.page();
        } else {
            page = searchRepository.search(request, pageable);

            if (cache != null) {
                cache.put(cacheKey, new CachedSearchResult(page));
            }
        }

        List<UUID> listingIds = page.getContent().stream()
            .map(SearchableListing::getId)
            .toList();

        Set<UUID> savedIds = currentUserId == null || listingIds.isEmpty()
            ? Set.of()
            : new HashSet<>(savedListingRepository.findListingIdsByGuestIdAndListingIdIn(currentUserId, listingIds));

        List<SearchResultItem> items = page.getContent().stream()
            .map(listing -> mapToListingItem(listing, savedIds.contains(listing.getId()), request))
            .toList();

        long responseTime = System.currentTimeMillis() - startTime;

        List<UUID> resultListingIds = page.getContent().stream()
            .map(SearchableListing::getId)
            .toList();

        SearchQuery recorded = recordSearchQuery(request, currentUserId, (int) page.getTotalElements(), (int) responseTime, resultListingIds);

        return SearchResponse.of(items, page.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize(), recorded != null ? recorded.getId() : null);
    }

    @CacheEvict(value = "searchResults", allEntries = true)
    public void invalidateSearchCache() {
        log.info("Search cache invalidated");
    }

    @Transactional(readOnly = true)
    public List<SearchSuggestionResponse> getSuggestions(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<SearchSuggestionProjection> suggestions = searchRepository.getSuggestions(query.trim(), 10);

        return suggestions.stream()
            .map(s -> {
                if ("location".equals(s.sourceType())) {
                    String[] parts = s.text().split(",");
                    if (parts.length >= 2) {
                        return SearchSuggestionResponse.ofLocation(parts[0].trim(), parts[1].trim());
                    }
                }
                return SearchSuggestionResponse.ofTitle(s.text(), "");
            })
            .toList();
    }

    @Transactional
    public void recordSearchClick(UUID currentUserId, RecordSearchClickRequest clickRequest) {
        SearchQuery searchQuery = searchQueryRepository.findById(clickRequest.searchQueryId())
            .orElseThrow(() -> new IllegalArgumentException("Search query not found: " + clickRequest.searchQueryId()));

        int resultPosition = findRecordedResultPosition(searchQuery, clickRequest.listingId());
        if (resultPosition < 1) {
            throw new IllegalArgumentException("Listing not found in search results: " + clickRequest.listingId());
        }

        SearchClick click = SearchClick.builder()
            .searchQuery(searchQuery)
            .userId(currentUserId)
            .listingId(clickRequest.listingId())
            .resultPosition(resultPosition)
            .deviceType(clickRequest.deviceType())
            .build();

        searchClickRepository.save(click);
    }

    @Transactional
    public SearchQuery recordSearchQuery(SearchRequest request, UUID currentUserId, int resultCount, int responseTimeMs, List<UUID> resultListingIds) {
        try {
            String queryHash = computeQueryHash(request);
            String filtersJson = serializeFilters(request);

            SearchQuery searchQuery = SearchQuery.builder()
                .userId(currentUserId)
                .queryHash(queryHash)
                .queryText(request.hasQuery() ? request.query().trim() : null)
                .filtersUsed(filtersJson)
                .resultCount(resultCount)
                .responseTimeMs(responseTimeMs)
                .geoCenterLat(request.lat())
                .geoCenterLng(request.lng())
                .radiusKm(request.radiusKm())
                .resultListingIds(resultListingIds)
                .build();

            return searchQueryRepository.save(searchQuery);
        } catch (Exception e) {
            log.error("Failed to record search query analytics", e);
            return null;
        }
    }

    private SearchResultItem mapToListingItem(SearchableListing listing, boolean isSaved, SearchRequest request) {
        List<String> amenityIds = parseAmenityIds(listing.getAmenityIds());
        Double distanceKm = computeDistanceKm(listing.getLatitude(), listing.getLongitude(), request.lat(), request.lng());

        return new SearchResultItem(
            listing.getId(),
            listing.getTitle(),
            listing.getCoverUrl(),
            listing.getCity(),
            listing.getCountry(),
            listing.getState(),
            listing.getNightlyPrice(),
            listing.getCategoryName(),
            listing.getLatitude(),
            listing.getLongitude(),
            distanceKm,
            listing.getMaxGuests(),
            listing.getBedrooms(),
            listing.getBeds(),
            listing.getBathrooms(),
            listing.getAverageRating(),
            listing.getReviewCount(),
            listing.getTrustScore(),
            listing.getPropertyType(),
            listing.getInstantBook(),
            amenityIds,
            isSaved
        );
    }

    private Double computeDistanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private List<String> parseAmenityIds(String amenityIdsJson) {
        if (amenityIdsJson == null || amenityIdsJson.equals("null")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(amenityIdsJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String computeQueryHash(SearchRequest request) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = String.join("|",
                nullToEmpty(request.query()),
                nullToEmpty(request.categoryId() != null ? request.categoryId().toString() : null),
                nullToEmpty(request.guests() != null ? request.guests().toString() : null),
                nullToEmpty(request.minPrice() != null ? request.minPrice().toString() : null),
                nullToEmpty(request.maxPrice() != null ? request.maxPrice().toString() : null),
                nullToEmpty(request.bedrooms() != null ? request.bedrooms().toString() : null),
                nullToEmpty(request.propertyTypes() != null ? String.join(",", request.propertyTypes()) : null),
                nullToEmpty(request.lat() != null ? request.lat().toString() : null),
                nullToEmpty(request.lng() != null ? request.lng().toString() : null),
                nullToEmpty(request.radiusKm() != null ? request.radiusKm().toString() : null)
            );
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString();
        }
    }

    private String serializeFilters(SearchRequest request) {
        try {
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("query", request.query());
            filters.put("categoryId", request.categoryId());
            filters.put("guests", request.guests());
            filters.put("checkIn", request.checkIn());
            filters.put("checkOut", request.checkOut());
            filters.put("minPrice", request.minPrice());
            filters.put("maxPrice", request.maxPrice());
            filters.put("amenityIds", request.amenityIds());
            filters.put("lat", request.lat());
            filters.put("lng", request.lng());
            filters.put("radiusKm", request.radiusKm());
            filters.put("bedrooms", request.bedrooms());
            filters.put("beds", request.beds());
            filters.put("bathrooms", request.bathrooms());
            filters.put("propertyTypes", request.propertyTypes());
            filters.put("instantBook", request.instantBook());
            filters.put("sort", request.sort());
            filters.put("swLat", request.swLat());
            filters.put("swLng", request.swLng());
            filters.put("neLat", request.neLat());
            filters.put("neLng", request.neLng());
            filters.put("page", request.page());
            filters.put("size", request.size());
            return objectMapper.writeValueAsString(filters);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String buildCacheKey(SearchRequest request, Pageable pageable, UUID currentUserId) {
        return request.hashCode() + "_" + pageable.hashCode() + "_" + (currentUserId != null ? currentUserId.toString() : "anon");
    }

    private int findRecordedResultPosition(SearchQuery searchQuery, UUID listingId) {
        List<UUID> resultListingIds = searchQuery.getResultListingIds();
        if (resultListingIds == null || resultListingIds.isEmpty()) {
            return -1;
        }

        int pageIndex = resultListingIds.indexOf(listingId);
        if (pageIndex < 0) {
            return -1;
        }

        SearchRequest storedRequest = parseStoredSearchRequest(searchQuery);
        return storedRequest.page() * storedRequest.size() + pageIndex + 1;
    }

    private SearchRequest parseStoredSearchRequest(SearchQuery searchQuery) {
        try {
            return objectMapper.readValue(searchQuery.getFiltersUsed(), SearchRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Search query filters are invalid: " + searchQuery.getId(), e);
        }
    }

    private record CachedSearchResult(Page<SearchableListing> page) {}

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
