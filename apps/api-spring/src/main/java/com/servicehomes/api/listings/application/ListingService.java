package com.servicehomes.api.listings.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.application.dto.CreateListingRequest;
import com.servicehomes.api.listings.application.dto.ListingDto;
import com.servicehomes.api.listings.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingCategoryRepository categoryRepository;
    private final ListingAmenityRepository amenityRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ListingDto create(UUID hostId, CreateListingRequest req) {
        ListingCategory category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Set<ListingAmenity> amenities = req.amenityIds() != null
            ? Set.copyOf(amenityRepository.findAllById(req.amenityIds()))
            : Set.of();

        Listing listing = Listing.builder()
            .hostId(hostId)
            .title(req.title())
            .description(req.description())
            .category(category)
            .propertyType(Listing.PropertyType.valueOf(req.propertyType()))
            .maxGuests(req.maxGuests())
            .bedrooms(req.bedrooms())
            .beds(req.beds())
            .bathrooms(req.bathrooms())
            .nightlyPrice(req.nightlyPrice())
            .cleaningFee(req.cleaningFee())
            .serviceFee(req.serviceFee())
            .status(Listing.Status.DRAFT)
            .amenities(amenities)
            .build();

        ListingLocation location = ListingLocation.builder()
            .listing(listing)
            .addressLine1(req.location().addressLine1())
            .addressLine2(req.location().addressLine2())
            .city(req.location().city())
            .state(req.location().state())
            .postalCode(req.location().postalCode())
            .country(req.location().country())
            .latitude(req.location().latitude())
            .longitude(req.location().longitude())
            .build();
        listing.setLocation(location);

        ListingPolicy policy = ListingPolicy.builder()
            .listing(listing)
            .checkInTime(req.policy().checkInTime())
            .checkOutTime(req.policy().checkOutTime())
            .minNights(req.policy().minNights())
            .maxNights(req.policy().maxNights())
            .cancellationPolicy(ListingPolicy.CancellationPolicy.valueOf(
                req.policy().cancellationPolicy() != null ? req.policy().cancellationPolicy() : "FLEXIBLE"
            ))
            .instantBook(req.policy().instantBook() != null ? req.policy().instantBook() : false)
            .build();
        listing.setPolicy(policy);

        Listing saved = listingRepository.save(listing);
        eventPublisher.publish("listing_created", "listing", saved.getId().toString(),
            java.util.Map.of("hostId", hostId.toString(), "category", category.getName()));
        return toDto(saved);
    }

    @Transactional
    public ListingDto update(UUID hostId, UUID listingId, CreateListingRequest req) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }

        ListingCategory category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Set<ListingAmenity> amenities = req.amenityIds() != null
            ? Set.copyOf(amenityRepository.findAllById(req.amenityIds()))
            : Set.of();

        listing.setTitle(req.title());
        listing.setDescription(req.description());
        listing.setCategory(category);
        listing.setPropertyType(Listing.PropertyType.valueOf(req.propertyType()));
        listing.setMaxGuests(req.maxGuests());
        listing.setBedrooms(req.bedrooms());
        listing.setBeds(req.beds());
        listing.setBathrooms(req.bathrooms());
        listing.setNightlyPrice(req.nightlyPrice());
        listing.setCleaningFee(req.cleaningFee());
        listing.setServiceFee(req.serviceFee());
        listing.getAmenities().clear();
        listing.getAmenities().addAll(amenities);

        listing.getLocation().setAddressLine1(req.location().addressLine1());
        listing.getLocation().setAddressLine2(req.location().addressLine2());
        listing.getLocation().setCity(req.location().city());
        listing.getLocation().setState(req.location().state());
        listing.getLocation().setPostalCode(req.location().postalCode());
        listing.getLocation().setCountry(req.location().country());
        listing.getLocation().setLatitude(req.location().latitude());
        listing.getLocation().setLongitude(req.location().longitude());

        listing.getPolicy().setCheckInTime(req.policy().checkInTime());
        listing.getPolicy().setCheckOutTime(req.policy().checkOutTime());
        listing.getPolicy().setMinNights(req.policy().minNights());
        listing.getPolicy().setMaxNights(req.policy().maxNights());
        listing.getPolicy().setCancellationPolicy(ListingPolicy.CancellationPolicy.valueOf(
            req.policy().cancellationPolicy() != null ? req.policy().cancellationPolicy() : "FLEXIBLE"
        ));
        listing.getPolicy().setInstantBook(req.policy().instantBook() != null ? req.policy().instantBook() : false);

        return toDto(listing);
    }

    @Transactional
    public ListingDto publish(UUID hostId, UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        listing.setStatus(Listing.Status.PUBLISHED);
        listing.setPublishedAt(Instant.now());
        eventPublisher.publish("listing_published", "listing", listingId.toString(),
            java.util.Map.of("hostId", hostId.toString()));
        return toDto(listing);
    }

    @Transactional
    public ListingDto unpublish(UUID hostId, UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        listing.setStatus(Listing.Status.UNPUBLISHED);
        eventPublisher.publish("listing_unpublished", "listing", listingId.toString(),
            java.util.Map.of("hostId", hostId.toString()));
        return toDto(listing);
    }

    public ListingDto getById(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        return toDto(listing);
    }

    public List<ListingDto> listByHost(UUID hostId) {
        return listingRepository.findAll().stream()
            .filter(l -> l.getHostId().equals(hostId))
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private ListingDto toDto(Listing l) {
        return new ListingDto(
            l.getId(),
            l.getHostId(),
            l.getTitle(),
            l.getDescription(),
            l.getCategory() != null ? new ListingDto.CategoryDto(l.getCategory().getId(), l.getCategory().getName(), l.getCategory().getIcon()) : null,
            l.getPropertyType().name(),
            l.getMaxGuests(),
            l.getBedrooms(),
            l.getBeds(),
            l.getBathrooms(),
            l.getNightlyPrice(),
            l.getCleaningFee(),
            l.getServiceFee(),
            l.getStatus().name(),
            l.getCreatedAt(),
            l.getUpdatedAt(),
            l.getPublishedAt(),
            l.getLocation() != null ? new ListingDto.LocationDto(
                l.getLocation().getAddressLine1(),
                l.getLocation().getAddressLine2(),
                l.getLocation().getCity(),
                l.getLocation().getState(),
                l.getLocation().getPostalCode(),
                l.getLocation().getCountry(),
                l.getLocation().getLatitude(),
                l.getLocation().getLongitude()
            ) : null,
            l.getPolicy() != null ? new ListingDto.PolicyDto(
                l.getPolicy().getCheckInTime() != null ? l.getPolicy().getCheckInTime().toString() : null,
                l.getPolicy().getCheckOutTime() != null ? l.getPolicy().getCheckOutTime().toString() : null,
                l.getPolicy().getMinNights(),
                l.getPolicy().getMaxNights(),
                l.getPolicy().getCancellationPolicy().name(),
                l.getPolicy().isInstantBook()
            ) : null,
            l.getPhotos().stream().map(p -> new ListingDto.PhotoDto(p.getId(), p.getUrl(), p.getOrderIndex(), p.isCover())).collect(Collectors.toList()),
            l.getAmenities().stream().map(a -> new ListingDto.AmenityDto(a.getId(), a.getName(), a.getIcon(), a.getCategory())).collect(Collectors.toList())
        );
    }
}
