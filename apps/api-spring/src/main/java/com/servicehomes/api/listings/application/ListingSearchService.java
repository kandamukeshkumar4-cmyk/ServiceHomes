package com.servicehomes.api.listings.application;

import com.servicehomes.api.listings.application.dto.ListingCardDto;
import com.servicehomes.api.listings.application.dto.SearchListingsRequest;
import com.servicehomes.api.listings.domain.ListingSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingSearchService {

    private final ListingSearchRepository searchRepository;

    public List<ListingCardDto> search(SearchListingsRequest request) {
        var amenityIds = request.amenityIds() != null ? request.amenityIds() : List.<java.util.UUID>of();
        String locationPattern = request.locationQuery() != null && !request.locationQuery().isBlank()
            ? "%" + request.locationQuery().toLowerCase() + "%"
            : null;
        return searchRepository.search(
            locationPattern,
            request.categoryId(),
            request.guests(),
            request.checkIn() != null ? request.checkIn() : java.time.LocalDate.now().plusYears(10),
            request.checkOut() != null ? request.checkOut() : java.time.LocalDate.now().plusYears(10).plusDays(1),
            request.minPrice(),
            request.maxPrice(),
            amenityIds,
            amenityIds.isEmpty(),
            request.swLat(),
            request.swLng(),
            request.neLat(),
            request.neLng()
        );
    }
}
