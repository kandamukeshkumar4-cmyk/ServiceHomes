package com.servicehomes.api.listings.web;

import com.servicehomes.api.listings.application.dto.AmenityDto;
import com.servicehomes.api.listings.application.dto.CategoryDto;
import com.servicehomes.api.listings.domain.ListingAmenity;
import com.servicehomes.api.listings.domain.ListingAmenityRepository;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/listings")
@RequiredArgsConstructor
public class ListingLookupController {

    private final ListingCategoryRepository categoryRepository;
    private final ListingAmenityRepository amenityRepository;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> categories() {
        List<CategoryDto> dtos = categoryRepository.findAll().stream()
            .map(c -> new CategoryDto(c.getId(), c.getName(), c.getIcon(), c.getDescription()))
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/amenities")
    public ResponseEntity<List<AmenityDto>> amenities() {
        List<AmenityDto> dtos = amenityRepository.findAll().stream()
            .map(a -> new AmenityDto(a.getId(), a.getName(), a.getIcon(), a.getCategory()))
            .toList();
        return ResponseEntity.ok(dtos);
    }
}
