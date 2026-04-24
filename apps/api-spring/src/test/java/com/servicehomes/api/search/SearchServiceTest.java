package com.servicehomes.api.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.saved.domain.SavedListingRepository;
import com.servicehomes.api.search.application.SearchService;
import com.servicehomes.api.search.application.dto.*;
import com.servicehomes.api.search.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchableListingRepositoryCustom searchRepository;

    @Mock
    private SavedListingRepository savedListingRepository;

    @Mock
    private SearchQueryRepository searchQueryRepository;

    @Mock
    private SearchClickRepository searchClickRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private SearchService searchService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        searchService = new SearchService(searchRepository, savedListingRepository, searchQueryRepository, searchClickRepository, currentUserService, objectMapper, cacheManager);
    }

    @Test
    void searchReturnsMappedResults() {
        SearchableListing listing = createSearchableListing("Test listing", "Miami", "USA", 25.76, -80.19);
        Page<SearchableListing> page = new PageImpl<>(List.of(listing));

        when(searchRepository.search(any(), any())).thenReturn(page);
        when(savedListingRepository.findListingIdsByGuestIdAndListingIdIn(any(), any())).thenReturn(List.of());
        when(searchQueryRepository.save(any())).thenAnswer(inv -> {
            SearchQuery q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        SearchRequest request = new SearchRequest("beach", null, 2, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 10);
        SearchResponse response = searchService.search(request, PageRequest.of(0, 10), UUID.randomUUID());

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("Test listing");
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.searchQueryId()).isNotNull();
    }

    @Test
    void searchMarksSavedListings() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        SearchableListing listing = createSearchableListingWithId(listingId, "Saved listing", "Miami", "USA", 25.76, -80.19);
        Page<SearchableListing> page = new PageImpl<>(List.of(listing));

        when(searchRepository.search(any(), any())).thenReturn(page);
        when(savedListingRepository.findListingIdsByGuestIdAndListingIdIn(eq(userId), any())).thenReturn(List.of(listingId));
        when(searchQueryRepository.save(any())).thenAnswer(inv -> {
            SearchQuery q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        SearchRequest request = new SearchRequest(null, null, 1, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 10);
        SearchResponse response = searchService.search(request, PageRequest.of(0, 10), userId);

        assertThat(response.content().get(0).isSaved()).isTrue();
    }

    @Test
    void searchDoesNotMarkUnsavedListings() {
        UUID userId = UUID.randomUUID();
        SearchableListing listing = createSearchableListing("Unsaved listing", "Miami", "USA", 25.76, -80.19);
        Page<SearchableListing> page = new PageImpl<>(List.of(listing));

        when(searchRepository.search(any(), any())).thenReturn(page);
        when(savedListingRepository.findListingIdsByGuestIdAndListingIdIn(any(), any())).thenReturn(List.of());
        when(searchQueryRepository.save(any())).thenAnswer(inv -> {
            SearchQuery q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        SearchRequest request = new SearchRequest(null, null, 1, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 10);
        SearchResponse response = searchService.search(request, PageRequest.of(0, 10), userId);

        assertThat(response.content().get(0).isSaved()).isFalse();
    }

    @Test
    void getSuggestionsReturnsEmptyForBlankQuery() {
        List<SearchSuggestionResponse> suggestions = searchService.getSuggestions("");
        assertThat(suggestions).isEmpty();

        suggestions = searchService.getSuggestions(null);
        assertThat(suggestions).isEmpty();
    }

    @Test
    void recordSearchClickSavesClick() {
        UUID searchQueryId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        SearchQuery searchQuery = SearchQuery.builder().id(searchQueryId).build();

        when(searchQueryRepository.findById(searchQueryId)).thenReturn(Optional.of(searchQuery));
        when(searchRepository.existsById(listingId)).thenReturn(true);

        RecordSearchClickRequest clickRequest = new RecordSearchClickRequest(searchQueryId, listingId, 1, "desktop");
        searchService.recordSearchClick(UUID.randomUUID(), clickRequest);

        ArgumentCaptor<SearchClick> captor = ArgumentCaptor.forClass(SearchClick.class);
        verify(searchClickRepository).save(captor.capture());

        SearchClick saved = captor.getValue();
        assertThat(saved.getListingId()).isEqualTo(listingId);
        assertThat(saved.getResultPosition()).isEqualTo(1);
        assertThat(saved.getDeviceType()).isEqualTo("desktop");
    }

    @Test
    void recordSearchClickThrowsForInvalidQueryId() {
        UUID invalidId = UUID.randomUUID();
        when(searchQueryRepository.findById(invalidId)).thenReturn(Optional.empty());

        RecordSearchClickRequest clickRequest = new RecordSearchClickRequest(invalidId, UUID.randomUUID(), 1, null);

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> searchService.recordSearchClick(null, clickRequest)
        );
    }

    private SearchableListing createSearchableListing(String title, String city, String country, double lat, double lng) {
        return createSearchableListingWithId(UUID.randomUUID(), title, city, country, lat, lng);
    }

    private SearchableListing createSearchableListingWithId(UUID id, String title, String city, String country, double lat, double lng) {
        SearchableListing listing = new SearchableListing();
        listing.setId(id);
        listing.setTitle(title);
        listing.setDescription("A nice place");
        listing.setNightlyPrice(new BigDecimal("150.00"));
        listing.setMaxGuests(4);
        listing.setBedrooms(2);
        listing.setBeds(2);
        listing.setBathrooms(1);
        listing.setPropertyType("HOUSE");
        listing.setStatus("PUBLISHED");
        listing.setCity(city);
        listing.setCountry(country);
        listing.setLatitude(lat);
        listing.setLongitude(lng);
        listing.setCategoryName("Trending");
        listing.setInstantBook(false);
        listing.setCreatedAt(Instant.now());
        return listing;
    }
}
