package com.servicehomes.api.listings.domain;

import com.servicehomes.api.listings.application.dto.ListingSearchRow;
import com.servicehomes.api.listings.application.dto.SearchListingsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingSearchRepositoryCustom {

    Page<ListingSearchRow> search(SearchListingsRequest filters, Pageable pageable);
}
