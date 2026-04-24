package com.servicehomes.api.wishlists;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.wishlists.application.SavedSearchService;
import com.servicehomes.api.wishlists.application.dto.SaveSearchRequest;
import com.servicehomes.api.wishlists.domain.SavedSearch;
import com.servicehomes.api.wishlists.domain.SavedSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedSearchServiceTest {

    @Mock SavedSearchRepository savedSearchRepository;
    @Mock EventPublisher eventPublisher;
    @InjectMocks SavedSearchService savedSearchService;

    @Test
    void savesRetrievesDeletesAndRoundTripsFilters() {
        UUID userId = UUID.randomUUID();
        UUID savedSearchId = UUID.randomUUID();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("guests", 2);
        filters.put("checkIn", "2026-06-01");
        when(savedSearchRepository.save(any())).thenAnswer(invocation -> {
            SavedSearch saved = invocation.getArgument(0);
            saved.setId(savedSearchId);
            return saved;
        });
        when(savedSearchRepository.findById(savedSearchId)).thenReturn(Optional.of(SavedSearch.builder()
            .id(savedSearchId)
            .userId(userId)
            .name("Porto")
            .filtersJson(filters)
            .build()));

        var created = savedSearchService.saveSearch(userId, new SaveSearchRequest("Porto", filters, "Porto", 41.1, -8.6, 25.0, true));
        var counted = savedSearchService.recordResultCount(userId, savedSearchId, 12);
        savedSearchService.deleteSavedSearch(userId, savedSearchId);

        assertThat(created.filters()).containsEntry("guests", 2);
        assertThat(counted.resultCountSnapshot()).isEqualTo(12);
        verify(savedSearchRepository).delete(any(SavedSearch.class));
    }
}
