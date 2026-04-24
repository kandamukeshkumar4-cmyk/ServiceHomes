package com.servicehomes.api.search.domain;

import com.servicehomes.api.search.application.dto.SearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchableListingRepositoryCustom {

    Page<SearchableListing> search(SearchRequest request, Pageable pageable);

    List<String> getSuggestions(String query, int limit);
}
