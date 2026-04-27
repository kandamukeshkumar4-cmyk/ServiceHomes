package com.servicehomes.api.wishlists.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingPhoto;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.wishlists.application.dto.*;
import com.servicehomes.api.wishlists.domain.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ListingRepository listingRepository;
    private final EventPublisher eventPublisher;
    private final WishlistPhotoUploadService wishlistPhotoUploadService;

    @Transactional
    public WishlistSummaryDto createWishlist(UUID ownerId, CreateWishlistRequest request) {
        Wishlist wishlist = Wishlist.builder()
            .ownerId(ownerId)
            .title(request.title())
            .description(request.description())
            .publicList(request.isPublic())
            .build();
        if (wishlist.isPublicList()) {
            ensureShareToken(wishlist);
        }
        wishlist = wishlistRepository.save(wishlist);
        return toSummary(wishlist, ownerId, 0);
    }

    public List<WishlistSummaryDto> getWishlistsForUser(UUID userId) {
        List<Wishlist> wishlists = wishlistRepository.findAccessibleByUserId(userId);
        Map<UUID, Long> itemCounts = itemCountsFor(wishlists);
        return wishlists.stream()
            .map(wishlist -> toSummary(wishlist, userId, itemCounts.getOrDefault(wishlist.getId(), 0L)))
            .toList();
    }

    public SavedWishlistIdsDto getWishlistIdsContainingListing(UUID userId, UUID listingId) {
        List<UUID> wishlistIds = wishlistRepository.findAccessibleByUserId(userId).stream()
            .map(Wishlist::getId)
            .toList();
        if (wishlistIds.isEmpty()) {
            return new SavedWishlistIdsDto(List.of());
        }
        return new SavedWishlistIdsDto(wishlistItemRepository.findWishlistIdsContainingListing(wishlistIds, listingId));
    }

    public WishlistDetailDto getWishlistDetail(UUID userId, UUID wishlistId, int page, int size) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
            .orElseThrow(() -> new EntityNotFoundException("Wishlist not found"));
        if (!canRead(userId, wishlist)) {
            throw new AccessDeniedException("Access denied");
        }
        return toDetail(wishlist, userId, page, size);
    }

    public WishlistDetailDto getSharedWishlist(String token, int page, int size) {
        Wishlist wishlist = wishlistRepository.findByShareToken(token)
            .filter(Wishlist::isPublicList)
            .orElseThrow(() -> new EntityNotFoundException("Wishlist not found"));
        return toPublicDetail(wishlist, page, size);
    }

    @Transactional
    public WishlistItemDto addItem(UUID userId, UUID wishlistId, AddWishlistItemRequest request) {
        Wishlist wishlist = requireEditable(userId, wishlistId);
        Listing listing = listingRepository.findById(request.listingId())
            .orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        int nextOrder = wishlistItemRepository.findNextSortOrderByWishlistId(wishlistId);
        WishlistItem item = WishlistItem.builder()
            .wishlist(wishlist)
            .listing(listing)
            .note(request.note())
            .sortOrder(nextOrder)
            .build();
        try {
            item = wishlistItemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException e) {
            return wishlistItemRepository.findByWishlistIdAndListingId(wishlistId, request.listingId())
                .map(this::toItemDto)
                .orElseThrow(() -> e);
        }
        touch(wishlist);

        eventPublisher.publish("wishlist_item_added", "wishlist", wishlistId.toString(), eventPayload(Map.of(
            "userId", userId.toString(),
            "wishlistId", wishlistId.toString(),
            "listingId", request.listingId().toString(),
            "sourcePage", Optional.ofNullable(request.sourcePage()).orElse("wishlist")
        )));
        return toItemDto(item);
    }

    @Transactional
    public WishlistItemDto updateItem(UUID userId, UUID wishlistId, UUID itemId, UpdateWishlistItemRequest request) {
        requireEditable(userId, wishlistId);
        WishlistItem item = wishlistItemRepository.findById(itemId)
            .filter(existing -> existing.getWishlist().getId().equals(wishlistId))
            .orElseThrow(() -> new EntityNotFoundException("Wishlist item not found"));
        item.setNote(request.note());
        touch(item.getWishlist());
        return toItemDto(item);
    }

    @Transactional
    public void reorderItems(UUID userId, UUID wishlistId, ReorderWishlistItemsRequest request) {
        Wishlist wishlist = requireEditable(userId, wishlistId);
        Map<UUID, WishlistItem> itemsById = new HashMap<>();
        wishlistItemRepository.findByWishlistIdOrderBySortOrderAscAddedAtAsc(wishlistId)
            .forEach(item -> itemsById.put(item.getId(), item));
        Set<UUID> requestedIds = new LinkedHashSet<>(request.itemIds());
        if (requestedIds.size() != request.itemIds().size()
            || requestedIds.size() != itemsById.size()
            || !itemsById.keySet().equals(requestedIds)) {
            throw new IllegalArgumentException("Reorder request must contain each wishlist item exactly once");
        }
        for (int i = 0; i < request.itemIds().size(); i++) {
            itemsById.get(request.itemIds().get(i)).setSortOrder(i);
        }
        touch(wishlist);
        eventPublisher.publish("wishlist_item_reordered", "wishlist", wishlistId.toString(), eventPayload(Map.of(
            "userId", userId.toString(),
            "wishlistId", wishlistId.toString(),
            "itemCount", request.itemIds().size()
        )));
    }

    @Transactional
    public void removeItem(UUID userId, UUID wishlistId, UUID itemId) {
        Wishlist wishlist = requireEditable(userId, wishlistId);
        Optional<WishlistItem> item = wishlistItemRepository.findByWishlistIdAndId(wishlistId, itemId);
        if (item.isEmpty()) {
            return;
        }
        wishlistItemRepository.delete(item.get());
        touch(wishlist);
        publishItemRemoved(userId, wishlistId, item.get());
    }

    @Transactional
    public void removeListing(UUID userId, UUID wishlistId, UUID listingId) {
        Wishlist wishlist = requireEditable(userId, wishlistId);
        Optional<WishlistItem> item = wishlistItemRepository.findByWishlistIdAndListingId(wishlistId, listingId);
        if (item.isEmpty()) {
            return;
        }
        wishlistItemRepository.delete(item.get());
        touch(wishlist);
        publishItemRemoved(userId, wishlistId, item.get());
    }

    private void publishItemRemoved(UUID userId, UUID wishlistId, WishlistItem item) {
        eventPublisher.publish("wishlist_item_removed", "wishlist", wishlistId.toString(), eventPayload(Map.of(
            "userId", userId.toString(),
            "wishlistId", wishlistId.toString(),
            "itemId", item.getId().toString(),
            "listingId", item.getListing().getId().toString()
        )));
    }

    @Transactional
    public WishlistDetailDto updateCollaborators(UUID ownerId, UUID wishlistId, UpdateCollaboratorsRequest request) {
        Wishlist wishlist = requireOwner(ownerId, wishlistId);
        wishlist.setCollaboratorIds(normalizeCollaborators(ownerId, request.collaboratorIds()));
        eventPublisher.publish("wishlist_shared", "wishlist", wishlistId.toString(), eventPayload(Map.of(
            "ownerId", ownerId.toString(),
            "wishlistId", wishlistId.toString(),
            "shareType", "collaborator"
        )));
        return toDetail(wishlist, ownerId, 0, 50);
    }

    @Transactional
    public ShareLinkDto generateShareLink(UUID ownerId, UUID wishlistId) {
        Wishlist wishlist = requireOwner(ownerId, wishlistId);
        ensureShareToken(wishlist);
        wishlist.setPublicList(true);
        eventPublisher.publish("wishlist_shared", "wishlist", wishlistId.toString(), eventPayload(Map.of(
            "ownerId", ownerId.toString(),
            "wishlistId", wishlistId.toString(),
            "shareType", "public"
        )));
        return new ShareLinkDto(wishlist.getShareToken(), "/wishlists/share/" + wishlist.getShareToken());
    }

    @Transactional
    public WishlistDetailDto updatePrivacy(UUID ownerId, UUID wishlistId, boolean isPublic) {
        Wishlist wishlist = requireOwner(ownerId, wishlistId);
        if (isPublic) {
            ensureShareToken(wishlist);
        }
        wishlist.setPublicList(isPublic);
        return toDetail(wishlist, ownerId, 0, 50);
    }

    @Transactional
    public WishlistCoverUploadResponse createCoverUpload(UUID ownerId, UUID wishlistId, WishlistCoverUploadRequest request) {
        requireOwner(ownerId, wishlistId);
        return wishlistPhotoUploadService.generateCoverUpload(wishlistId, request);
    }

    @Transactional
    public WishlistDetailDto updateCoverPhoto(UUID ownerId, UUID wishlistId, FinalizeWishlistCoverRequest request) {
        Wishlist wishlist = requireOwner(ownerId, wishlistId);
        String expectedPrefix = "wishlists/%s/cover-".formatted(wishlistId);
        if (!request.s3Key().startsWith(expectedPrefix) || request.s3Key().contains("..")) {
            throw new IllegalArgumentException("Wishlist cover key is invalid");
        }
        String oldCoverUrl = wishlist.getCoverPhotoUrl();
        wishlist.setCoverPhotoUrl(wishlistPhotoUploadService.publicUrlForKey(request.s3Key()));
        if (oldCoverUrl != null && !oldCoverUrl.isBlank()) {
            String oldKey = extractS3KeyFromUrl(oldCoverUrl);
            if (oldKey != null && !oldKey.startsWith(expectedPrefix)) {
                wishlistPhotoUploadService.deleteCoverByKey(oldKey);
            }
        }
        return toDetail(wishlist, ownerId, 0, 50);
    }

    @Transactional
    public void deleteWishlist(UUID ownerId, UUID wishlistId) {
        Wishlist wishlist = requireOwner(ownerId, wishlistId);
        if (wishlist.getCoverPhotoUrl() != null && !wishlist.getCoverPhotoUrl().isBlank()) {
            String coverKey = extractS3KeyFromUrl(wishlist.getCoverPhotoUrl());
            if (coverKey != null) {
                wishlistPhotoUploadService.deleteCoverByKey(coverKey);
            }
        }
        wishlistRepository.deleteById(wishlistId);
    }

    private String extractS3KeyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int slashIdx = url.indexOf("/wishlists/");
        if (slashIdx < 0) {
            return null;
        }
        return url.substring(slashIdx + 1);
    }

    private Wishlist requireOwner(UUID userId, UUID wishlistId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
            .orElseThrow(() -> new EntityNotFoundException("Wishlist not found"));
        if (!wishlist.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        return wishlist;
    }

    private Wishlist requireEditable(UUID userId, UUID wishlistId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
            .orElseThrow(() -> new EntityNotFoundException("Wishlist not found"));
        if (!wishlist.getOwnerId().equals(userId) && !wishlist.getCollaboratorIds().contains(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        return wishlist;
    }

    private boolean canRead(UUID userId, Wishlist wishlist) {
        return wishlist.getOwnerId().equals(userId)
            || (userId != null && wishlist.getCollaboratorIds().contains(userId));
    }

    private List<UUID> normalizeCollaborators(UUID ownerId, List<UUID> collaboratorIds) {
        return Optional.ofNullable(collaboratorIds).orElse(List.of()).stream()
            .filter(Objects::nonNull)
            .filter(collaboratorId -> !ownerId.equals(collaboratorId))
            .distinct()
            .toList();
    }

    private void touch(Wishlist wishlist) {
        wishlist.setUpdatedAt(Instant.now());
    }

    private void ensureShareToken(Wishlist wishlist) {
        if (wishlist.getShareToken() == null || wishlist.getShareToken().isBlank()) {
            wishlist.setShareToken(UUID.randomUUID().toString());
        }
    }

    private Map<String, Object> eventPayload(Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("timestamp", Instant.now().toString());
        payload.putAll(data);
        return payload;
    }

    private Map<UUID, Long> itemCountsFor(List<Wishlist> wishlists) {
        List<UUID> wishlistIds = wishlists.stream()
            .map(Wishlist::getId)
            .toList();
        if (wishlistIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        wishlistItemRepository.countByWishlistIds(wishlistIds)
            .forEach(count -> counts.put(count.getWishlistId(), count.getItemCount()));
        return counts;
    }

    private WishlistSummaryDto toSummary(Wishlist wishlist, UUID userId, long itemCount) {
        return new WishlistSummaryDto(
            wishlist.getId(),
            wishlist.getOwnerId(),
            wishlist.getTitle(),
            wishlist.getDescription(),
            wishlist.getCoverPhotoUrl(),
            wishlist.isPublicList(),
            userId != null && wishlist.getOwnerId().equals(userId),
            wishlist.getCollaboratorIds().size(),
            itemCount,
            wishlist.getUpdatedAt()
        );
    }

    private WishlistDetailDto toDetail(Wishlist wishlist, UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        List<WishlistItemDto> itemPage = wishlistItemRepository
            .findByWishlistIdOrderBySortOrderAscAddedAtAsc(wishlist.getId(), PageRequest.of(safePage, safeSize))
            .map(this::toItemDto)
            .toList();
        long totalItems = wishlistItemRepository.countByWishlistId(wishlist.getId());
        boolean editable = userId != null && (wishlist.getOwnerId().equals(userId) || wishlist.getCollaboratorIds().contains(userId));
        boolean owner = userId != null && wishlist.getOwnerId().equals(userId);
        return new WishlistDetailDto(
            wishlist.getId(),
            wishlist.getOwnerId(),
            wishlist.getTitle(),
            wishlist.getDescription(),
            wishlist.getCoverPhotoUrl(),
            wishlist.isPublicList(),
            owner,
            wishlist.getCollaboratorIds().size(),
            totalItems,
            wishlist.getUpdatedAt(),
            wishlist.getShareToken(),
            wishlist.getCollaboratorIds(),
            itemPage,
            totalItems,
            editable
        );
    }

    private WishlistDetailDto toPublicDetail(Wishlist wishlist, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        List<WishlistItemDto> itemPage = wishlistItemRepository
            .findByWishlistIdOrderBySortOrderAscAddedAtAsc(wishlist.getId(), PageRequest.of(safePage, safeSize))
            .map(this::toItemDto)
            .toList();
        long totalItems = wishlistItemRepository.countByWishlistId(wishlist.getId());
        return new WishlistDetailDto(
            wishlist.getId(),
            null,
            wishlist.getTitle(),
            wishlist.getDescription(),
            wishlist.getCoverPhotoUrl(),
            wishlist.isPublicList(),
            false,
            0,
            totalItems,
            wishlist.getUpdatedAt(),
            null,
            List.of(),
            itemPage,
            totalItems,
            false
        );
    }

    private WishlistItemDto toItemDto(WishlistItem item) {
        return new WishlistItemDto(item.getId(), toListingSummary(item.getListing()), item.getNote(), item.getSortOrder(), item.getAddedAt());
    }

    private ListingSummaryDto toListingSummary(Listing listing) {
        return new ListingSummaryDto(listing.getId(), listing.getTitle(), thumbnail(listing), listing.getNightlyPrice(), listing.getAverageRating());
    }

    private String thumbnail(Listing listing) {
        return listing.getPhotos().stream()
            .filter(ListingPhoto::isCover)
            .findFirst()
            .map(ListingPhoto::getUrl)
            .orElse(listing.getPhotos().isEmpty() ? null : listing.getPhotos().get(0).getUrl());
    }
}
