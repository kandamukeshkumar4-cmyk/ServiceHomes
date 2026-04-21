package com.servicehomes.api.listings.web;

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
    public ResponseEntity<List<ListingCategory>> categories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/amenities")
    public ResponseEntity<List<ListingAmenity>> amenities() {
        return ResponseEntity.ok(amenityRepository.findAll());
    }
}
